import java.sql.*;
import utils.DatabaseUtils;

public class ConnectionManager {
    private static final String DB_URL = "jdbc:derby:librarydb;create=true";
    private static final String SHUTDOWN_URL = "jdbc:derby:librarydb;shutdown=true";

    static {
        try {

            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: Apache Derby driver missing from classpath.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void shutdown() {
        try {
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {

            if (e.getSQLState().equals("08006")) {
                System.out.println("Database shutdown successfully.");
            }
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            DatabaseUtils.executeSqlScript(conn, "schema.sql");
            seedData(conn);
            System.out.println("Database initialization completed.");
        } catch (SQLException e) {
            System.err.println("Initialization failure: " + e.getMessage());
        }
    }

    private static void seedData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Members")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO Members (Name, Email) VALUES ('Alice Smith', 'alice@example.com')");
            stmt.executeUpdate("INSERT INTO Members (Name, Email) VALUES ('Bob Johnson', 'bob@example.com')");
            stmt.executeUpdate("INSERT INTO Books (ISBN, Title, Author) VALUES ('978-0061120084', 'To Kill a Mockingbird', 'Harper Lee')");
            stmt.executeUpdate("INSERT INTO Books (ISBN, Title, Author) VALUES ('978-0451524935', '1984', 'George Orwell')");
            System.out.println("Sample data added.");
        }
    }
}
