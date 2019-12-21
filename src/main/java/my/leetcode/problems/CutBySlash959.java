package my.leetcode.problems;

import java.util.*;

/**
 * https://leetcode.com/problems/regions-cut-by-slashes/
 * In a N x N grid composed of 1 x 1 squares, each 1 x 1 square consists of a /, \, or blank space.
 * These characters divide the square into contiguous regions.
 * (Note that backslash characters are escaped, so a \ is represented as "\\".)
 * Return the number of regions.
 *
 * Input:
 * [
 *   " /",
 *   "  "
 * ]
 * Output: 1
 *
 * Input:
 * [
 *   "\\/",
 *   "/\\"
 * ]
 * Output: 4
 */
public class CutBySlash959 {
  private Map<String, List<String>> adj = new HashMap<>();
  private Set<String> visited = new HashSet<>();

  public int regionsBySlashes(String[] grid) {
    int m = grid.length;
    if (m == 0) return 0;
    int n = grid[0].length();
    if (n == 0) return 0;

    for (int i = 0; i < m; i++) {
      String el = grid[i];
      if (el.length() != n) throw new IllegalArgumentException();
      char[] chars = el.toCharArray();
      for (int j = 0; j < n; j++) {
        char c = chars[j];
        String north = i + "N" + j;
        String south = i + "S" + j;
        String east = i + "E" + j;
        String west = i + "W" + j;
        if (c == '\\') {
          connect(west, south);
          connect(east, north);
        } else if (c == '/') {
          connect(west, north);
          connect(east, south);
        } else {
          connect(east, south);
          connect(south, west);
          connect(west, north);
          connect(north, east);
        }
      }
    }

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        int nextI = i + 1;
        int nextJ = j + 0;

        if (nextI >= 0 && nextJ >= 0
            && nextI < m && nextJ < n) {
          String south = i + "S" + j;
          String north = nextI + "N" + nextJ;
          connect(south, north);
        }

        nextI = i + 0;
        nextJ = j + 1;

        if (nextI >= 0 && nextJ >= 0
            && nextI < m && nextJ < n) {
          String east = i + "E" + j;
          String west = nextI + "W" + nextJ;
          connect(east, west);
        }
      }
    }


    int components = 0;
    for (String v : adj.keySet()) {
      if (!visited.contains(v)) {
        components++;
        dfs(v);
      }
    }

    return components;
  }

  private void dfs(String v) {
    visited.add(v);
    for (String w : adj.get(v))
      if (!visited.contains(w))
        dfs(w);
  }

  private void connect(String v, String w) {
    adj.computeIfAbsent(v, (k) -> new ArrayList<>()).add(w);
    adj.computeIfAbsent(w, (k) -> new ArrayList<>()).add(v);
  }
}
