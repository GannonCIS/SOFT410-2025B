package org.example;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;

public class OptionMenu {

    // ---- Dependencies (injected) ----------------------------------------------------
    private final AuthenticationRepository auth;
    private final AccountRepository accounts;
    private final AccountService accountService;

    // ---- UI helpers -----------------------------------------------------------------
    private final Scanner in = new Scanner(System.in);
    private final DecimalFormat money = new DecimalFormat("'$'###,##0.00");

    // ---- Session state --------------------------------------------------------------
    private Integer currentCustomerNumber = null;

    public enum Operation { VIEW_BALANCE, DEPOSIT, WITHDRAW, TRANSFER, EXIT }

    public OptionMenu(AuthenticationRepository auth,
                      AccountRepository accounts,
                      AccountService accountService) {
        this.auth = auth;
        this.accounts = accounts;
        this.accountService = accountService;
    }

    // ---- Login ----------------------------------------------------------------------
    public void getLogin() {
        System.out.println("Welcome to ATM");
        while (true) {
            System.out.print("Enter your Customer Number: ");
            int cn = safeIntInput();
            System.out.print("Enter your PIN Number: ");
            int pn = safeIntInput();

            if (auth.verify(cn, pn)) {
                currentCustomerNumber = cn;
                mainMenuLoop();
                return;
            } else {
                System.out.println("\nWrong Customer Number or PIN\n");
            }
        }
    }

    // ---- Main loop ------------------------------------------------------------------
    public void mainMenuLoop() {
        while (true) {
            Operation op = selectOperation();
            if (op == Operation.EXIT) {
                System.out.println("Thank you for using ATM. Bye!");
                return;
            }

            // For these operations the user must pick an existing account
            Account selected = chooseAccountFor(op);
            if (selected == null) continue; // cancelled or none available

            perform(op, selected);
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
                return selectOperation(); // re-prompt
        }
    }

    /** Shows accounts for the current customer and returns the chosen one (or null to cancel). */
    private Account chooseAccountFor(Operation op) {
        List<Account> list = accounts.findAllByCustomer(currentCustomerNumber);
        if (list == null || list.isEmpty()) {
            System.out.println("No accounts found for your profile.");
            return null;
        }

        while (true) {
            System.out.println("\nChoose an account (0 = cancel):");
            for (int i = 0; i < list.size(); i++) {
                Account a = list.get(i);
                System.out.printf("%d) %s #%d — %s%n",
                        i + 1, a.getAccountType(), a.getAccountNumber(), money.format(a.getAccountBalance()));
            }
            System.out.print("Choice: ");
            int pick = safeIntInput();

            if (pick == 0) return null;
            if (pick >= 1 && pick <= list.size()) return list.get(pick - 1);

            System.out.println("Invalid choice. Try again.");
        }
    }

    private void perform(Operation op, Account acct) {
        switch (op) {
            case VIEW_BALANCE: {
                // Optional: re-fetch to display freshest balance
                Account fresh = accounts.findOneForCustomer(currentCustomerNumber, acct.getAccountNumber());
                double bal = (fresh != null ? fresh.getAccountBalance() : acct.getAccountBalance());
                System.out.println("Balance: " + money.format(bal));
                return;
            }
            case DEPOSIT: {
                double amt = askAmount("Deposit amount (0 = cancel)");
                if (amt == 0.0) { System.out.println("Cancelled."); return; }
                try {
                    double newBal = accountService.deposit(currentCustomerNumber, acct.getAccountNumber(), amt);
                    System.out.println("New balance: " + money.format(newBal));
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    System.out.println("Deposit failed: " + ex.getMessage());
                }
                return;
            }
            case WITHDRAW: {
                double amt = askAmount("Withdraw amount (0 = cancel)");
                if (amt == 0.0) { System.out.println("Cancelled."); return; }
                try {
                    double newBal = accountService.withdraw(currentCustomerNumber, acct.getAccountNumber(), amt);
                    System.out.println("New balance: " + money.format(newBal));
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    System.out.println("Withdrawal failed: " + ex.getMessage());
                }
                return;
            }
            case TRANSFER: {
                System.out.println("Select destination account:");
                Account to = chooseAccountFor(op);
                if (to == null) { System.out.println("Cancelled."); return; }
                if (to.getAccountNumber() == acct.getAccountNumber()) {
                    System.out.println("Cannot transfer to the same account.");
                    return;
                }
                double amt = askAmount("Transfer amount (0 = cancel)");
                if (amt == 0.0) { System.out.println("Cancelled."); return; }
                try {
                    AccountService.TransferResult res = accountService.transfer(
                            currentCustomerNumber, acct.getAccountNumber(), to.getAccountNumber(), amt);
                    System.out.println("Transfer complete.");
                    System.out.println("Source new balance: " + money.format(res.fromNewBalance));
                    System.out.println("Dest   new balance: " + money.format(res.toNewBalance));
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    System.out.println("Transfer failed: " + ex.getMessage());
                }
                return;
            }
            default:
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
        int v = in.nextInt();
        in.nextLine(); // consume trailing newline
        return v;
    }

    private double askAmount(String prompt) {
        System.out.print(prompt + ": ");
        while (!in.hasNextDouble()) {
            System.out.println("Numbers only. Try again.");
            in.next(); // consume bad token
            System.out.print(prompt + ": ");
        }
        double v = in.nextDouble();
        in.nextLine(); // consume trailing newline
        if (v < 0.0) {
            System.out.println("Amount cannot be negative.");
            return 0.0;
        }
        return v;
    }

    // ---- Contracts (use your own top-level interfaces if you already have them) ----
    public interface AuthenticationRepository {
        boolean verify(int customerNumber, int pin);
    }

}
