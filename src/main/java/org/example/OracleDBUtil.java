/*
 Follow driver installation and setup instructions here:
 https://www.oracle.com/database/technologies/getting-started-using-jdbc.html
*/

// Replace 'app' with your package name
package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSource;

/**
 * Utility class that bootstraps a UCP pool for the Oracle Autonomous Database.
 * <p>
 * The pool can connect either with the raw connect descriptor (default) or, when
 * {@code TNS_ADMIN} is present, using an Autonomous Database wallet located in
 * that directory. In wallet mode the JDBC URL follows the
 * {@code jdbc:oracle:thin:@alias?TNS_ADMIN=/path/to/wallet} format so both the
 * wallet and password-based authentication are used.
 */
public class OracleDBUtil {
    // Replace USER_NAME, PASSWORD with your username and password
    private static final String DEFAULT_DB_USER = "ADMIN";
    private static final String DEFAULT_DB_PASSWORD = "tONGSSHIRT6767";
    private static final String DEFAULT_CONNECT_DESCRIPTOR = "(description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.ca-toronto-1.oraclecloud.com))(connect_data=(service_name=g7f714696e05215_a92l3f0ab96rm7kr_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))";
    private static final String DEFAULT_TNS_ALIAS = "g7f714696e05215_a92l3f0ab96rm7kr_high";
    private static final String ENV_DB_USER = "DB_USER";
    private static final String ENV_DB_PASSWORD = "DB_PASSWORD";
    private static final String ENV_DB_CONNECT_DESCRIPTOR = "DB_CONNECT_DESCRIPTOR";
    private static final String ENV_DB_TNS_ALIAS = "DB_TNS_ALIAS";
    private static final String ENV_TNS_ADMIN = "TNS_ADMIN";
    private final static String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.replay.OracleConnectionPoolDataSourceImpl";
    private PoolDataSource poolDataSource;
    public OracleDBUtil() throws SQLException {
        this.poolDataSource = PoolDataSourceFactory.getPoolDataSource();
        poolDataSource.setConnectionFactoryClassName(CONN_FACTORY_CLASS_NAME);
        poolDataSource.setURL(buildJdbcUrl());
        poolDataSource.setUser(resolveEnvOrDefault(ENV_DB_USER, DEFAULT_DB_USER));
        poolDataSource.setPassword(resolveEnvOrDefault(ENV_DB_PASSWORD, DEFAULT_DB_PASSWORD));
        poolDataSource.setConnectionPoolName("JDBC_UCP_POOL");
    }

    private static String buildJdbcUrl() {
        String tnsAdmin = System.getenv(ENV_TNS_ADMIN);
        if (tnsAdmin != null && !tnsAdmin.isBlank()) {
            Path walletDir = Paths.get(tnsAdmin).toAbsolutePath();
            if (!Files.isDirectory(walletDir)) {
                throw new IllegalStateException("TNS_ADMIN does not point to an existing directory: " + walletDir);
            }
            String tnsAlias = resolveEnvOrDefault(ENV_DB_TNS_ALIAS, DEFAULT_TNS_ALIAS);
            return String.format("jdbc:oracle:thin:@%s?TNS_ADMIN=%s", tnsAlias, walletDir);
        }

        String descriptor = resolveEnvOrDefault(ENV_DB_CONNECT_DESCRIPTOR, DEFAULT_CONNECT_DESCRIPTOR);
        return "jdbc:oracle:thin:@" + descriptor;
    }

    private static String resolveEnvOrDefault(String envKey, String defaultValue) {
        return Optional.ofNullable(System.getenv(envKey))
                .filter(v -> !v.isBlank())
                .orElse(defaultValue);
    }
    public void testConnection() {
        try {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
                if (rs.next()) {
                    System.out.println("Oracle Connection is working! Query result: " + rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not connect to the database - SQLException occurred: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return poolDataSource.getConnection();
    }

    public static void main(String[] args) {
        try {
            OracleDBUtil uds = new OracleDBUtil();
            uds.testConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
