# Library Loan Management System

This is a Java-based library system using Apache Derby. It features transaction management and performance tests to see how different database strategies affect speed.

## What it does
This system shows how to use JDBC for database connections and how to handle errors. It also includes a benchmarking tool to test things like batching and indexing.

## Main Features
- Transactions: Uses manual commits and savepoints to keep data safe.
- Speed Tests: Compares things like batch inserts vs individual ones.
- Error Handling: Checks for things like duplicate keys.
- Simple Schema: Tables for Members, Books, and Loans.

## Build and Execution

### Unix-based Systems (macOS/Linux)
```bash
mkdir -p bin
javac -cp "lib/derby.jar" -d bin src/utils/DatabaseUtils.java src/*.java
java -cp "bin:lib/derby.jar" MainApp
```

### Windows
```cmd
mkdir bin
javac -cp "lib/derby.jar" -d bin src/utils/DatabaseUtils.java src/*.java
java -cp "bin;lib/derby.jar" MainApp
```

## Performance Benchmarking
The evaluation framework executes each test suite 3–5 times to ensure statistical significance. A JVM warm-up phase is included to stabilize JIT compilation and Derby's internal buffer cache.

### Evaluated Strategies
1. Insert Strategy: Individual executeUpdate() vs. addBatch() execution.
2. Query Strategy: Full-table scan vs. indexed lookup on optimized columns.
3. Statement Type: Standard Statement vs. PreparedStatement overhead analysis.
4. Transaction Granularity: Per-operation commit vs. batched commit performance.

## Demo Transaction Isolation
The system's robustness can be verified through the CLI by selecting Option 8:

```text
=== Library Loan System ===
8. Demo Transaction Isolation
Enter choice: 8

--- Demonstrating Transaction Isolation & Rollback ---
Attempting to insert a duplicate book (Constraint Violation)...
Caught Expected Error: The statement was aborted because it would have caused a duplicate key value...
Transaction rolled back successfully. Data consistency preserved.
```

## Project Structure
- src/: Modular source code organized by responsibility (Connection, Transaction, Business, UI).
- lib/: External dependencies (Apache Derby).
- schema.sql/: Database initialization and indexing logic.
- analysis.md/: Technical deep-dive into performance and transaction behavior.
- librarydb/: Embedded database storage (automatically generated).

## Troubleshooting
- Classpath: Ensure the correct path separator is used (':' for Unix, ';' for Windows).
- Locks: If the database is locked by another process, the librarydb/ directory may need to be cleared manually to reset the state. ;)
