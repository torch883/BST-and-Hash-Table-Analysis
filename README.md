# BST and Hash Table Analysis

Java implementations, correctness tests, and measured insertion, search, and deletion times for 100, 1,000, and 10,000 elements.

Repository: https://github.com/torch883/BST-and-Hash-Table-Analysis

## Implementation

`BinarySearchTree<K,V>` is an ordinary, unbalanced binary search tree. Smaller keys go left and larger keys go right. Insertion follows this ordering until an empty child is reached. Search follows the same path. Deletion reconnects the parent to the remaining child when there are zero or one children. For two children, it copies the smallest key in the right subtree into the target node, then removes that successor. The implementation uses loops, so a tree with 10,000 levels does not cause recursive stack overflow. It does not perform rotations or balance itself.

`HashTable<K,V>` uses an array of buckets and separate chaining with linked nodes. A mixed hash selects a bucket; key equality resolves collisions. The initial capacity is 16. Before a new insertion would exceed a load factor of 0.75, the array doubles and existing nodes are redistributed. Resizing directly relinks nodes without calling `put`, so even the all-collision resize itself remains linear. Buckets stay linked lists; they are not converted into trees. Deletion unlinks the matching node, and the table does not shrink.

Both structures implement `SimpleMap`: `put`, `get`, `remove`, and `size`. Repeated insertion replaces the existing value without increasing the size. `put` and `remove` return the previous value, or `null` if the key was absent; `get` returns `null` for an absent key. Null keys and values are rejected. Keys must remain unchanged while stored; comparison and equality must be consistent. These classes are not thread-safe. Neither implementation uses a library map internally. `TreeMap` is used only as a test oracle.

## Time complexity

Let n be the current number of elements, h the BST height, and C the hash-table capacity. Comparison, equality, and hashing are assumed to take constant time, as they do for the fixed-size keys in these experiments. The table describes **one operation**, not a batch of n operations.

| Structure | Operation | Best case | Expected case | Worst case |
|---|---|---|---|---|
| Unbalanced BST | Insert / update | O(1) | O(log n) with random insertion order | O(n) |
| Unbalanced BST | Search | O(1) | O(log n) with random insertion order | O(n) |
| Unbalanced BST | Delete | O(1) | O(log n) for the random-tree workload | O(n) |
| Chained hash table | Insert / update | O(1) | O(1) amortized with well-distributed hashes | O(n) |
| Chained hash table | Search | O(1) | O(1) with well-distributed hashes and bounded load | O(n) |
| Chained hash table | Delete | O(1) | O(1) with well-distributed hashes and bounded load | O(n) |

BST operations take O(h). A search for the root, an insertion at an empty root child, or deletion of a root with at most one child can finish in constant time. Thus, O(log n) is not the absolute best case of an individual operation. If the tree is balanced, its height is O(log n), giving logarithmic worst-case operations on that tree. This implementation does not guarantee balance. Increasing sorted input creates a chain with h = n, so a deep operation takes O(n). Deleting a node with two children also includes finding its successor, still within O(h). Random insertion gives expected logarithmic height; arbitrary long update sequences are not guaranteed to preserve that expectation.

In the hash table, an empty bucket or a match at the head gives the constant best case. Under well-distributed hashing, the expected chain length is O(1 + n/C), and the load limit keeps this bounded. A particular insertion that expands the table costs O(n), but geometric growth spreads that cost across many insertions, yielding expected amortized O(1). If all keys hash to one bucket, finding or checking a key can inspect n nodes. All three operations therefore have a linear worst case. A low load factor alone cannot prevent this.

For complete batches, inserting n random keys into the BST takes expected O(n log n), while n well-distributed hash insertions take expected amortized O(n). Sorted BST insertion and all-collision hash insertion each take O(n²) in total. Searching all n keys in a chain likewise takes O(n²). Deleting all nodes in a random order from a degenerate tree or chain can also take O(n²); repeatedly removing a root or chain head would be a much easier workload.

