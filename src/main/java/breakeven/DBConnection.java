package breakeven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens (and re-uses) the one JDBC connection the app needs.
 *
 * Settings come from an optional "db.properties" file in the folder the app is started from
 * (keys: db.url, db.user, db.password). That file is git-ignored so passwords never reach GitHub;
 * copy "db.properties.example" to "db.properties" to change them. Without the file the XAMPP defaults are used.
 *
 * OOP [ENCAPSULATION]: the settings and the shared connection are private.
 * Callers must NOT close the connection returned by getConnection(); close only your statements.
 */
public class DBConnection {
    private static final String CONFIG_FILE = "db.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/breakeven_db?useSSL=false&allowPublicKeyRetrieval=true";

    private static final String URL, USER, PASSWORD;
    private static Connection shared;

    static {
        Properties p = new Properties();
        File f = new File(CONFIG_FILE);
        if (f.isFile()) {
            try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            catch (IOException e) { System.err.println("Could not read " + CONFIG_FILE + ": " + e.getMessage() + " (using defaults)"); }
        }
        URL = p.getProperty("db.url", DEFAULT_URL).trim();
        USER = p.getProperty("db.user", "root").trim();
        PASSWORD = p.getProperty("db.password", "");
    }

    /** Returns a live connection, reconnecting if the old one died, or null if the database cannot be reached. */
    public static synchronized Connection getConnection() {
        try {
            if (shared != null && !shared.isClosed() && shared.isValid(1)) return shared;
        } catch (SQLException ignored) { /* fall through and reconnect */ }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("connectTimeout", "3000");   // don't freeze the window for long if MySQL is off
            shared = DriverManager.getConnection(URL, props);
            return shared;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver missing. Check pom.xml for mysql-connector-j.");
        } catch (SQLException e) {
            System.err.println("Database connection failed (" + e.getMessage() + "). Is the XAMPP MySQL service running?");
        }
        shared = null;
        return null;
    }
}
