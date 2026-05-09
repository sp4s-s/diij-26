import pandas as pd
import matplotlib.pyplot as plt
import os

# Ensure plots directory exists
os.makedirs('evaluation/plots', exist_ok=True)
os.makedirs('report/figs', exist_ok=True)

def plot_transactions():
    df = pd.read_csv('evaluation/data/transactions.csv')
    plt.figure(figsize=(10, 6))
    plt.plot(df['Load'], df['IndividualCommit'], marker='o', label='Individual Commit')
    plt.plot(df['Load'], df['BatchedCommit'], marker='s', label='Batched Commit')
    plt.plot(df['Load'], df['SavepointNested'], marker='^', label='Savepoint Nested')
    plt.title('Transaction Strategy Latency Comparison')
    plt.xlabel('Transaction Load (Operations)')
    plt.ylabel('Execution Time (ms)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.savefig('evaluation/plots/transactions.png', dpi=300)
    plt.savefig('report/figs/transactions.png', dpi=300)
    plt.close()

def plot_batching():
    df = pd.read_csv('evaluation/data/batching.csv')
    plt.figure(figsize=(10, 6))
    plt.bar(df['Load'].astype(str), df['Individual'], label='Individual Insert', alpha=0.7)
    plt.bar(df['Load'].astype(str), df['Batch'], label='Batch Insert', alpha=0.7)
    plt.title('Batch Processing vs. Sequential Insertion')
    plt.xlabel('Record Count')
    plt.ylabel('Total Latency (ms)')
    plt.legend()
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.savefig('evaluation/plots/batching.png', dpi=300)
    plt.savefig('report/figs/batching.png', dpi=300)
    plt.close()

def plot_indexing():
    df = pd.read_csv('evaluation/data/indexing.csv')
    plt.figure(figsize=(10, 6))
    plt.plot(df['Queries'], df['FullScan'], marker='o', label='Full Table Scan')
    plt.plot(df['Queries'], df['IndexedLookup'], marker='s', label='B-Tree Indexed Lookup')
    plt.yscale('log')
    plt.title('Impact of Indexing on Query Latency (Log Scale)')
    plt.xlabel('Number of Queries')
    plt.ylabel('Time (ms)')
    plt.legend()
    plt.grid(True, which="both", ls="-", alpha=0.5)
    plt.savefig('evaluation/plots/indexing.png', dpi=300)
    plt.savefig('report/figs/indexing.png', dpi=300)
    plt.close()

def plot_statements():
    df = pd.read_csv('evaluation/data/statements.csv')
    plt.figure(figsize=(10, 6))
    plt.plot(df['Load'], df['StandardStatement'], marker='o', label='Standard Statement')
    plt.plot(df['Load'], df['PreparedStatement'], marker='s', label='PreparedStatement')
    plt.title('SQL Execution Strategy: Statement vs. PreparedStatement')
    plt.xlabel('Operation Complexity')
    plt.ylabel('Averaged Latency (ms)')
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.savefig('evaluation/plots/statements.png', dpi=300)
    plt.savefig('report/figs/statements.png', dpi=300)
    plt.close()

if __name__ == "__main__":
    plot_transactions()
    plot_batching()
    plot_indexing()
    plot_statements()
    print("Plots generated successfully in evaluation/plots/ and report/figs/")
