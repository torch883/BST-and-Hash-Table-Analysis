package analysis;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {
    private static final int WARMUP_ROUNDS = 4;
    private static final int MEASUREMENT_ROUNDS = 9;
    private static final long SEED = 20261011L;
    private static volatile long blackhole;

    private Benchmark() {
    }

    private record Key(int id, boolean colliding) implements Comparable<Key> {
        @Override
        public int compareTo(Key other) {
            int comparison = Integer.compare(id, other.id);
            return comparison != 0 ? comparison : Boolean.compare(colliding, other.colliding);
        }

        @Override
        public int hashCode() {
            return colliding ? 1 : id;
        }
    }

    private record Dataset(Key[] insertion, Key[] search, Key[] missing, Key[] deletion, Integer[] values) {
    }

    private record Sample(String scenario, int n, String structure, int round, int batches,
                          long insertion, long search, long missing, long deletion, double shape) {
        private double millis(int operation) {
            long duration = switch (operation) {
                case 0 -> insertion;
                case 1 -> search;
                case 2 -> missing;
                case 3 -> deletion;
                default -> throw new IllegalArgumentException();
            };
            return duration / (batches * 1_000_000.0);
        }
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ROOT);
        Path output = Path.of(args.length == 0 ? "generated-results" : args[0]);
        Files.createDirectories(output);
        Instant started = Instant.now();
        List<Sample> samples = new ArrayList<>();
        System.out.println("Initial JVM warmup...");
        for (int i = 0; i < 200; i++) {
            Dataset data = dataset(1_000, "random", SEED + i);
            measure("random", 1_000, "BST", -1, new Dataset[]{data});
            measure("random", 1_000, "HashTable", -1, new Dataset[]{data});
        }
        for (String scenario : List.of("random", "sorted", "collisions")) {
            for (int n : new int[]{100, 1_000, 10_000}) {
                int batches = Math.max(1, 10_000 / n);
                System.out.printf("%s, n=%d, batches=%d%n", scenario, n, batches);
                for (int round = -WARMUP_ROUNDS; round < MEASUREMENT_ROUNDS; round++) {
                    Dataset[] datasets = new Dataset[batches];
                    for (int batch = 0; batch < batches; batch++) {
                        datasets[batch] = dataset(n, scenario, SEED + 100_003L * (round + WARMUP_ROUNDS)
                                + 997L * batch + n);
                    }
                    String[] order = (round & 1) == 0
                            ? new String[]{"BST", "HashTable"} : new String[]{"HashTable", "BST"};
                    for (String structure : order) {
                        Sample sample = measure(scenario, n, structure, round + 1, datasets);
                        if (round >= 0) {
                            samples.add(sample);
                        }
                    }
                }
            }
        }
        writeResults(output, samples);
        writeEnvironment(output, started);
        System.out.println("Benchmark complete: " + output.toAbsolutePath());
        System.out.println("Checksum: " + blackhole);
    }

    private static Dataset dataset(int n, String scenario, long seed) {
        boolean colliding = scenario.equals("collisions");
        Key[] insertion = new Key[n];
        Key[] search = new Key[n];
        Key[] missing = new Key[n];
        Key[] deletion = new Key[n];
        Integer[] values = new Integer[n];
        for (int i = 0; i < n; i++) {
            insertion[i] = new Key(i, colliding);
            search[i] = new Key(i, colliding);
            missing[i] = new Key(n + i, colliding);
            deletion[i] = new Key(i, colliding);
            values[i] = i;
        }
        Random random = new Random(seed);
        if (!scenario.equals("sorted")) {
            shuffle(insertion, random);
        }
        shuffle(search, random);
        shuffle(missing, random);
        shuffle(deletion, random);
        return new Dataset(insertion, search, missing, deletion, values);
    }

    private static void shuffle(Key[] keys, Random random) {
        for (int i = keys.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Key temporary = keys[i];
            keys[i] = keys[j];
            keys[j] = temporary;
        }
    }

    private static Sample measure(String scenario, int n, String structure, int round, Dataset[] datasets) {
        long insertionTime = 0;
        long searchTime = 0;
        long missingTime = 0;
        long deletionTime = 0;
        double shape = 0;
        long expected = (long) n * (n - 1) / 2;
        for (Dataset data : datasets) {
            SimpleMap<Key, Integer> map = structure.equals("BST") ? new BinarySearchTree<>() : new HashTable<>();
            long started = System.nanoTime();
            for (Key key : data.insertion) {
                map.put(key, data.values[key.id]);
            }
            insertionTime += System.nanoTime() - started;
            require(map.size() == n, "Size after insertion");
            long foundSum = 0;
            started = System.nanoTime();
            for (Key key : data.search) {
                Integer value = map.get(key);
                foundSum += value == null ? -1 : value;
            }
            searchTime += System.nanoTime() - started;
            require(foundSum == expected, "Search checksum");
            long misses = 0;
            started = System.nanoTime();
            for (Key key : data.missing) {
                if (map.get(key) == null) {
                    misses++;
                }
            }
            missingTime += System.nanoTime() - started;
            require(misses == n, "Missing-key search");
            if (map instanceof BinarySearchTree<Key, Integer> tree) {
                shape += tree.height();
            } else if (map instanceof HashTable<Key, Integer> table) {
                shape += table.maxChainLength();
            }
            long removedSum = 0;
            started = System.nanoTime();
            for (Key key : data.deletion) {
                Integer value = map.remove(key);
                removedSum += value == null ? -1 : value;
            }
            deletionTime += System.nanoTime() - started;
            require(removedSum == expected && map.size() == 0, "Deletion checksum and final size");
            blackhole = blackhole + foundSum + misses + removedSum;
        }
        return new Sample(scenario, n, structure, round, datasets.length, insertionTime, searchTime,
                missingTime, deletionTime, shape / datasets.length);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static void writeResults(Path output, List<Sample> samples) throws IOException {
        StringBuilder raw = new StringBuilder("scenario,n,structure,round,batches,insert_total_ns,search_hit_total_ns,search_miss_total_ns,delete_total_ns,mean_height_or_max_chain\n");
        for (Sample sample : samples) {
            raw.append(String.format(Locale.ROOT, "%s,%d,%s,%d,%d,%d,%d,%d,%d,%.2f%n",
                    sample.scenario, sample.n, sample.structure, sample.round, sample.batches,
                    sample.insertion, sample.search, sample.missing, sample.deletion, sample.shape));
        }
        StringBuilder summary = new StringBuilder("scenario,n,structure,insert_median_ms,search_hit_median_ms,search_miss_median_ms,delete_median_ms,median_height_or_max_chain\n");
        StringBuilder table = new StringBuilder("| Scenario | n | Structure | Insert (ms) | Search hit (ms) | Search miss (ms) | Delete (ms) | Height / max chain |\n|---|---:|---|---:|---:|---:|---:|---:|\n");
        StringBuilder spread = new StringBuilder("scenario,n,structure,operation,min_ms,median_ms,max_ms\n");
        String[] operations = {"insert", "search_hit", "search_miss", "delete"};
        for (String scenario : List.of("random", "sorted", "collisions")) {
            for (int n : new int[]{100, 1_000, 10_000}) {
                for (String structure : List.of("BST", "HashTable")) {
                    List<Sample> group = samples.stream().filter(s -> s.scenario.equals(scenario)
                            && s.n == n && s.structure.equals(structure)).toList();
                    double[] medians = new double[4];
                    for (int operation = 0; operation < 4; operation++) {
                        int index = operation;
                        double[] values = group.stream().mapToDouble(s -> s.millis(index)).toArray();
                        medians[operation] = median(values);
                        spread.append(String.format(Locale.ROOT, "%s,%d,%s,%s,%.6f,%.6f,%.6f%n",
                                scenario, n, structure, operations[operation], Arrays.stream(values).min().orElseThrow(),
                                medians[operation], Arrays.stream(values).max().orElseThrow()));
                    }
                    double shape = median(group.stream().mapToDouble(Sample::shape).toArray());
                    summary.append(String.format(Locale.ROOT, "%s,%d,%s,%.6f,%.6f,%.6f,%.6f,%.2f%n",
                            scenario, n, structure, medians[0], medians[1], medians[2], medians[3], shape));
                    table.append(String.format(Locale.ROOT, "| %s | %,d | %s | %.6f | %.6f | %.6f | %.6f | %.2f |%n",
                            scenario, n, structure, medians[0], medians[1], medians[2], medians[3], shape));
                }
            }
        }
        Files.writeString(output.resolve("raw.csv"), raw, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("summary.csv"), summary, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("spread.csv"), spread, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("table.md"), table, StandardCharsets.UTF_8);
        System.out.print(table);
    }

    private static void writeEnvironment(Path output, Instant started) throws IOException {
        String text = "started_utc=" + started + "\nfinished_utc=" + Instant.now()
                + "\njava_version=" + System.getProperty("java.version")
                + "\njava_vm=" + System.getProperty("java.vm.name")
                + "\njava_vendor=" + System.getProperty("java.vendor")
                + "\nos=" + System.getProperty("os.name")
                + "\nos_version=" + System.getProperty("os.version")
                + "\narchitecture=" + System.getProperty("os.arch")
                + "\navailable_processors=" + Runtime.getRuntime().availableProcessors()
                + "\nmax_heap_bytes=" + Runtime.getRuntime().maxMemory()
                + "\njvm_arguments=" + ManagementFactory.getRuntimeMXBean().getInputArguments()
                + "\nseed=" + SEED + "\nwarmup_rounds_per_case=" + WARMUP_ROUNDS
                + "\nmeasurement_rounds_per_case=" + MEASUREMENT_ROUNDS
                + "\ninitial_warmup=200 random batches of 1000 keys per structure"
                + "\nbatches_per_round=max(1,10000/n)\nchecksum=" + blackhole + "\n";
        Files.writeString(output.resolve("environment.txt"), text, StandardCharsets.UTF_8);
    }
}
