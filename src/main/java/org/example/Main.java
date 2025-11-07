package org.example;

public class Main {
    public static void main(String[] args) {
        ATM atm = AppConfig.devATM();  // build an ATM with in-memory deps for now
        atm.getLogin();                // start the UI
    }
}
