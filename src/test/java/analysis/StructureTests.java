package analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

public final class StructureTests {
    private static long assertions;
    private static int groups;

    private StructureTests() {
    }

    public static void main(String[] args) {
        run("BST basic operations", () -> basic(new BinarySearchTree<>()));
        run("Hash table basic operations", () -> basic(new HashTable<>()));
        run("BST deletion cases", StructureTests::treeDeletion);
        run("BST sorted depth 10000", StructureTests::deepTree);
        run("Hash collisions and resizing", StructureTests::collisions);
        run("Negative and extreme integer keys", StructureTests::extremeKeys);
        run("BST null rejection", () -> nullRejection(new BinarySearchTree<>()));
        run("Hash table null rejection", () -> nullRejection(new HashTable<>()));
        run("BST randomized differential test", () -> differential(new BinarySearchTree<>()));
        run("Hash table randomized differential test", () -> differential(new HashTable<>()));
        System.out.printf("PASS: %d test groups, %d assertions%n", groups, assertions);
    }

    private static void run(String name, Runnable test) {
        test.run();
        groups++;
        System.out.println("PASS: " + name);
    }

    private static void equal(Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void basic(SimpleMap<Integer, String> map) {
        equal(0, map.size());
        equal(null, map.get(7));
        equal(null, map.remove(7));
        equal(null, map.put(7, "first"));
        equal("first", map.get(7));
        equal("first", map.put(7, "updated"));
        equal(1, map.size());
        equal("updated", map.remove(7));
        equal(0, map.size());
        equal(null, map.remove(7));
        equal(null, map.put(7, "again"));
        equal("again", map.get(7));
    }

    private static void treeDeletion() {
        BinarySearchTree<Integer, String> tree = new BinarySearchTree<>();
        for (int key : new int[]{50, 30, 80, 20, 40, 60, 90, 55, 57, 70}) {
            tree.put(key, "v" + key);
        }
        equal("v20", tree.remove(20));
        equal("v55", tree.remove(55));
        equal("v50", tree.remove(50));
        equal(List.of(30, 40, 57, 60, 70, 80, 90), tree.keysInOrder());
        for (int key : tree.keysInOrder()) {
            equal("v" + key, tree.get(key));
        }
        for (int key : new int[]{57, 30, 40, 60, 70, 80, 90}) {
            equal("v" + key, tree.remove(key));
        }
        equal(0, tree.height());
        equal(0, tree.size());
        equal(List.of(), tree.keysInOrder());
        tree.put(2, "two");
        tree.put(1, "one");
        equal("two", tree.remove(2));
        equal("one", tree.get(1));
        equal("one", tree.remove(1));
        equal(0, tree.size());
    }

    private static void deepTree() {
        BinarySearchTree<Integer, Integer> tree = new BinarySearchTree<>();
        for (int i = 0; i < 10_000; i++) {
            tree.put(i, i);
        }
        equal(10_000, tree.height());
        equal(10_000, tree.keysInOrder().size());
        equal(9_999, tree.get(9_999));
        equal(null, tree.get(10_000));
        for (int i = 9_999; i >= 0; i--) {
            equal(i, tree.remove(i));
        }
        equal(0, tree.size());
    }

    private record CollisionKey(int value) {
        @Override
        public int hashCode() {
            return Integer.MIN_VALUE;
        }
    }

    private static void collisions() {
        HashTable<CollisionKey, Integer> table = new HashTable<>();
        for (int i = 0; i < 1_000; i++) {
            equal(null, table.put(new CollisionKey(i), i));
        }
        equal(2_048, table.capacity());
        equal(1_000, table.maxChainLength());
        for (int i = 0; i < 1_000; i++) {
            equal(i, table.get(new CollisionKey(i)));
            equal(i, table.put(new CollisionKey(i), -i));
        }
        equal(1_000, table.size());
        equal(null, table.remove(new CollisionKey(2_000)));
        for (int i = 0; i < 1_000; i += 2) {
            equal(-i, table.remove(new CollisionKey(i)));
            equal(null, table.get(new CollisionKey(i)));
        }
        for (int i = 1; i < 1_000; i += 2) {
            equal(-i, table.remove(new CollisionKey(i)));
        }
        equal(0, table.size());
        equal(0, table.maxChainLength());
    }

    private static void extremeKeys() {
        List<SimpleMap<Integer, Integer>> maps = List.of(new BinarySearchTree<>(), new HashTable<>());
        for (SimpleMap<Integer, Integer> map : maps) {
            for (int key : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                equal(null, map.put(key, key));
            }
            for (int key : new int[]{Integer.MAX_VALUE, 0, -1, Integer.MIN_VALUE, 1}) {
                equal(key, map.get(key));
                equal(key, map.remove(key));
            }
            equal(0, map.size());
        }
    }

    private static void nullRejection(SimpleMap<Integer, Integer> map) {
        List<Runnable> actions = List.of(() -> map.put(null, 1), () -> map.put(1, null),
                () -> map.get(null), () -> map.remove(null));
        for (Runnable action : actions) {
            assertions++;
            try {
                action.run();
                throw new AssertionError("Expected NullPointerException");
            } catch (NullPointerException expected) {
                equal(0, map.size());
            }
        }
    }

    private static void differential(SimpleMap<Integer, Integer> map) {
        TreeMap<Integer, Integer> reference = new TreeMap<>();
        Random random = new Random(20261011L);
        for (int i = 0; i < 30_000; i++) {
            int key = random.nextInt(2_000) - 1_000;
            int value = random.nextInt();
            switch (random.nextInt(3)) {
                case 0 -> equal(reference.put(key, value), map.put(key, value));
                case 1 -> equal(reference.get(key), map.get(key));
                case 2 -> equal(reference.remove(key), map.remove(key));
                default -> throw new AssertionError();
            }
            equal(reference.size(), map.size());
            if (i % 1_000 == 0) {
                for (int probe = -1_000; probe < 1_000; probe++) {
                    equal(reference.get(probe), map.get(probe));
                }
                if (map instanceof BinarySearchTree<Integer, Integer> tree) {
                    equal(new ArrayList<>(reference.keySet()), tree.keysInOrder());
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : reference.entrySet()) {
            equal(entry.getValue(), map.remove(entry.getKey()));
        }
        equal(0, map.size());
    }
}
