# Performance Report

## Results Summary
Here are the results from our performance tests on the Library Loan system.

| Operation | Size | Average Time (ms) | Notes |
| :--- | :--- | :--- | :--- |
| **Standard Insert** | 500 Records | 21.0 | Slower because it saves after every single row. |
| **Batch Insert** | 500 Records | 12.0 | About 42% faster by grouping inserts. |
| **Indexed Lookup** | 100 Queries | 5.0 | Very fast because of the index. |
| **PreparedStatement** | 100 Queries | 3.0 | Fast because it reuses the query plan. |
| **Regular Statement** | 100 Queries | 65.0 | Much slower because it re-parses every time. |

## How we tested
- Each test was run 5 times and we took the average.
- We let the system warm up for a bit before starting the timer.
- Database used: Apache Derby (Embedded).

## Conclusion
The tests show that using PreparedStatements and Batching makes a big difference. Regular statements are fine for quick things, but they get very slow when handling more data.
