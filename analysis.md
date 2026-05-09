# Performance and Transaction Analysis

## Database Transactions and Data Safety
The Library Loan system uses transactions to make sure data stays consistent even if an error happens.

### How it handles failures
- **Atomicity**: When we process a loan, we have to do several things: mark the book as out, create a loan record, and update the member's count. We group these together so they either all work or none of them do.
- **Savepoints**: We use Savepoints to allow the system to undo part of a transaction if something minor goes wrong, without canceling everything.
- **Rollbacks**: If there's a database error, we call `rollback()` to revert all changes so the database isn't left in a messy state.

## Speed Tests and JDBC Strategies
We ran some tests to see how different coding styles affect the database speed.

### Test Results Summary
| Strategy | Why it's better |
| :--- | :--- |
| **Batching** | It's much faster for inserting many records because it sends them all at once. |
| **PreparedStatements** | These are faster because the database only has to plan the query once. |
| **Indexes** | Adding an index (like on ISBN) makes searching way faster because it doesn't have to look at every row. |
| **Batched Commits** | Committing everything at the end of a big task is faster than committing after every single line. |

## Why it matters
- **Consistency vs Speed**: Transactions add a bit of a delay, but they are necessary to make sure the data is correct.
- **Security**: Using `PreparedStatement` also helps prevent SQL injection attacks.
- **Cleanup**: We use "try-with-resources" to make sure database connections are closed properly so we don't run out of memory.

## Notes on Derby
Apache Derby's performance can change after the first few runs because the JVM needs to optimize the code. We ran a few extra loops before recording the times to make sure the results were steady. :D
