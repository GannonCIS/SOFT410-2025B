package org.example;

import java.util.Objects;

/**
 * AccountService: business logic boundary for account operations.
 * - Validates inputs (positive/finite amounts, ownership, same-account, etc.)
 * - Loads the freshest state from the repository
 * - Applies domain changes (using Account methods)
 * - Persists immediately (no "save on logout")
 *
 * NOTE: This implementation assumes same-customer transfers. If you want cross-customer
 * transfers, add AccountRepository.findByAccountNumber(int) and use that for the destination.
 */
public interface AccountService {

    /** Deposit money into an account. Returns the new balance in dollars. */
    double deposit(int customerNumber, int accountNumber, double amount);

    /** Withdraw money from an account. Returns the new balance in dollars. */
    double withdraw(int customerNumber, int accountNumber, double amount);

    /** Transfer money between two accounts (same customer). Returns both new balances. */
    TransferResult transfer(int customerNumber, int fromAccount, int toAccount, double amount);

    /** Create a new account for this customer and (optionally) seed an initial deposit. */
    int openAccount(int customerNumber, AccountType type, double initialDeposit);

    final class TransferResult {
        public final double fromNewBalance;
        public final double toNewBalance;

        public TransferResult(double fromNewBalance, double toNewBalance) {
            this.fromNewBalance = fromNewBalance;
            this.toNewBalance = toNewBalance;
        }
    }
}

/**
 * SimpleAccountService: concrete implementation backed by an AccountRepository.
 * - Keeps transactions simple (one repo save per mutation).
 * - Uses the domain object (Account) for the actual arithmetic & invariants.
 */
final class SimpleAccountService implements AccountService {

    private final AccountRepository accounts;

    public SimpleAccountService(AccountRepository accounts) {
        this.accounts = Objects.requireNonNull(accounts, "accounts repository required");
    }

    @Override
    public double deposit(int customerNumber, int accountNumber, double amount) {
        requirePositiveFinite(amount);

        Account a = accounts.findOneForCustomer(customerNumber, accountNumber);
        if (a == null) {
            throw new IllegalArgumentException("Account not found for this customer");
        }

        boolean ok = a.deposit(amount);
        if (!ok) {
            throw new IllegalArgumentException("Deposit rejected (invalid amount)");
        }

        accounts.save(a);
        return a.getAccountBalance();
    }

    @Override
    public double withdraw(int customerNumber, int accountNumber, double amount) {
        requirePositiveFinite(amount);

        Account a = accounts.findOneForCustomer(customerNumber, accountNumber);
        if (a == null) {
            throw new IllegalArgumentException("Account not found for this customer");
        }

        boolean ok = a.withdraw(amount);
        if (!ok) {
            throw new IllegalStateException("Insufficient funds or invalid amount");
        }

        accounts.save(a);
        return a.getAccountBalance();
    }

    @Override
    public TransferResult transfer(int customerNumber, int fromAccount, int toAccount, double amount) {
        requirePositiveFinite(amount);
        if (fromAccount == toAccount) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // same-customer transfer (simple, safe for your current project)
        Account from = accounts.findOneForCustomer(customerNumber, fromAccount);
        if (from == null) {
            throw new IllegalArgumentException("Source account not found for this customer");
        }

        Account to = accounts.findOneForCustomer(customerNumber, toAccount);
        if (to == null) {
            throw new IllegalArgumentException("Destination account not found for this customer");
        }

        // Two-step with compensation (since we aren't opening a DB transaction here)
        if (!from.withdraw(amount)) {
            throw new IllegalStateException("Insufficient funds");
        }
        if (!to.deposit(amount)) {
            // roll back in-memory if deposit failed for some reason
            from.deposit(amount);
            throw new IllegalStateException("Destination rejected the deposit");
        }

        // Persist both updated accounts
        accounts.save(from);
        accounts.save(to);

        return new TransferResult(from.getAccountBalance(), to.getAccountBalance());
    }

    @Override
    public int openAccount(int customerNumber, AccountType type, double initialDeposit) {
        if (type == null) throw new IllegalArgumentException("Account type is required");
        if (Double.isNaN(initialDeposit) || Double.isInfinite(initialDeposit) || initialDeposit < 0.0) {
            throw new IllegalArgumentException("Initial deposit must be finite and >= 0");
        }

        // Delegate creation to the repository (which allocates a new account number and persists it).
        // Store money as cents in the DB; repo converts back to your Account(double) in finders.
        long initialCents = toCents(initialDeposit);
        int newAccountNumber = accounts.create(customerNumber, type, initialCents);

        return newAccountNumber;
    }

    // ---- helpers -------------------------------------------------------------------

    private static void requirePositiveFinite(double v) {
        if (v <= 0.0 || Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException("Amount must be positive and finite");
        }
    }

    private static long toCents(double d) {
        return Math.round(d * 100.0);
    }
}
