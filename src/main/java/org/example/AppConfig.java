package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central place to wire dependencies.
 * Swap the in-memory repos with JDBC repos later without touching UI code.
 */
public final class AppConfig {
    private AppConfig() {}

    /** Build an ATM wired with in-memory repositories and seed data (great for dev/testing). */
    public static ATM devATM() {
        var auth = new InMemoryAuthRepo()
                .seed(952141, 191904)
                .seed(989947, 717976);

        var accounts = new InMemoryAccountRepo();
        // seed a few accounts
        accounts.seed(new Account(952141, 1001, AccountType.CHECKING, 500.00));
        accounts.seed(new Account(952141, 1002, AccountType.SAVINGS, 1200.00));
        accounts.seed(new Account(989947, 2001, AccountType.CHECKING, 250.00));

        var service = new SimpleAccountService(accounts);

        return new ATM(auth, accounts, service);
    }

    // ==================== In-memory repositories ====================

    /** In-memory Authentication (PINs as plain ints for demo). */
    static class InMemoryAuthRepo implements OptionMenu.AuthenticationRepository {
        private final Map<Integer, Integer> pins = new HashMap<>();
        InMemoryAuthRepo seed(int customer, int pin) { pins.put(customer, pin); return this; }
        @Override public boolean verify(int customerNumber, int pin) {
            return Objects.equals(pins.get(customerNumber), pin);
        }
    }

    /** In-memory Account repo (thread-safe maps). */
    static class InMemoryAccountRepo implements OptionMenu.AccountRepository {
        private final Map<Integer, Account> byNo = new ConcurrentHashMap<>();
        private final Map<Integer, Set<Integer>> byCustomer = new ConcurrentHashMap<>();

        void seed(Account a) {
            byNo.put(a.getAccountNumber(), a);
            byCustomer.computeIfAbsent(a.getCustomerNumber(), k -> new LinkedHashSet<>())
                    .add(a.getAccountNumber());
        }

        @Override public List<Account> findAllByCustomer(int customerNumber) {
            var nos = byCustomer.getOrDefault(customerNumber, Set.of());
            var list = new ArrayList<Account>(nos.size());
            for (int n : nos) {
                var a = byNo.get(n);
                if (a != null) list.add(a);
            }
            return list;
        }

        @Override public Account findOneForCustomer(int customerNumber, int accountNumber) {
            var a = byNo.get(accountNumber);
            return (a != null && a.getCustomerNumber() == customerNumber) ? a : null;
        }
    }

    // ==================== Service (uses your existing AccountService interface) ====================

    /** Simple service: validates, loads fresh from repo, mutates, returns new balances. */
    static class SimpleAccountService implements AccountService {
        private final OptionMenu.AccountRepository accounts;

        SimpleAccountService(OptionMenu.AccountRepository accounts) {
            this.accounts = Objects.requireNonNull(accounts);
        }

        @Override
        public double deposit(int customerNumber, int accountNumber, double amount) {
            requirePositiveFinite(amount);
            var a = accounts.findOneForCustomer(customerNumber, accountNumber);
            if (a == null) throw new IllegalArgumentException("Account not found for this customer");
            if (!a.deposit(amount)) throw new IllegalArgumentException("Deposit rejected");
            return a.getAccountBalance();
        }

        @Override
        public double withdraw(int customerNumber, int accountNumber, double amount) {
            requirePositiveFinite(amount);
            var a = accounts.findOneForCustomer(customerNumber, accountNumber);
            if (a == null) throw new IllegalArgumentException("Account not found for this customer");
            if (!a.withdraw(amount)) throw new IllegalStateException("Insufficient funds");
            return a.getAccountBalance();
        }

        @Override
        public TransferResult transfer(int customerNumber, int fromAccount, int toAccount, double amount) {
            requirePositiveFinite(amount);
            if (fromAccount == toAccount) throw new IllegalArgumentException("Cannot transfer to the same account");

            var from = accounts.findOneForCustomer(customerNumber, fromAccount);
            var to   = accounts.findOneForCustomer(customerNumber, toAccount);
            if (from == null || to == null) throw new IllegalArgumentException("Account not found for this customer");

            if (!from.withdraw(amount)) throw new IllegalStateException("Insufficient funds");
            if (!to.deposit(amount)) { from.deposit(amount); throw new IllegalStateException("Deposit failed"); }

            return new TransferResult(from.getAccountBalance(), to.getAccountBalance());
        }

        @Override
        public int openAccount(int customerNumber, AccountType type, double initialDeposit) {
            throw new UnsupportedOperationException("ATM cannot open accounts");
        }

        private static void requirePositiveFinite(double v) {
            if (v <= 0.0 || Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("Amount must be positive and finite");
            }
        }
    }
}
