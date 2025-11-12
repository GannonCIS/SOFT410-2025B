/*
 Follow driver installation and setup instructions here:
 https://www.oracle.com/database/technologies/getting-started-using-jdbc.html
*/

// Replace 'app' with your package name
package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSource;

public class OracleDBUtil {
    // Replace USER_NAME, PASSWORD with your username and password
    private final static String DB_USER = "ADMIN";
    private final static String DB_PASSWORD = "tONGSSHIRT6767";
    // If you want to connect using Wallet, comment the following line.
    private final static String CONNECT_STRING = "(description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.ca-toronto-1.oraclecloud.com))(connect_data=(service_name=g7f714696e05215_a92l3f0ab96rm7kr_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))";
    /*
      If you want to connect with a wallet, uncomment the CONNECT_STRING line below (this is only applicable for Oracle Database 23ai versions and THICK MODE).
      dbname - is the TNS alias present in tnsnames.ora.
    */
    // private final static String CONNECT_STRING ="dbname_medium";
    private final static String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.replay.OracleConnectionPoolDataSourceImpl";
    private PoolDataSource poolDataSource;
    public OracleDBUtil() throws SQLException {
        this.poolDataSource = PoolDataSourceFactory.getPoolDataSource();
        poolDataSource.setConnectionFactoryClassName(CONN_FACTORY_CLASS_NAME);
        // If THICK mode is needed, comment the following line. Unset TNS_ADMIN environment variable if you are using THIN mode
        poolDataSource.setURL("jdbc:oracle:thin:@" + CONNECT_STRING);
		/*
		 If THICK mode is needed, uncomment the following poolDataSource.setURL line.
		  Note:
		  1. You should download and unzip the wallet. You should also set the TNS_ADMIN environment variable. Otherwise, it will not work for oci8 driver.
		   TNS_ADMIN - Should be the path where the client credentials zip (wallet_dbname.zip) file is downloaded.
		  2. Edit Wallet location value in sqlnet.ora
		*/
        // poolDataSource.setURL("jdbc:oracle:oci8:@" + CONNECT_STRING);
        poolDataSource.setUser(DB_USER);
        poolDataSource.setPassword(DB_PASSWORD);
        poolDataSource.setConnectionPoolName("JDBC_UCP_POOL");
    }

    public Connection getConnection() throws SQLException {
        return poolDataSource.getConnection();
    }
    public void testConnection() {
        try {
            try (Connection conn = poolDataSource.getConnection();
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
    public static void main(String[] args) {
        try {
            OracleDBUtil uds = new OracleDBUtil();
            uds.testConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}