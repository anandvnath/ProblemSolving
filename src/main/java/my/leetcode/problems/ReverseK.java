/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */

class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}

class ReverseK {
    public static void main(String[] args) {
        ReverseK reverseK = new ReverseK();
        ListNode head = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        int k = 2;
        printList(head);
        ListNode result = reverseK.reverseKGroup(head, k);

        printList(result); // Expected output: 2 -> 1 -> 4 -> 3 -> 5
    }

   public static void printList(ListNode head) {
       while (head != null) {
           System.out.print(head.val + " -> ");
           head = head.next;
       }
       System.out.println("null");
   }

    public ListNode reverseKGroup(ListNode head, int k) {
        ListNode result = null;
        ListNode prev = null;
        while(hasLength(head, k)) {
            ListNode[] separated = separateKNodes(head, k);
            head = separated[1];
            ListNode[] reversed = reverse(separated[0]);
            if (result == null) result = reversed[0];
            if (prev == null) {
                prev = reversed[1];
            } else {
                prev.next = reversed[0];
                prev = reversed[1];
            }
        }

        if (head != null) prev.next = head;
        return result;
    }

    private boolean hasLength(ListNode head, int k) {
        int i = 0;
        while (head != null) {
            i++;
            if (i == k) return true;
            head = head.next;
        }

        return false;
    }

    // Always called only when k nodes are present.
    private ListNode[] separateKNodes(ListNode node, int k) {
        ListNode head = node;

        for (int i = 1; i < k; i++) {
            node = node.next;
        }

        ListNode nextHead = node.next;
        node.next = null;
        return new ListNode[] { head, nextHead };
    }

    

    private ListNode[] reverse(ListNode node) {
        ListNode tail = node;
        ListNode prev = null;

        while (node != null) {
            ListNode next = node.next;
            node.next = prev;
            prev = node;
            node = next;
        }

        return new ListNode[] {prev, tail};

    }
}

