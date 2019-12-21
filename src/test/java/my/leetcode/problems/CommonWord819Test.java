package my.leetcode.problems;

import org.junit.Assert;
import org.junit.Test;

public class CommonWord819Test {
  @Test
  public void test() {
    Assert.assertEquals(new CommonWord819().mostCommonWord("Bob hit a ball, the hit BALL flew far after it was hit.",
        new String[] { "hit" }), "ball");
  }
}
