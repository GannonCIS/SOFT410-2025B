package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central place to wire dependencies.
 * Swap the in-memory repos with JDBC repos later without touching UI code.
 */
public final class AppConfig {
    private AppConfig() {}

    /** Build an ATM wired with in-memory repositories and seed data (great for dev/testing). */
    public static ATM devATM() {
        var auth = new InMemoryAuthRepo()
                .seed(952141, 191904)
                .seed(989947, 717976);

        var accounts = new InMemoryAccountRepo();
        // seed a few accounts
        accounts.seed(new Account(952141, 1001, AccountType.CHECKING, 500.00));
        accounts.seed(new Account(952141, 1002, AccountType.SAVINGS, 1200.00));
        accounts.seed(new Account(989947, 2001, AccountType.CHECKING, 250.00));

        var service = new SimpleAccountService(accounts);

        return new ATM(auth, accounts, service);
    }

    /** Build an ATM backed by the Oracle database. */
    public static ATM prodATM() {
        try {
            var auth = new InMemoryAuthRepo();
            var accounts = new JdbcAccountRepository(new OracleDBUtil());
            var service = new SimpleAccountService(accounts);
            return new ATM(auth, accounts, service);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to configure production ATM", ex);
        }
    }

    // ==================== In-memory repositories ====================

    /** In-memory Authentication (PINs as plain ints for demo). */
    static class InMemoryAuthRepo implements OptionMenu.AuthenticationRepository {
        private final Map<Integer, Integer> pins = new HashMap<>();
        InMemoryAuthRepo seed(int customer, int pin) { pins.put(customer, pin); return this; }
        @Override public boolean verify(int customerNumber, int pin) {
            return Objects.equals(pins.get(customerNumber), pin);
        }
    }

    /** In-memory Account repo (thread-safe maps). */
    static class InMemoryAccountRepo implements OptionMenu.AccountRepository, AccountRepository {
        private final Map<Integer, Account> byNo = new ConcurrentHashMap<>();
        private final Map<Integer, Set<Integer>> byCustomer = new ConcurrentHashMap<>();
        private final AtomicInteger nextAccountNumber = new AtomicInteger(2000);

        void seed(Account a) {
            byNo.put(a.getAccountNumber(), a);
            byCustomer.computeIfAbsent(a.getCustomerNumber(), k -> new LinkedHashSet<>())
                    .add(a.getAccountNumber());
            nextAccountNumber.accumulateAndGet(a.getAccountNumber(), Math::max);
        }

        @Override public List<Account> findAllByCustomer(int customerNumber) {
            var nos = byCustomer.getOrDefault(customerNumber, Set.of());
            var list = new ArrayList<Account>(nos.size());
            for (int n : nos) {
                var a = byNo.get(n);
                if (a != null) list.add(a);
            }
            return list;
        }

        @Override public Account findOneForCustomer(int customerNumber, int accountNumber) {
            var a = byNo.get(accountNumber);
            return (a != null && a.getCustomerNumber() == customerNumber) ? a : null;
        }
        @Override public void save(Account account) {
            Objects.requireNonNull(account, "account");
            byNo.put(account.getAccountNumber(), account);
            byCustomer.computeIfAbsent(account.getCustomerNumber(), k -> new LinkedHashSet<>())
                    .add(account.getAccountNumber());
        }

        @Override public int create(int customerNumber, AccountType type, long initialCents) {
            Objects.requireNonNull(type, "type");
            int accountNumber = nextAccountNumber.incrementAndGet();
            double balance = initialCents / 100.0;
            Account account = new Account(customerNumber, accountNumber, type, balance);
            save(account);
            return accountNumber;
        }
    }
}
