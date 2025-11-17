package org.example;

import java.sql.SQLException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws SQLException {
        boolean useProd = true;

        ATM atm = useProd ? AppConfig.prodATM() : AppConfig.devATM();
        atm.getLogin();
    }
}
