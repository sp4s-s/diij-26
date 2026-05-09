import java.sql.*;

public class BusinessLogic {

    public static void verifyBookAvailable(Connection conn, int bookId) throws SQLException {
        String sql = "SELECT IsAvailable FROM Books WHERE BookID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next() || !rs.getBoolean(1)) {
                    throw new SQLException("Book ID " + bookId + " is already out.");
                }
            }
        }
    }

    public static void updateBookStatus(Connection conn, int bookId, boolean available) throws SQLException {
        String sql = "UPDATE Books SET IsAvailable = ? WHERE BookID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, available);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
        }
    }

    public static int insertLoan(Connection conn, int bookId, int memberId) throws SQLException {
        String sql = "INSERT INTO Loans (BookID, MemberID, LoanDate) VALUES (?, ?, CURRENT_DATE)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, memberId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Error: Could not save loan.");
    }

    public static void updateMemberLoanCount(Connection conn, int memberId, int delta) throws SQLException {
        String sql = "UPDATE Members SET ActiveLoans = ActiveLoans + ? WHERE MemberID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setInt(2, memberId);
            pstmt.executeUpdate();
        }
    }

    public static int registerMember(String name, String email) throws SQLException {
        String sql = "INSERT INTO Members (Name, Email) VALUES (?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Error: Could not register member.");
    }

    public static int addBook(String isbn, String title, String author) throws SQLException {
        String sql = "INSERT INTO Books (ISBN, Title, Author) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, isbn);
            pstmt.setString(2, title);
            pstmt.setString(3, author);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Error: Could not add book.");
    }

    public static void returnBook(int bookId) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {

                String loanSql = "SELECT LoanID, MemberID FROM Loans WHERE BookID = ? AND ReturnDate IS NULL";
                int loanId = -1;
                int memberId = -1;

                try (PreparedStatement pstmt = conn.prepareStatement(loanSql)) {
                    pstmt.setInt(1, bookId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            loanId = rs.getInt(1);
                            memberId = rs.getInt(2);
                        } else {
                            throw new SQLException("Error: No active loan found for Book ID " + bookId);
                        }
                    }
                }

                String updateLoanSql = "UPDATE Loans SET ReturnDate = CURRENT_DATE WHERE LoanID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateLoanSql)) {
                    pstmt.setInt(1, loanId);
                    pstmt.executeUpdate();
                }

                updateBookStatus(conn, bookId, true);
                updateMemberLoanCount(conn, memberId, -1);

                conn.commit();
                System.out.println("Book returned successfully.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static void queryActiveLoansByMember(int memberId) throws SQLException {
        String sql = "SELECT L.LoanID, B.Title, L.LoanDate FROM Loans L " +
                     "JOIN Books B ON L.BookID = B.BookID " +
                     "WHERE L.MemberID = ? AND L.ReturnDate IS NULL";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n--- Active Loans for Member " + memberId + " ---");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("Loan ID: %d | Title: %-25s | Issued: %s\n",
                        rs.getInt("LoanID"), rs.getString("Title"), rs.getDate("LoanDate"));
                }
                if (!found) System.out.println("No active loans found.");
            }
        }
    }

    public static void queryOverdueBooks() throws SQLException {

        String sql = "SELECT L.LoanID, M.Name, B.Title, L.LoanDate FROM Loans L " +
                     "JOIN Members M ON L.MemberID = M.MemberID " +
                     "JOIN Books B ON L.BookID = B.BookID " +
                     "WHERE L.ReturnDate IS NULL AND {fn TIMESTAMPDIFF(SQL_TSI_DAY, L.LoanDate, CURRENT_DATE)} > 14";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("\n--- Overdue Books (>14 Days) ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Loan ID: %d | Member: %-15s | Title: %-20s | Issued: %s\n",
                    rs.getInt("LoanID"), rs.getString("Name"), rs.getString("Title"), rs.getDate("LoanDate"));
            }
            if (!found) System.out.println("No overdue books found.");
        }
    }
}
