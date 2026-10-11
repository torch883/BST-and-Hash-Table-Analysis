package analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BinarySearchTree<K extends Comparable<? super K>, V> implements SimpleMap<K, V> {
    private Node<K, V> root;
    private int size;

    private static final class Node<K, V> {
        private K key;
        private V value;
        private Node<K, V> left;
        private Node<K, V> right;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (root == null) {
            root = new Node<>(key, value);
            size++;
            return null;
        }
        Node<K, V> current = root;
        while (true) {
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            if (comparison < 0) {
                if (current.left == null) {
                    current.left = new Node<>(key, value);
                    size++;
                    return null;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new Node<>(key, value);
                    size++;
                    return null;
                }
                current = current.right;
            }
        }
    }

    @Override
    public V get(K key) {
        Objects.requireNonNull(key);
        Node<K, V> current = root;
        while (current != null) {
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                return current.value;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key);
        Node<K, V> parent = null;
        Node<K, V> current = root;
        while (current != null) {
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                break;
            }
            parent = current;
            current = comparison < 0 ? current.left : current.right;
        }
        if (current == null) {
            return null;
        }
        V oldValue = current.value;
        if (current.left != null && current.right != null) {
            Node<K, V> successorParent = current;
            Node<K, V> successor = current.right;
            while (successor.left != null) {
                successorParent = successor;
                successor = successor.left;
            }
            current.key = successor.key;
            current.value = successor.value;
            parent = successorParent;
            current = successor;
        }
        Node<K, V> child = current.left != null ? current.left : current.right;
        if (parent == null) {
            root = child;
        } else if (parent.left == current) {
            parent.left = child;
        } else {
            parent.right = child;
        }
        size--;
        return oldValue;
    }

    @Override
    public int size() {
        return size;
    }

    public int height() {
        if (root == null) {
            return 0;
        }
        ArrayDeque<Node<K, V>> queue = new ArrayDeque<>();
        queue.add(root);
        int height = 0;
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                Node<K, V> node = queue.remove();
                if (node.left != null) {
                    queue.add(node.left);
                }
                if (node.right != null) {
                    queue.add(node.right);
                }
            }
            height++;
        }
        return height;
    }

    public List<K> keysInOrder() {
        List<K> keys = new ArrayList<>(size);
        ArrayDeque<Node<K, V>> stack = new ArrayDeque<>();
        Node<K, V> current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            current = stack.pop();
            keys.add(current.key);
            current = current.right;
        }
        return keys;
    }
}
