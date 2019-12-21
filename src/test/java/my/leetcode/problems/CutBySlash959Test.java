package my.leetcode.problems;

import org.junit.Assert;
import org.junit.Test;

public class CutBySlash959Test {
  @Test
  public void test() {
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{"/"}),  2);
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{"\\"}),  2);
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{" "}),  1);
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{"\\/", "/\\"}),  4);
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{"/\\", "\\/"}),  5);
    Assert.assertEquals(new CutBySlash959().regionsBySlashes(new String[]{"//", "/ "}),  3);
  }
}
