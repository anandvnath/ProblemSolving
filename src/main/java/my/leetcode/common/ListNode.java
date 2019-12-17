package my.leetcode.common;

/**
 * ListNode implementation for linked list problems in leetcode.
 * This is not the best way to implement. But keeping this consistent with leetcode
 * so that the solutions can be run as is.
 */
public class ListNode {
  public int val;
  public ListNode next;

  public ListNode(int x) {
    val = x;
  }

  public static ListNode create(int... values) {
    ListNode root = null;
    ListNode node = null;
    for (int v : values) {
      ListNode newNode = new ListNode(v);
      if (root == null) {
        root = newNode;
        node = newNode;
      } else {
        node.next = newNode;
        node = newNode;
      }
    }
    return root;
  }

}
