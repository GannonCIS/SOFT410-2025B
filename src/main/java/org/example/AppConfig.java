package org.example;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central place to wire dependencies.
 * Swap the in-memory repos with JDBC repos without touching UI code.
 */
public final class AppConfig {
    private AppConfig() {
    }

    /**
     * Build an ATM wired with in-memory repositories and seed data (great for dev/testing).
     */
    public static ATM devATM() {
        var auth = new InMemoryAuthRepo()
                .seed(952141, 191904)
                .seed(989947, 717976);

        var accounts = new InMemoryAccountRepo();
        accounts.seed(new Account(952141, 1001, AccountType.CHECKING, 500.00));
        accounts.seed(new Account(952141, 1002, AccountType.SAVINGS, 1200.00));
        accounts.seed(new Account(989947, 2001, AccountType.CHECKING, 250.00));

        AccountService service = new SimpleAccountService(accounts);

        return new ATM(auth, accounts, service);
    }

    /**
     * Build an ATM wired with JDBC repositories (use in production).
     */
    public static ATM prodATM() throws SQLException {
        OracleDBUtil dbUtil = new OracleDBUtil();

        var auth = new JdbcAuthenticationRepository(dbUtil);
        var accounts = new JdbcAccountRepository(dbUtil);
        AccountService service = new org.example.SimpleAccountService(accounts);

        return new ATM(auth, accounts, service);
    }

    // ==================== In-memory repositories ====================

    /**
     * In-memory Authentication (PINs as plain ints for demo).
     */
    static class InMemoryAuthRepo implements OptionMenu.AuthenticationRepository {
        private final Map<Integer, Integer> pins = new ConcurrentHashMap<>();

        InMemoryAuthRepo seed(int customer, int pin) {
            pins.put(customer, pin);
            return this;
        }

        @Override
        public boolean verify(int customerNumber, int pin) {
            return Objects.equals(pins.get(customerNumber), pin);
        }
    }

    /**
     * In-memory Account repo (thread-safe maps).
     */
    static class InMemoryAccountRepo implements AccountRepository {
        private final Map<Integer, Account> byNo = new ConcurrentHashMap<>();
        private final Map<Integer, Set<Integer>> byCustomer = new ConcurrentHashMap<>();
        private final AtomicInteger nextAccountNumber = new AtomicInteger(1000);

        void seed(Account account) {
            save(account);
            nextAccountNumber.accumulateAndGet(account.getAccountNumber() + 1, Math::max);
        }

        @Override
        public List<Account> findAllByCustomer(int customerNumber) {
            var nos = byCustomer.getOrDefault(customerNumber, Set.of());
            var list = new ArrayList<Account>(nos.size());
            for (int n : nos) {
                Account a = byNo.get(n);
                if (a != null) {
                    list.add(a);
                }
            }
            return list;
        }

        @Override
        public Account findOneForCustomer(int customerNumber, int accountNumber) {
            Account account = byNo.get(accountNumber);
            return (account != null && account.getCustomerNumber() == customerNumber) ? account : null;
        }

        @Override
        public void save(Account account) {
            byNo.put(account.getAccountNumber(), account);
            byCustomer.computeIfAbsent(account.getCustomerNumber(), k -> new LinkedHashSet<>())
                    .add(account.getAccountNumber());
        }

        @Override
        public int create(int customerNumber, AccountType type, long initialCents) {
            int accountNumber = nextAccountNumber.getAndIncrement();
            Account account = new Account(customerNumber, accountNumber, type, initialCents / 100.0);
            save(account);
            return accountNumber;



        }
    }
}
