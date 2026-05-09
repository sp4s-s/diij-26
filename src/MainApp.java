import java.util.Scanner;
import java.sql.SQLException;

public class MainApp {

    public static void main(String[] args) {

        ConnectionManager.initializeDatabase();

        try (Scanner sc = new Scanner(System.in)) {
            boolean running = true;
            System.out.println("Library Loan Management System v1.0 initialized.");

            while (running) {
                printMenu();
                String input = sc.nextLine();
                int choice;

                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid Entry: Please provide a numeric operation code.");
                    continue;
                }

                try {
                    switch (choice) {
                        case 1 -> registerMember(sc);
                        case 2 -> addBook(sc);
                        case 3 -> processLoanCLI(sc);
                        case 4 -> returnBookCLI(sc);
                        case 5 -> PerformanceEvaluator.runAllBenchmarks();
                        case 6 -> queryActiveLoansCLI(sc);
                        case 7 -> BusinessLogic.queryOverdueBooks();
                        case 8 -> TransactionService.demonstrateTransactionIsolation();
                        case 0 -> running = false;
                        default -> System.out.println("Operation Error: Unknown operation code.");
                    }
                } catch (Exception e) {
                    System.err.println("Runtime Exception: " + e.getMessage());
                }
            }
        }

        ConnectionManager.shutdown();
        System.out.println("Application termination complete. :D");
    }

    private static void printMenu() {
        System.out.println("\n--- Library Menu ---");
        System.out.println("1. Register New Member");
        System.out.println("2. Catalog New Title");
        System.out.println("3. Issue Book");
        System.out.println("4. Return Book");
        System.out.println("5. Run Performance Tests");
        System.out.println("6. View Member Loans");
        System.out.println("7. View Overdue Books");
        System.out.println("8. Test Database Transactions");
        System.out.println("0. Terminate System");
        System.out.print("Select Operation Code: ");
    }

    private static void registerMember(Scanner sc) throws SQLException {
        System.out.print("Member Name: ");
        String name = sc.nextLine();
        System.out.print("Email Address: ");
        String email = sc.nextLine();
        int id = BusinessLogic.registerMember(name, email);
        System.out.println("Success: Member registered with ID: " + id);
    }

    private static void addBook(Scanner sc) throws SQLException {
        System.out.print("Resource ISBN: ");
        String isbn = sc.nextLine();
        System.out.print("Resource Title: ");
        String title = sc.nextLine();
        System.out.print("Primary Author: ");
        String author = sc.nextLine();
        int id = BusinessLogic.addBook(isbn, title, author);
        System.out.println("Success: Book added with ID: " + id);
    }

    private static void processLoanCLI(Scanner sc) throws SQLException {
        System.out.print("Target Book UID: ");
        int bookId = Integer.parseInt(sc.nextLine());
        System.out.print("Target Member UID: ");
        int memberId = Integer.parseInt(sc.nextLine());
        TransactionService.processLoan(bookId, memberId);
    }

    private static void returnBookCLI(Scanner sc) throws SQLException {
        System.out.print("Target Book UID: ");
        int bookId = Integer.parseInt(sc.nextLine());
        BusinessLogic.returnBook(bookId);
    }

    private static void queryActiveLoansCLI(Scanner sc) throws SQLException {
        System.out.print("Target Member UID: ");
        int memberId = Integer.parseInt(sc.nextLine());
        BusinessLogic.queryActiveLoansByMember(memberId);
    }
}