BST storage is O(n), and its core operations use O(1) auxiliary space. Hash-table storage is O(n + C). Since this implementation does not shrink, C depends on the peak size, even after most elements are deleted. A resize temporarily holds both bucket arrays, using O(C) extra references. Diagnostic traversal and height calculations are outside the timed regions.

## Experimental method

The recorded experiment was run on the user's computer on **11 October 2026, 13:25–13:26, Asia/Qyzylorda (UTC+5)**.

| Item | Configuration |
|---|---|
| CPU | Intel Core i5-13420H, 8 cores / 12 logical processors |
| Memory | Approximately 16 GB installed RAM |
| Operating system | Windows 11 Pro, build 22631, 64-bit |
| Runtime | Oracle OpenJDK 27, OpenJDK 64-Bit Server VM |
| Source compatibility | Java 17 and newer |
| JVM heap options | `-Xms256m -Xmx1g` |
| Timer | `System.nanoTime()` |
| Base random seed | `20261011` |
| Repetitions | 4 discarded warmup rounds and 9 measured rounds per case |

The program first runs 200 additional random warmup batches of 1,000 elements per structure. It then measures three scenarios for each required size:

1. **random:** keys 0 through n−1 inserted in a seeded random order with ordinary hashes.
2. **sorted:** the same keys inserted in ascending order with ordinary hashes.
3. **collisions:** the same random insertion order, but every key's `hashCode()` returns 1. BST ordering remains unchanged.

Each round runs 100 independent batches for n = 100, 10 for n = 1,000, and 1 for n = 10,000. The reported sample is total timed nanoseconds divided by the number of batches. Each table entry is the median of these nine samples, converted to milliseconds. There are 162 raw samples: 3 scenarios × 3 sizes × 2 structures × 9 measured rounds.

Each batch starts with a new empty structure, inserts all n unique keys, searches all n existing keys, searches n absent keys, and finally deletes all n existing keys. Successful searches and deletions use independently shuffled orders. Missing keys are n through 2n−1, also shuffled. These misses deliberately go beyond the largest BST key; they are not a sample of every possible failed search. Equivalent but separately allocated search keys exercise key equality. Both structures receive identical datasets, and their execution order alternates between rounds.

Dataset creation, shuffling, construction of the empty structure, shape diagnostics, validation, and file output are excluded from timing. Insertion includes node allocation and hash-table resizing. The keys and boxed values are prepared beforehand. Search and deletion timings include loop and checksum work. Checksums and sizes are checked outside timing, and a volatile sink consumes results. Garbage collection, if it occurs inside a timed region, is included.

The final hash capacity is 256, 2,048, or 16,384 for the three sizes. In the ordinary-hash scenarios, the chosen integer IDs occupy distinct final buckets, which is a favorable hash distribution. The collision scenario deliberately tests the opposite extreme.

## Measured results

**Every time below is in milliseconds for a whole batch of n operations. It is not the time for one operation.** To obtain an average nanosecond cost per operation, multiply the displayed milliseconds by 1,000,000 and divide by n. The shape column is the median across rounds of the batch-average BST height or hash-table maximum chain length. Height counts nodes, so an empty tree has height 0 and a one-node tree has height 1.

