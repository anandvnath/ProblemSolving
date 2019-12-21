package my.leetcode.problems;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * https://leetcode.com/problems/most-common-word/
 * Given a paragraph and a list of banned words, return the most frequent word that is not in the list of banned words.
 * It is guaranteed there is at least one word that isn't banned, and that the answer is unique.
 *
 * Words in the list of banned words are given in lowercase, and free of punctuation.
 * Words in the paragraph are not case sensitive.  The answer is in lowercase.
 */
public class CommonWord819 {
  public String mostCommonWord(String paragraph, String[] banned) {
    if (paragraph == null || paragraph.isEmpty()) return null;

    Set<String> bannedSet = new HashSet<>();
    for (String ban : banned)
      bannedSet.add(ban);

    StringBuilder sb = new StringBuilder();
    Map<String, Integer> freq = new HashMap<>();
    paragraph = paragraph.toLowerCase();

    for (char c : paragraph.toCharArray()) {
      if (c >= 'a' && c <= 'z')
        sb.append(c);
      else if (sb.length() != 0) {
        if (!bannedSet.contains(sb.toString())) {
          String key = sb.toString();
          freq.put(key, freq.getOrDefault(key, 0) + 1);
        }
        sb.setLength(0);
      }
    }

    if (sb.length() != 0 && !bannedSet.contains(sb.toString())) {
      String key = sb.toString();
      freq.put(key, freq.getOrDefault(key, 0) + 1);
    }

    int max = 0;
    String maxStr = null;
    for (String key : freq.keySet())
      if (max < freq.get(key)) {
        max = freq.get(key);
        maxStr = key;
      }

    return maxStr;
  }
}
