package my.leetcode.problems;

import java.util.*;

public class DivideArrayConsecutive5292 {
  public boolean isPossibleDivide(int[] nums, int k) {
    int n = nums.length;
    if (n == 0) return k == 0;
    if (k == 0) return false;

    if (n % k != 0) return false;

    Map<Integer, Integer> freq = new HashMap<>();
    for (int num : nums)
      freq.put(num, freq.getOrDefault(num, 0) + 1);

    while (!freq.isEmpty()) {
      List<Integer> keys = new ArrayList(freq.keySet());
      Collections.sort(keys);
      int start = keys.get(0);

      for (int i = 0; i < k; i++) {
        if (!freq.containsKey(start))
          return false;
        freq.put(start, freq.get(start) - 1);
        if (freq.get(start) == 0)
          freq.remove(start);
        start++;
      }
    }

    return true;
  }

  public static void main(String[] args) {
    System.out.println(new DivideArrayConsecutive5292().isPossibleDivide(new int[] {1,2,3,3,4,4,5,6}, 4));
  }
}
