package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.example.AccountRepository;
/**
 * JDBC-backed implementation of account persistence.
 */
public class JdbcAccountRepository implements AccountRepository {

    private static final String SQL_FIND_ALL =
            "SELECT ACCOUNTNUMBER, ACCOUNTTYPE, ACCOUNTBALANCE " +
            "FROM CUSTOMERACCOUNTS WHERE CUSTOMERNUMBER = ? ORDER BY ACCOUNTNUMBER";

    private static final String SQL_FIND_ONE =
            "SELECT ACCOUNTNUMBER, ACCOUNTTYPE, ACCOUNTBALANCE " +
            "FROM CUSTOMERACCOUNTS WHERE CUSTOMERNUMBER = ? AND ACCOUNTNUMBER = ?";

    private static final String SQL_UPDATE_BALANCE =
            "UPDATE CUSTOMERACCOUNTS SET ACCOUNTBALANCE = ? " +
            "WHERE CUSTOMERNUMBER = ? AND ACCOUNTNUMBER = ?";

    private static final String SQL_INSERT =
            "INSERT INTO CUSTOMERACCOUNTS (CUSTOMERNUMBER, ACCOUNTNUMBER, ACCOUNTTYPE, ACCOUNTBALANCE) " +
            "VALUES (?, ?, ?, ?)";

    private final OracleDBUtil db;

    public JdbcAccountRepository(OracleDBUtil db) {
        this.db = Objects.requireNonNull(db, "OracleDBUtil is required");
    }

    @Override
    public List<Account> findAllByCustomer(int customerNumber) {
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL)) {
            ps.setInt(1, customerNumber);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(customerNumber, rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load accounts for customer " + customerNumber, ex);
        }
        return accounts;
    }

    @Override
    public Account findOneForCustomer(int customerNumber, int accountNumber) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ONE)) {
            ps.setInt(1, customerNumber);
            ps.setInt(2, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccount(customerNumber, rs);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load account " + accountNumber + " for customer " + customerNumber, ex);
        }
        return null;
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account, "account is required");
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            ps.setLong(1, toCents(account.getAccountBalance()));
            ps.setInt(2, account.getCustomerNumber());
            ps.setInt(3, account.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to persist account " + account.getAccountNumber(), ex);
        }
    }

    @Override
    public int create(int customerNumber, AccountType type, long initialCents) {
        Objects.requireNonNull(type, "type is required");
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            int accountNumber = allocateAccountNumber(conn);

            ps.setInt(1, customerNumber);
            ps.setInt(2, accountNumber);
            ps.setString(3, type.name());
            ps.setLong(4, initialCents);
            ps.executeUpdate();

            return accountNumber;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create account for customer " + customerNumber, ex);
        }
    }

    private Account mapAccount(int customerNumber, ResultSet rs) throws SQLException {
        int accountNumber = rs.getInt("ACCOUNTNUMBER");
        String typeText = rs.getString("ACCOUNTTYPE");
        if (typeText == null) {
            throw new SQLException("Account type was null for account " + accountNumber);
        }
        AccountType type = AccountType.valueOf(typeText.trim().toUpperCase(Locale.ROOT));
        long balanceCents = rs.getLong("ACCOUNTBALANCE");
        double balance = balanceCents / 100.0;
        return new Account(customerNumber, accountNumber, type, balance);
    }

    private int allocateAccountNumber(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CUSTOMERACCOUNTS_SEQ.NEXTVAL FROM DUAL")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException seqEx) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT NVL(MAX(ACCOUNTNUMBER), 0) + 1 FROM CUSTOMERACCOUNTS")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException maxEx) {
                seqEx.addSuppressed(maxEx);
                throw seqEx;
            }
            throw seqEx;
        }
        throw new SQLException("Unable to allocate new account number");
    }

    private long toCents(double amount) {
        return Math.round(amount * 100.0);
    }
}
