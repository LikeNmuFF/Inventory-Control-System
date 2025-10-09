import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DB_Connection {
    // ⚠️ IMPORTANT: UPDATE THESE CREDENTIALS FOR YOUR LOCAL SETUP
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/ics_db"; // Database name from SQL dump is 'ics_db'
    private static final String USER = "root"; // Common default user for XAMPP/WAMP
    private static final String PASS = ""; // Common default password for XAMPP/WAMP

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Load the driver class
            Class.forName(DRIVER);
            // Establish the connection
            conn = DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found. Please add the library.", "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Failed to connect to the database 'ics_db'. Check your credentials and server status.", "Database Connection Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        return conn;
    }
    
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}