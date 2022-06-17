package DataBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Parameter needed to get database connection.
 *
 * @author Emily Kast
 */

public class DatasourceConfig {


    /*
     * Change database url and login information depending on your database
     * */
    private static String dbUrl = "jdbc:postgresql://localhost/dbname";
    private static String user = "username";
    private static String password = "password";
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;


    static {
        config.setJdbcUrl(dbUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    /**
     * Get database connection.
     * @return database connection
     * @throws SQLException SQL Exception
     */
    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
