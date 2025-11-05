package org.example;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;

/**
 * OptionMenu: operation-first controller (no persistence details inside).
 * - Depends on AuthenticationRepository and AccountRepository (injected).
 * - Holds only session state: currentCustomerNumber.
 * - Flow: login → select operation → choose existing account → perform → save.
 */
public class OptionMenu {

    // ---- Dependencies (injected) ----------------------------------------------------
    private final AuthenticationRepository auth;
    private final AccountRepository accounts;

    // ---- UI helpers -----------------------------------------------------------------
    private final Scanner in = new Scanner(System.in);
    private final DecimalFormat money = new DecimalFormat("'$'###,##0.00");

    // ---- Session state (non-PII) ----------------------------------------------------
    private Integer currentCustomerNumber = null;

    public enum Operation { VIEW_BALANCE, DEPOSIT, WITHDRAW, TRANSFER, EXIT }

    // ---- Constructor: inject repositories (no seeding here) -------------------------
    public OptionMenu(AuthenticationRepository auth, AccountRepository accounts) {
        this.auth = auth;
        this.accounts = accounts;
    }

    // ---- Login ----------------------------------------------------------------------
    public void getLogin() {
        System.out.println("Welcome to ATM");
        while (true) {
            try {
                System.out.print("Enter your Customer Number: ");
                int cn = in.nextInt();
                System.out.print("Enter your PIN Number: ");
                int pn = in.nextInt();

                if (auth.verify(cn, pn)) {
                    currentCustomerNumber = cn;
                    mainMenuLoop();
                    return;
                } else {
                    System.out.println("\nWrong Customer Number or PIN\n");
                }
            } catch (Exception e) {
                System.out.println("\nInvalid input. Numbers only.\n");
                in.nextLine(); // consume bad token
            }
        }
    }

    // ---- Main operation-first loop --------------------------------------------------
    public void mainMenuLoop() {
        while (true) {
            Operation op = selectOperation();
            if (op == Operation.EXIT) {
                System.out.println("Thank you for using ATM. Bye!");
                return;
            }
            Account acct = chooseAccountFor(op);
            if (acct == null) { // no eligible accounts or bad selection
                continue;
            }
            perform(op, acct);
        }
    }

    // ---- Menus & actions ------------------------------------------------------------

    private Operation selectOperation() {
        System.out.println("\nWhat would you like to do?");
        System.out.println("1) View Balance");
        System.out.println("2) Deposit");
        System.out.println("3) Withdraw");
        System.out.println("4) Transfer");
        System.out.println("5) Exit");
        System.out.print("Choice: ");

        int pick = safeIntInput();
        switch (pick) {
            case 1: return Operation.VIEW_BALANCE;
            case 2: return Operation.DEPOSIT;
            case 3: return Operation.WITHDRAW;
            case 4: return Operation.TRANSFER;
            case 5: return Operation.EXIT;
            default:
                System.out.println("Invalid choice.");
                return selectOperation(); // simple re-prompt
        }
    }

    private Account chooseAccountFor(Operation op) {
        List<Account> list = accounts.findAllByCustomer(currentCustomerNumber);
        if (list.isEmpty()) {
            System.out.println("No accounts found for your profile. Please contact support to open one.");
            return null;
        }

        // (Optional) filter by op. For now we show all accounts for all ops.
        System.out.println("\nChoose an account:");
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            System.out.printf("%d) %s #%d — %s%n",
                    i + 1, a.getAccountType(), a.getAccountNumber(), money.format(a.getAccountBalance()));
        }
        System.out.print("Choice: ");
        int pick = safeIntInput();
        if (pick < 1 || pick > list.size()) {
            System.out.println("Invalid choice.");
            return null;
        }
        return list.get(pick - 1);
    }

    private void perform(Operation op, Account acct) {
        switch (op) {
            case VIEW_BALANCE: {
                System.out.println("Balance: " + money.format(acct.getAccountBalance()));
                return;
            }
            case DEPOSIT: {
                double amt = askAmount("Deposit amount");
                if (amt <= 0.0 || !Double.isFinite(amt)) {
                    System.out.println("Amount must be a positive number.");
                    return;
                }
                boolean ok = acct.deposit(amt);
                if (!ok) {
                    System.out.println("Deposit failed (invalid amount).");
                    return;
                }
                accounts.save(acct); // persistence handled by repository
                System.out.println("New balance: " + money.format(acct.getAccountBalance()));
                return;
            }
            case WITHDRAW: {
                double amt = askAmount("Withdraw amount");
                if (amt <= 0.0 || !Double.isFinite(amt)) {
                    System.out.println("Amount must be a positive number.");
                    return;
                }
                boolean ok = acct.withdraw(amt);
                if (!ok) {
                    System.out.println("Insufficient funds or invalid amount.");
                    return;
                }
                accounts.save(acct); // persistence handled by repository
                System.out.println("New balance: " + money.format(acct.getAccountBalance()));
                return;
            }
            case TRANSFER: {
                System.out.println("Transfer not implemented yet.");
                // Future: choose source (acct), choose destination (another account), amount, withdraw+deposit+save both
                return;
            }
            default:
                // EXIT handled in main loop
                return;
        }
    }

    // ---- Input helpers --------------------------------------------------------------

    private int safeIntInput() {
        while (!in.hasNextInt()) {
            System.out.println("Numbers only. Try again.");
            in.next(); // consume bad token
            System.out.print("Choice: ");
        }
        return in.nextInt();
    }

    private double askAmount(String prompt) {
        System.out.print(prompt + ": ");
        while (!in.hasNextDouble()) {
            System.out.println("Numbers only. Try again.");
            in.next(); // consume bad token
            System.out.print(prompt + ": ");
        }
        return in.nextDouble();
    }

    // ---- Repository contracts (interfaces) ------------------------------------------
    // Implementations are provided elsewhere (e.g., InMemory* for dev, JDBC for prod).
    public interface AuthenticationRepository {
        boolean verify(int customerNumber, int pin);
    }

    public interface AccountRepository {
        List<Account> findAllByCustomer(int customerNumber);
        void save(Account account); // persist updated balance only
    }
}
