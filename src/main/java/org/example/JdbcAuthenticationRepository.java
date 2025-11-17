package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Authentication repository backed by the CUSTOMERPINS table.
 */
public class JdbcAuthenticationRepository implements OptionMenu.AuthenticationRepository {

    private static final String SQL_VERIFY =
            "SELECT PIN FROM CUSTOMERPINS WHERE CUSTOMERNUMBER = ?";

    private final OracleDBUtil db;

    public JdbcAuthenticationRepository(OracleDBUtil db) {
        this.db = Objects.requireNonNull(db, "OracleDBUtil is required");
    }

    @Override
    public boolean verify(int customerNumber, int pin) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_VERIFY)) {
            ps.setInt(1, customerNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == pin;
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to verify customer PIN", ex);
        }
    }
}
