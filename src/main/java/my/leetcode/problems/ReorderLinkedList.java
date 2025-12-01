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
    public void printList() {
        ListNode current = this;
        while (current != null) {
            System.out.print(current.val + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}

class ReorderLinkedList {
    public static void main(String[] args) {
        ReorderLinkedList reorderLinkedList = new ReorderLinkedList();
        ListNode first = new ListNode(2, new ListNode(6, new ListNode(3)));
        ListNode second = new ListNode(4, new ListNode(8));

        first.printList();
        second.printList();

        second = reorderLinkedList.reverse(second);
        second.printList();

        reorderLinkedList.merge(first, second);
        System.out.print("Merged list: ");
        first.printList();


        // ListNode head = new ListNode(2, new ListNode(4, new ListNode(6, new ListNode(8))));
        // System.out.print("Original list: ");
        // head.printList();
        // reorderLinkedList.reorderList(head);
        // System.out.print("Reordered list: ");
        // head.printList();
    }


    public void reorderList(ListNode head) {
        if (head == null) return;

        ListNode mid = head;
        ListNode fast = head;

        while (fast != null && fast.next != null) {
            fast = fast.next.next;
            mid = mid.next;
        }

        ListNode first = head;
        ListNode second = mid.next;
        mid.next = null;

        second = reverse(second);

        merge(first, second);
    }

    private void merge(ListNode first, ListNode second) {
        while (first != null && second != null) {
            ListNode temp = first.next;
            first.next = second;
            first = temp;
            
            temp = second.next;
            second.next = first;
            second = temp;
        }
    }

    private ListNode reverse(ListNode head) {
        ListNode prev = null;

        while (head != null) {
            ListNode next = head.next;
            head.next = prev;
            prev = head;
            head = next;
        }

        return prev;
    }


}
