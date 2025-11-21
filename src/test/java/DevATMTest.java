
import org.example.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.ATM;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests that exercise the devATM wiring:
 * AppConfig.devATM() + in-memory repos + OptionMenu flow.
 */
class DevATMTest {

    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void stashSystemIO() {
        originalOut = System.out;
        originalIn  = System.in;
    }

    @AfterEach
    void restoreSystemIO() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    /**
     * Happy path: login as 952141/191904, deposit 50 into CHECKING #1001, then exit.
     * Seeded start balance: 500.00 → expected new balance: 550.00.
     */
    @Test
    void devATM_depositFlow_updatesBalanceAndPrintsNewBalance() {
        // Scripted input:
        // 952141 (customer)
        // 191904 (pin)
        // 2 (Deposit)
        // 1 (pick first account: CHECKING #1001)
        // 50 (deposit amount)
        // 5 (Exit)
        String script = String.join(System.lineSeparator(),
                "952141",
                "191904",
                "2",
                "1",
                "50",
                "5"
        ) + System.lineSeparator();

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        ATM atm = AppConfig.devATM();
        atm.getLogin();   // run the whole flow

        String output = outBuffer.toString();

        // Basic sanity checks
        assertTrue(output.contains("Welcome to ATM"));
        assertTrue(output.contains("What would you like to do?"));

        // Most important: the new balance is correct for seeded data (500 + 50 = 550)
        assertTrue(output.contains("New balance: $550.00"));
    }

    /**
     * Error path: login as 952141/191904, attempt to withdraw more than the balance
     * from CHECKING #1001 (500.00), then exit.
     */
    @Test
    void devATM_withdrawTooMuch_showsErrorAndDoesNotShowNewBalance() {
        // 952141 (customer)
        // 191904 (pin)
        // 3 (Withdraw)
        // 1 (CHECKING #1001)
        // 600 (amount > 500)
        // 5 (Exit)
        String script = String.join(System.lineSeparator(),
                "952141",
                "191904",
                "3",
                "1",
                "600",
                "5"
        ) + System.lineSeparator();

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        ATM atm = AppConfig.devATM();
        atm.getLogin();

        String output = outBuffer.toString();

        // OptionMenu + AccountService should report a failure
        assertTrue(output.contains("Withdrawal failed:"), "Should show a withdrawal failed message");

        // We don't know the exact message text from the exception, but in your code it's:
        // "Withdrawal failed: Insufficient funds or invalid amount"
        assertTrue(output.contains("Insufficient funds"), "Should mention insufficient funds or invalid amount");

        // There should be no "New balance" line because withdraw failed
        assertFalse(output.contains("New balance:"), "No new balance should be printed on failure");
    }

    /**
     * Flow: login, transfer 200 from SAVINGS #1002 to CHECKING #1001, then exit.
     * Seeded balances: #1001: 500.00, #1002: 1200.00
     * After transfer:
     *   - source (SAVINGS #1002) 1200 - 200 = 1000
     *   - dest   (CHECKING #1001) 500 + 200 = 700
     */
    @Test
    void devATM_transferBetweenSeededAccounts_updatesBothBalances() {
        // 952141 (customer)
        // 191904 (pin)
        // 4 (Transfer)
        // 2 (pick source: SAVINGS #1002 — second in list)
        // 1 (pick destination: CHECKING #1001 — first in list)
        // 200 (amount)
        // 5 (Exit)
        String script = String.join(System.lineSeparator(),
                "952141",
                "191904",
                "4",
                "2",
                "1",
                "200",
                "5"
        ) + System.lineSeparator();

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        ATM atm = AppConfig.devATM();
        atm.getLogin();

        String output = outBuffer.toString();

        // Transfer flow confirmation
        assertTrue(output.contains("Transfer complete"), "Should confirm transfer completion");

        // Check the printed new balances match expectations
        assertTrue(output.contains("Source new balance: $1,000.00"),
                "Source (SAVINGS #1002) should be 1000.00 after transfer");
        assertTrue(output.contains("Dest   new balance: $700.00"),
                "Destination (CHECKING #1001) should be 700.00 after transfer");
    }

    /**
     * Login behavior: wrong PIN first, then correct PIN, then immediate exit.
     */
    @Test
    void devATM_wrongPinThenSuccess_showsErrorThenProceedsToMenu() {
        // 952141 / 999999 (wrong)
        // 952141 / 191904 (correct)
        // 5 (Exit)
        String script = String.join(System.lineSeparator(),
                "952141",
                "999999",
                "952141",
                "191904",
                "5"
        ) + System.lineSeparator();

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuffer));
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        ATM atm = AppConfig.devATM();
        atm.getLogin();

        String output = outBuffer.toString();

        // Wrong PIN message from OptionMenu.getLogin()
        assertTrue(output.contains("Wrong Customer Number or PIN"),
                "Should show error when PIN is incorrect");

        // And eventually the exit message from mainMenuLoop
        assertTrue(output.contains("Thank you for using ATM. Bye!"),
                "Should reach the menu and allow exit after correct login");
    }
}
