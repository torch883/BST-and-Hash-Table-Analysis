package analysis;

import java.util.Objects;

public final class HashTable<K, V> implements SimpleMap<K, V> {
    private Node<K, V>[] buckets = newBuckets(16);
    private int size;

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> next;

        private Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Node<K, V>[] newBuckets(int capacity) {
        return (Node<K, V>[]) new Node<?, ?>[capacity];
    }

    private int index(K key, int capacity) {
        int hash = key.hashCode();
        return (hash ^ (hash >>> 16)) & (capacity - 1);
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        int index = index(key, buckets.length);
        for (Node<K, V> node = buckets[index]; node != null; node = node.next) {
            if (node.key.equals(key)) {
                V oldValue = node.value;
                node.value = value;
                return oldValue;
            }
        }
        if (size >= buckets.length - buckets.length / 4) {
            resize();
            index = index(key, buckets.length);
        }
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        return null;
    }

    private void resize() {
        if (buckets.length >= 1 << 30) {
            throw new IllegalStateException("Maximum hash table capacity reached");
        }
        Node<K, V>[] expanded = newBuckets(buckets.length * 2);
        for (Node<K, V> bucket : buckets) {
            Node<K, V> node = bucket;
            while (node != null) {
                Node<K, V> next = node.next;
                int index = index(node.key, expanded.length);
                node.next = expanded[index];
                expanded[index] = node;
                node = next;
            }
        }
        buckets = expanded;
    }

    @Override
    public V get(K key) {
        Objects.requireNonNull(key);
        for (Node<K, V> node = buckets[index(key, buckets.length)]; node != null; node = node.next) {
            if (node.key.equals(key)) {
                return node.value;
            }
        }
        return null;
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key);
        int index = index(key, buckets.length);
        Node<K, V> previous = null;
        Node<K, V> node = buckets[index];
        while (node != null) {
            if (node.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = node.next;
                } else {
                    previous.next = node.next;
                }
                size--;
                return node.value;
            }
            previous = node;
            node = node.next;
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return buckets.length;
    }

    public int maxChainLength() {
        int maximum = 0;
        for (Node<K, V> bucket : buckets) {
            int length = 0;
            for (Node<K, V> node = bucket; node != null; node = node.next) {
                length++;
            }
            maximum = Math.max(maximum, length);
        }
        return maximum;
    }
}
