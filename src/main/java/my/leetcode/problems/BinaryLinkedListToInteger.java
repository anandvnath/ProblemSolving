package my.leetcode.problems;

import my.leetcode.common.ListNode;

/**
 * https://leetcode.com/problems/convert-binary-number-in-a-linked-list-to-integer/
 * Given head which is a reference node to a singly-linked list. The value of each node in the linked list is either 0 or 1.
 * The linked list holds the binary representation of a number. Return the decimal value of the number in the linked list.
 */
public class BinaryLinkedListToInteger {
  public int getDecimalValue(ListNode head) {
    if (head == null) return 0;
    StringBuilder binStr = new StringBuilder();

    while (head != null) {
      binStr.append(head.val);
      head = head.next;
    }
    return Integer.parseInt(binStr.toString(), 2);
  }
}
