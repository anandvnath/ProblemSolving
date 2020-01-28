package my.leetcode.problems;

import java.util.HashMap;
import java.util.Map;

public class LRUCache {
  private static class Node {
    private int key;
    private int value;
    private Node next;
    private Node prev;

    public Node(int k, int v) {
      key = k;
      value = v;
    }
  }

  private Node head;
  private Node tail;
  private Map<Integer, Node> cache = new HashMap<>();
  private int capacity;

  public LRUCache(int capacity) {
    this.capacity = capacity;
  }

  public int get(int key) {
    if (!cache.containsKey(key)) return -1;
    Node node = cache.get(key);
    deleteNode(node);
    insertNode(node);
    return node.value;
  }

  public void put(int key, int value) {
    if (cache.containsKey(key)) {
      Node node = cache.get(key);
      node.value = value;
      get(key);
      return;
    }

    adjustSize();
    Node node = new Node(key, value);
    insertNode(node);
    cache.put(key, node);
  }

  private void adjustSize() {
    if (cache.size() < capacity) return;
    Node toRemove = tail;
    Node prev = tail.prev;
    prev.next = null;
    tail = prev;
    toRemove.prev = null;
    toRemove.next = null;
      cache.remove(toRemove.key);
  }

  private void deleteNode(Node node) {
    Node prev = node.prev;
    Node next = node.next;
    if (prev != null) prev.next = next;
    if (next != null) next.prev = prev;
    if (node == tail) tail = prev;
    node.prev = null;
    node.next = null;
  }

  private void insertNode(Node node) {
    node.next = head;
    if (head != null) head.prev = node;
    head = node;
    if (tail == null) tail = head;
  }

  public static void main(String[] args) {
    LRUCache cache = new LRUCache(1);
    cache.put(1, 1);
    System.out.println(cache.get(1));
    cache.put(2, 2);
    System.out.println(cache.get(1));
    System.out.println(cache.get(2));
  }
}
