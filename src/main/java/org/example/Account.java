package org.example;

import java.text.DecimalFormat;




public class Account {
    //variables
    private final AccountType accountType;
    private final int customerNumber;
    private final int accountNumber;
    private double accountBalance;
    DecimalFormat moneyFormat = new DecimalFormat("'$'###,##0.00");

    //constructor
    public Account(int customerNumber, int accountNumber, AccountType type, double startingBalance) {
        this.customerNumber = customerNumber;
        this.accountType = type;
        this.accountBalance = startingBalance;
        this.accountNumber = accountNumber;
    }

    //getters
    public int getAccountNumber() {return accountNumber;}

    public int getCustomerNumber(){
        return customerNumber;
    }

    public double getAccountBalance(){return accountBalance;}

    public AccountType getAccountType(){return accountType;}

    //input validation should be in the GUI, as well as output (must be a number and positive), only logic to make sure transaction woln't bounce goes here
    //deposit money into account
    public boolean deposit(double amount) {
        accountBalance += amount;
        return true;
    }

    //withdraw money from account
    public boolean withdraw(double amount) {
        if(accountBalance < amount) {
            return false;
        } else {
            accountBalance -= amount;
            return true;
        }
    }

}
