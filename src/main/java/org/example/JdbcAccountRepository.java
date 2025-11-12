package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC implementation backed by the Oracle connection pool provided by {@link OracleDBUtil}.
 */
public class JdbcAccountRepository implements AccountRepository {

    private final OracleDBUtil dbUtil;

    public JdbcAccountRepository(OracleDBUtil dbUtil) {
        this.dbUtil = Objects.requireNonNull(dbUtil, "OracleDBUtil is required");
    }

    @Override
    public List<Account> findAllByCustomer(int customerNumber) {
        String sql = "SELECT customer_number, account_number, account_type, balance_cents FROM accounts WHERE customer_number = ? ORDER BY account_number";
        List<Account> results = new ArrayList<>();
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to load accounts for customer " + customerNumber, ex);
        }
        return results;
    }

    @Override
    public Account findOneForCustomer(int customerNumber, int accountNumber) {
        String sql = "SELECT customer_number, account_number, account_type, balance_cents FROM accounts WHERE customer_number = ? AND account_number = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            ps.setInt(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to load account " + accountNumber + " for customer " + customerNumber, ex);
        }
        return null;
    }

    @Override
    public void save(Account account) {
        String sql = "UPDATE accounts SET balance_cents = ? WHERE customer_number = ? AND account_number = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, toCents(account.getAccountBalance()));
            ps.setInt(2, account.getCustomerNumber());
            ps.setInt(3, account.getAccountNumber());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("Account not found: " + account.getAccountNumber());
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to persist account " + account.getAccountNumber(), ex);
        }
    }

    @Override
    public int create(int customerNumber, AccountType type, long initialCents) {
        String sql = "INSERT INTO accounts (customer_number, account_type, balance_cents) VALUES (?, ?, ?)";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ACCOUNT_NUMBER"})) {
            ps.setInt(1, customerNumber);
            ps.setString(2, type.name());
            ps.setLong(3, initialCents);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new RepositoryException("Database did not return an account number");
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to create account for customer " + customerNumber, ex);
        }
    }

    private static Account mapRow(ResultSet rs) throws SQLException {
        int customerNumber = rs.getInt("customer_number");
        int accountNumber = rs.getInt("account_number");
        String type = rs.getString("account_type");
        long balanceCents = rs.getLong("balance_cents");
        AccountType accountType = AccountType.valueOf(type);
        double balance = balanceCents / 100.0;
        return new Account(customerNumber, accountNumber, accountType, balance);
    }

    private static long toCents(double amount) {
        return Math.round(amount * 100.0);
    }

    /** Runtime wrapper so callers do not have to deal with checked SQLExceptions. */
    public static class RepositoryException extends RuntimeException {
        public RepositoryException(String message) {
            super(message);
        }

        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