| Scenario | n | Structure | Insert (ms) | Search hit (ms) | Search miss (ms) | Delete (ms) | Height / max chain |
|---|---:|---|---:|---:|---:|---:|---:|
| random | 100 | BST | 0.004684 | 0.004927 | 0.001288 | 0.005156 | 13.31 |
| random | 100 | HashTable | 0.003716 | 0.000869 | 0.000681 | 0.001014 | 1.00 |
| random | 1,000 | BST | 0.075920 | 0.079810 | 0.019250 | 0.082790 | 22.00 |
| random | 1,000 | HashTable | 0.024770 | 0.008720 | 0.004710 | 0.008980 | 1.00 |
| random | 10,000 | BST | 1.025900 | 1.159900 | 0.139000 | 1.058600 | 31.00 |
| random | 10,000 | HashTable | 0.253900 | 0.099500 | 0.097300 | 0.099600 | 1.00 |
| sorted | 100 | BST | 0.009077 | 0.010203 | 0.017863 | 0.006930 | 100.00 |
| sorted | 100 | HashTable | 0.002578 | 0.000944 | 0.000629 | 0.001052 | 1.00 |
| sorted | 1,000 | BST | 0.995440 | 1.126250 | 2.759740 | 0.575770 | 1000.00 |
| sorted | 1,000 | HashTable | 0.020080 | 0.003010 | 0.001540 | 0.003830 | 1.00 |
| sorted | 10,000 | BST | 112.745700 | 124.016900 | 284.733600 | 80.939100 | 10000.00 |
| sorted | 10,000 | HashTable | 0.149200 | 0.040300 | 0.064800 | 0.049500 | 1.00 |
| collisions | 100 | BST | 0.004533 | 0.005224 | 0.000979 | 0.004928 | 13.31 |
| collisions | 100 | HashTable | 0.010027 | 0.009739 | 0.015427 | 0.005903 | 100.00 |
| collisions | 1,000 | BST | 0.080730 | 0.086290 | 0.016840 | 0.087920 | 22.00 |
| collisions | 1,000 | HashTable | 1.109900 | 1.129630 | 2.270650 | 0.605170 | 1000.00 |
| collisions | 10,000 | BST | 1.472400 | 1.593400 | 0.130100 | 1.516200 | 31.00 |
| collisions | 10,000 | HashTable | 117.044400 | 124.182800 | 246.550700 | 74.829900 | 10000.00 |

The generated records are included for inspection:

- [summary.csv](results/summary.csv): median times used in this report.
- [raw.csv](results/raw.csv): actual summed nanosecond durations and batch counts for all 162 samples.
- [spread.csv](results/spread.csv): minimum, median, and maximum normalized batch times.
- [table.md](results/table.md): the generated table.
- [environment.txt](results/environment.txt): runtime, timing, seed, and heap details.
- [hardware.txt](results/hardware.txt): measured machine configuration.
- [run-log.txt](results/run-log.txt): correctness-test and benchmark console output.

## Comparison and interpretation

With **10,000 random keys**, insertion took 1.025900 ms for the BST and 0.253900 ms for the hash table. Successful search took 1.159900 ms versus 0.099500 ms, about **11.7 times faster** for the hash table. Deletion took 1.058600 ms versus 0.099600 ms, about **10.6 times faster**. The measured BST height was 31; the maximum ordinary hash chain was 1.

For the BST, sorted insertion of 10,000 keys took **112.745700 ms**, about **110 times** its random insertion time. Its height became 10,000. Successful search took 124.016900 ms and unsuccessful search took 284.733600 ms. Every failed search in this scenario travels the whole right chain. Sorted insertion time increased from 0.995440 ms at 1,000 keys to 112.745700 ms at 10,000, roughly 113 times for ten times the input, consistent with quadratic total work.

With **10,000 colliding keys**, hash-table insertion took **117.044400 ms**, compared with 0.253900 ms for ordinary random hashes. The chain length became 10,000, and successful search took 124.182800 ms. Here the BST search took only 1.593400 ms because changing the hash function does not change tree ordering. This demonstrates that constant expected hash-table time is conditional on hash distribution.

Deletion is measured on a shrinking structure, so its batch time should not be read as n identical deletions from a permanently full map. Random deletion avoids the especially favorable case of repeatedly removing the root of an ascending BST. Miss and hit timings also differ because they follow different paths.

Small absolute differences should be interpreted cautiously. For example, random insertion into the 100-element hash table ranged from 0.002439 to 0.007477 ms per normalized batch. Six decimal places preserve the measured data; they do not imply that measurements are accurate to one nanosecond.

