import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class PerformanceEvaluator {

    private static final String DATA_DIR = "evaluation/data/";
    private static final int TRIALS = 5;

    public static void runAllBenchmarks() {
        System.out.println("=== Running Performance Tests ===");
        ensureDataDirExists();
        enableRuntimeStatistics();
        warmUp();

        benchmarkTransactionStrategies();

        benchmarkBatchProcessing();

        benchmarkIndexingImpact();

        benchmarkStatementComparison();

        System.out.println("=== Performance Analysis Completed. Results saved in evaluation/data/ ===");
    }

    private static void ensureDataDirExists() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    private static void enableRuntimeStatistics() {
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1)");
        } catch (SQLException ignored) {}
    }

    private static void warmUp() {
        System.out.println("Warming up the database...");
        for (int i = 0; i < 50; i++) {
            try (Connection c = ConnectionManager.getConnection();
                 Statement s = c.createStatement()) {
                s.executeQuery("SELECT COUNT(*) FROM Books").close();
            } catch (Exception ignored) {}
        }
    }

    private static void exportToCSV(String filename, String header, List<String> lines) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_DIR + filename))) {
            writer.println(header);
            for (String line : lines) {
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error exporting CSV: " + e.getMessage());
        }
    }

    private static void benchmarkTransactionStrategies() {
        System.out.println("Benchmarking Transaction Strategies...");
        List<String> results = new ArrayList<>();
        int[] loads = {100, 500, 1000, 2000};

        for (int load : loads) {
            long individual = runTask(() -> benchmarkPerOpCommit(load));
            long batched = runTask(() -> benchmarkBatchedCommit(load));
            long savepoint = runTask(() -> benchmarkSavepointTransaction(load));
            results.add(String.format("%d,%d,%d,%d", load, individual, batched, savepoint));
        }
        exportToCSV("transactions.csv", "Load,IndividualCommit,BatchedCommit,SavepointNested", results);
    }

    private static void benchmarkBatchProcessing() {
        System.out.println("Benchmarking Batch Processing...");
        List<String> results = new ArrayList<>();
        int[] loads = {500, 1000, 2500, 5000};

        for (int load : loads) {
            long individual = runTask(() -> benchmarkInsertIndividual(load));
            long batch = runTask(() -> benchmarkInsertBatch(load));
            results.add(String.format("%d,%d,%d", load, individual, batch));
        }
        exportToCSV("batching.csv", "Load,Individual,Batch", results);
    }

    private static void benchmarkIndexingImpact() {
        System.out.println("Benchmarking Indexing Impact...");
        List<String> results = new ArrayList<>();
        int[] loads = {100, 500, 1000};

        for (int load : loads) {
            long fullScan = runTask(() -> benchmarkFullScan(load));
            long indexed = runTask(() -> benchmarkIndexedLookup(load));
            results.add(String.format("%d,%d,%d", load, fullScan, indexed));
        }
        exportToCSV("indexing.csv", "Queries,FullScan,IndexedLookup", results);
    }

    private static void benchmarkStatementComparison() {
        System.out.println("Benchmarking Statement Types...");
        List<String> results = new ArrayList<>();
        int[] loads = {500, 1000, 2000};

        for (int load : loads) {
            long statement = runTask(() -> benchmarkStatement(load));
            long prepared = runTask(() -> benchmarkPreparedStatement(load));
            results.add(String.format("%d,%d,%d", load, statement, prepared));
        }
        exportToCSV("statements.csv", "Load,StandardStatement,PreparedStatement", results);
    }

    private static long runTask(BenchmarkTask task) {
        long total = 0;
        try {
            for (int i = 0; i < TRIALS; i++) {
                total += task.run();
                System.gc();
            }
        } catch (SQLException e) {
            System.err.println("Benchmark failed: " + e.getMessage());
        }
        return total / TRIALS;
    }

    @FunctionalInterface
    private interface BenchmarkTask {
        long run() throws SQLException;
    }

    private static long benchmarkInsertIndividual(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(true);
            String sql = "INSERT INTO Books (ISBN, Title, Author) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    pstmt.setString(1, UUID.randomUUID().toString().substring(0, 10));
                    pstmt.setString(2, "Bench");
                    pstmt.setString(3, "Author");
                    pstmt.executeUpdate();
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkInsertBatch(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO Books (ISBN, Title, Author) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    pstmt.setString(1, UUID.randomUUID().toString().substring(0, 10));
                    pstmt.setString(2, "Bench");
                    pstmt.setString(3, "Author");
                    pstmt.addBatch();
                    if (i % 500 == 0) pstmt.executeBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkFullScan(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            for (int i = 0; i < count; i++) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM Books WHERE Author = 'Unknown NonExistent Author'")) {
                    while (rs.next()) {}
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkIndexedLookup(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Books WHERE ISBN = ?")) {
            for (int i = 0; i < count; i++) {
                pstmt.setString(1, "NON-EXISTENT");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {}
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkPerOpCommit(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection()) {
            for (int i = 0; i < count; i++) {
                conn.setAutoCommit(true);
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE Books SET Title = ? WHERE BookID = 1")) {
                    pstmt.setString(1, "Updated " + i);
                    pstmt.executeUpdate();
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkBatchedCommit(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE Books SET Title = ? WHERE BookID = 1")) {
                for (int i = 0; i < count; i++) {
                    pstmt.setString(1, "Updated " + i);
                    pstmt.executeUpdate();
                }
            }
            conn.commit();
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkSavepointTransaction(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE Books SET Title = ? WHERE BookID = 1")) {
                for (int i = 0; i < count; i++) {
                    Savepoint sp = conn.setSavepoint("sp" + i);
                    pstmt.setString(1, "Updated SP " + i);
                    pstmt.executeUpdate();
                    if (i % 10 == 0) {
                        conn.releaseSavepoint(sp);
                    }
                }
            }
            conn.commit();
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkStatement(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            for (int i = 0; i < count; i++) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM Books WHERE BookID = " + (i % 10 + 1))) {
                    while (rs.next()) {}
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkPreparedStatement(int count) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Books WHERE BookID = ?")) {
            for (int i = 0; i < count; i++) {
                pstmt.setInt(1, i % 10 + 1);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {}
                }
            }
        }
        return System.currentTimeMillis() - start;
    }
}
