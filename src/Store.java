import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Store {
    private static Connection conn;

    public static synchronized Connection connection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            new java.io.File("db").mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:db/drinks.db");
        }
        return conn;
    }

    public static void init() throws SQLException {
        new java.io.File("db").mkdirs();
        try (Connection setupConn = DriverManager.getConnection("jdbc:sqlite:db/drinks.db");
             Statement stmt = setupConn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            
            // Create tables
            stmt.execute("CREATE TABLE IF NOT EXISTS branches (id INTEGER PRIMARY KEY, name TEXT, city TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS drinks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, brand TEXT, price REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS stock (id INTEGER PRIMARY KEY AUTOINCREMENT, branch_id INTEGER, drink_id INTEGER, quantity INT, threshold INT, UNIQUE(branch_id, drink_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT, customer_phone TEXT, branch_id INTEGER, drink_id INTEGER, quantity INT, unit_price REAL, total_price REAL, order_time DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // Seed Branches
            stmt.execute("INSERT OR IGNORE INTO branches (id, name, city) VALUES " +
                "(1, 'HQ', 'Nairobi'), " +
                "(2, 'Nakuru Branch', 'Nakuru'), " +
                "(3, 'Mombasa Branch', 'Mombasa'), " +
                "(4, 'Kisumu Branch', 'Kisumu')");

            // Seed Drinks
            String[] drinks = {
               "(1, 'Tusker', 'EABL', 250.0)",
               "(2, 'White Cap', 'EABL', 250.0)",
               "(3, 'Guinness', 'EABL', 300.0)",
               "(4, 'Balozi', 'EABL', 250.0)",
               "(5, 'Pilsner', 'EABL', 250.0)",
               "(6, 'Water', 'Dasani', 100.0)",
               "(7, 'Coca Cola', 'Coca Cola', 100.0)",
               "(8, 'Fanta Orange', 'Coca Cola', 100.0)",
               "(9, 'Sprite', 'Coca Cola', 100.0)",
               "(10, 'Stoney', 'Coca Cola', 100.0)"
            };
            for (String drink : drinks) {
                stmt.execute("INSERT OR IGNORE INTO drinks (id, name, brand, price) VALUES " + drink);
            }

            // Seed Stock
            for (int b = 1; b <= 4; b++) {
                for (int d = 1; d <= 10; d++) {
                    stmt.execute("INSERT OR IGNORE INTO stock (branch_id, drink_id, quantity, threshold) VALUES (" + b + ", " + d + ", 50, 10)");
                }
            }
        }
    }
}
