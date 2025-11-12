package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-backed implementation of {@link AccountRepository} that talks to Oracle.
 */
public class JdbcAccountRepository implements AccountRepository, OptionMenu.AccountRepository {

    private static final String BASE_SELECT =
            "SELECT account_number, customer_number, account_type, balance_cents FROM accounts WHERE customer_number = ?";

    private static final String SELECT_BY_CUSTOMER = BASE_SELECT + " ORDER BY account_number";

    private static final String SELECT_ONE = BASE_SELECT + " AND account_number = ?";

    private static final String UPDATE_BALANCE =
            "UPDATE accounts SET balance_cents = ? WHERE account_number = ? AND customer_number = ?";

    private static final String INSERT_ACCOUNT =
            "INSERT INTO accounts (customer_number, account_type, balance_cents) VALUES (?, ?, ?)";

    private final OracleDBUtil dbUtil;

    public JdbcAccountRepository(OracleDBUtil dbUtil) {
        this.dbUtil = Objects.requireNonNull(dbUtil, "dbUtil");
    }

    @Override
    public List<Account> findAllByCustomer(int customerNumber) {
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CUSTOMER)) {
            ps.setInt(1, customerNumber);
            try (ResultSet rs = ps.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
                return accounts;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load accounts for customer " + customerNumber, ex);
        }
    }

    @Override
    public Account findOneForCustomer(int customerNumber, int accountNumber) {
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ONE)) {
            ps.setInt(1, customerNumber);
            ps.setInt(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(rs);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Failed to load account " + accountNumber + " for customer " + customerNumber, ex);
        }
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account, "account");
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_BALANCE)) {
            ps.setLong(1, dollarsToCents(account.getAccountBalance()));
            ps.setInt(2, account.getAccountNumber());
            ps.setInt(3, account.getCustomerNumber());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("No rows updated for account " + account.getAccountNumber());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to persist account " + account.getAccountNumber(), ex);
        }
    }

    @Override
    public int create(int customerNumber, AccountType type, long initialCents) {
        Objects.requireNonNull(type, "type");
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_ACCOUNT, new String[] {"account_number"})) {
            ps.setInt(1, customerNumber);
            ps.setString(2, type.name());
            ps.setLong(3, initialCents);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Database did not return a generated account number");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create account for customer " + customerNumber, ex);
        }
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        int customerNumber = rs.getInt("customer_number");
        int accountNumber = rs.getInt("account_number");
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        long balanceCents = rs.getLong("balance_cents");
        double balance = centsToDollars(balanceCents);
        return new Account(customerNumber, accountNumber, type, balance);
    }

    private static double centsToDollars(long cents) {
        return cents / 100.0;
    }

    private static long dollarsToCents(double dollars) {
        return Math.round(dollars * 100.0);
    }
}
