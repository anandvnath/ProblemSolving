package my.leetcode.problems;

import my.leetcode.common.ListNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BinaryLinkedListToInteger1290Test {
  @Test
  public void testListToInteger() {
    List<ListNode> cases = new ArrayList<>();
    cases.add(ListNode.create(1, 0, 1));
    cases.add(ListNode.create(0));
    cases.add(ListNode.create(1));
    cases.add(ListNode.create(0, 0));
    cases.add(ListNode.create(0, 1));
    List<Integer> results = Arrays.asList(5, 0, 1, 0, 1);

    BinaryLinkedListToInteger1290 listToInteger = new BinaryLinkedListToInteger1290();

    for (int i = 0; i < cases.size(); i++)
      Assert.assertEquals(listToInteger.getDecimalValue(cases.get(i)), results.get(i).intValue());
  }
}
