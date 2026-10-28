package breakeven;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/breakeven_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root"; 
    private static final String PASSWORD = ""; 

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver missing. Check pom.xml for mysql-connector-j.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection failed. Ensure XAMPP MySQL service is running.");
            e.printStackTrace();
        }
        return null;
    }
}
