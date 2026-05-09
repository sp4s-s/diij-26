import java.sql.*;

public class TransactionService {

    public static void processLoan(int bookId, int memberId) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            Savepoint savepoint = null;

            try {

                BusinessLogic.verifyBookAvailable(conn, bookId);
                BusinessLogic.updateBookStatus(conn, bookId, false);

                savepoint = conn.setSavepoint("InventoryLocked");

                try {

                    BusinessLogic.insertLoan(conn, bookId, memberId);

                    BusinessLogic.updateMemberLoanCount(conn, memberId, 1);
                } catch (SQLException e) {
                    if (savepoint != null) {
                        System.out.println("Partial transaction failure. Reverting to inventory lock state.");
                        conn.rollback(savepoint);

                        conn.rollback();
                        throw new SQLException("Critical failure during loan finalization: " + e.getMessage());
                    }
                }

                conn.commit();
                System.out.println("Transaction finished successfully.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static void demonstrateTransactionIsolation() {
        System.out.println("\n--- Testing Transaction Isolation ---");
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            System.out.println("Trying to insert a duplicate book...");
            String sql = "INSERT INTO Books (ISBN, Title, Author) VALUES ('978-0451524935', 'Duplicate Entry Test', 'System Author')";

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                conn.commit();
            } catch (SQLException e) {
                System.out.println("Expected Constraint Violation Caught: " + e.getMessage());
                conn.rollback();
                System.out.println("Transaction rolled back successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Unexpected failure during demonstration: " + e.getMessage());
        }
    }
}
