package org.example;

/**
 * Declares ATM class that inherits from OptionMenu
 */
public class ATM extends OptionMenu {
    // Main method: entry point of the program
    public static void main(String[] args) {
        OptionMenu options = new OptionMenu(); // Creates an object of OptionMenu class
        options.getLogin(); // Calls the getLogin method to prompt user login
    }
}
