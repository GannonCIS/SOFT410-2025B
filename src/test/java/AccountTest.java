import org.example.Account;
import org.example.AccountType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void deposit_positiveIncreasesBalance() {
        Account a = new Account(123, 1001, AccountType.CHECKING, 100.00);

        boolean ok = a.deposit(50.00);

        Assertions.assertTrue(ok);
        Assertions.assertEquals(150.00, a.getAccountBalance(), 0.0001);
    }

    @Test
    void withdraw_positiveDecreasesBalance() {
        Account a = new Account(123, 1001, AccountType.CHECKING, 100.00);

        boolean ok = a.withdraw(50.00);

        Assertions.assertTrue(ok);
        Assertions.assertEquals(50.00, a.getAccountBalance(), 0.0001);
    }

    @Test
    void withdraw_bounce() {
        Account a = new Account(123, 1001, AccountType.CHECKING, 100.00);

        boolean ok = a.withdraw(150.00);

        Assertions.assertFalse(false);
        Assertions.assertEquals(100.00, a.getAccountBalance(), 0.0001);
    }
}
