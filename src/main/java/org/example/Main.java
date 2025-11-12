package org.example;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        boolean useProd = Arrays.stream(args)
                .anyMatch(arg -> "prod".equalsIgnoreCase(arg) || "--prod".equalsIgnoreCase(arg));

        ATM atm = useProd ? AppConfig.prodATM() : AppConfig.devATM();
        atm.getLogin();
    }
}
