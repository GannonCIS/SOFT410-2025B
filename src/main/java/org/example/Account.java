package org.example;

import java.text.DecimalFormat;
import java.util.Scanner;

public class Account {
    //variables
    private String accountType;
    private int customerNumber;
    private double accountBalance = 0;
    DecimalFormat moneyFormat = new DecimalFormat("'$'###,##0.00");

    //constructor
    public Account(String accountType){
        this.accountType = accountType;
    }

    //getters
    public void setCustomerNumber(int customerNumber){
        this.customerNumber = customerNumber;
    }

    public int getCustomerNumber(){
        return customerNumber;
    }

    public double getAccountBalance(){return accountBalance;}

    public String getAccountType(){return accountType;}

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
