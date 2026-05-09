package utils;

import java.sql.*;
import java.io.*;
import java.util.Scanner;

public class DatabaseUtils {

    public static void executeSqlScript(Connection conn, String scriptPath) throws SQLException {
        try (InputStream is = new FileInputStream(scriptPath);
             Scanner s = new Scanner(is).useDelimiter(";")) {

            while (s.hasNext()) {
                String sql = s.next().trim();
                if (!sql.isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    } catch (SQLException e) {

                        if (!e.getSQLState().equals("X0Y32") && !e.getSQLState().equals("X0Y68")) {
                            throw e;
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("IO Error: Initialization script not found at " + scriptPath);
        } catch (IOException e) {
            System.err.println("IO Error: Failed to read database script: " + e.getMessage());
        }
    }
}