## Why performance differs in practice

Tree shape and collision length determine how many nodes must be visited. Each node visit can involve a pointer chase, key comparison, and branch. Node objects may be scattered in memory, while the bucket array provides direct access to the start of a hash chain. Hashing also has its own cost, especially for larger keys. Resizing allocates an array and redistributes nodes, so some individual hash insertions are much slower than others.

Big-O notation does not describe allocation cost, CPU caches, branch prediction, JIT compilation, garbage collection, operating-system scheduling, power management, or background applications. These affect the recorded times. For example, sorted hash lookup was faster than random hash lookup in this run although both had maximum chain length 1. Allocation order and runtime effects are possible explanations, but this experiment does not isolate their individual contributions. The random and collision BST cases also differ in wall-clock time despite having the same insertion orders and heights.

This is a single-process educational benchmark, not a rigorous isolated microbenchmark suite. Warmup, batching, alternating order, medians, and raw data reduce some sources of error, but do not eliminate them. Results were not collected in multiple independent JVM forks, and no confidence intervals are claimed. Other machines and later runs may produce different numbers. The largest structural differences are the strongest evidence; small timing differences do not prove asymptotic complexity.

## Conclusions

The ordinary hash table was faster in the random-key experiments because its chains stayed short. An unbalanced BST was strongly affected by insertion order, and a hash table was strongly affected by collisions. Neither structure guarantees constant or logarithmic worst-case operations in this implementation. A balanced BST would protect against sorted-input degeneration, while a well-designed hash function and resizing policy support fast expected map operations. BST ordering also supports sorted traversal, which this hash table does not provide.

## Correctness testing

The test runner passed **10 groups and 256,666 assertions**. It checks empty structures, replacement without size growth, missing keys, reinsertion, null rejection, negative and extreme integer keys, BST leaf/one-child/two-child/root deletion, successor relinking, 10,000-level trees, heavy collisions, and repeated hash-table expansion. Each structure also receives 30,000 deterministic random operations checked against `TreeMap`, with periodic full-content comparisons. Tests throw `AssertionError` directly, so the `-ea` JVM option is not required. Compilation uses `--release 17 -Xlint:all -Werror`. The `Tests` configuration was also executed in IntelliJ IDEA 2026.2.3 and finished with the same assertion count and exit code 0.

## Run the project

No external libraries, Maven, or Gradle are required. Install a full JDK 17 or newer; a JRE alone is not sufficient.

### IntelliJ IDEA

Open this project directory and select a JDK under **File → Project Structure → Project SDK**. The portable `bst-hash-table.iml` marks `src/main/java` as sources and `src/test/java` as test sources. If IDEA asks which module to load, select that file. Choose **Tests** or **Benchmark** in the run-configuration list and press Run. The benchmark configuration sets the heap options and writes to `generated-results/`.

### PowerShell on Windows

From the project directory, with `JAVA_HOME` pointing to a JDK or `javac` on PATH:

```powershell
./run.ps1 -Mode all
./run.ps1 -Mode test
./run.ps1 -Mode benchmark
```

If necessary, provide the JDK installation directory explicitly:

```powershell
./run.ps1 -Mode all -Jdk 'C:/path/to/jdk'
```

The script uses the compiler and runtime from the same JDK to avoid accidentally running compiled classes with an older Java installation. New runs default to the ignored `generated-results/` directory, preserving the submitted `results/` snapshot. To deliberately replace that snapshot, use `-Results results`.

### macOS / Linux shell

```sh
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes src/main/java/analysis/*.java src/test/java/analysis/*.java
java -cp build/classes analysis.StructureTests
java -Xms256m -Xmx1g -cp build/classes analysis.Benchmark generated-results
```

The recorded Windows experiment was executed as follows:

```powershell
./run.ps1 -Mode all -Jdk 'C:/Users/user/.jdks/openjdk-27' -Results results
```
