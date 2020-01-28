package my.leetcode.problems;

import java.util.*;

public class WordSearchInBoard {
  private Map<String, Boolean> trie = new HashMap<>();
  private int[][] directions = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
  private char[][] board;
  private int m;
  private int n;
  private List<String> result = new ArrayList<>();

  public List<String> findWords(char[][] board, String[] words) {
    m = board.length;
    if (m == 0) return Collections.emptyList();
    n = board[0].length;
    if (n == 0) return Collections.emptyList();

    this.board = board;
    createTrie(words);
    StringBuilder aux = new StringBuilder();
    boolean[][] visited = new boolean[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        aux.setLength(0);
        aux.append(board[i][j]);
        for (boolean[] b : visited)
          Arrays.fill(b, false);
        visited[i][j] = true;
        findWords(i, j, visited, aux);
      }
    }

    return result;
  }


  private void findWords(int i, int j, boolean[][] visited, StringBuilder aux) {
    String soFar = aux.toString();
    if (!trie.containsKey(soFar)) return;
    if (trie.get(soFar) && !result.contains(soFar)) {
      result.add(soFar);
    }

    for (int[] dir : directions) {
      int nextI = i + dir[0];
      int nextJ = j + dir[1];

      if (nextI < 0 || nextJ < 0) continue;
      if (nextI >= m || nextJ >= n) continue;
      if (visited[nextI][nextJ]) continue;

      aux.append(board[nextI][nextJ]);
      visited[nextI][nextJ] = true;
      findWords(nextI, nextJ, visited, aux);
      aux.deleteCharAt(aux.length() - 1);
      visited[nextI][nextJ] = false;
    }
  }

  private void createTrie(String[] words) {
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      sb.setLength(0);
      for (int i = 0; i < w.length(); i++) {
        sb.append(w.charAt(i));
        trie.put(sb.toString(), trie.getOrDefault(sb.toString(), false));
      }

      trie.put(w, true);
    }
  }

  public static void main(String[] args) {
    WordSearchInBoard board = new WordSearchInBoard();
    System.out.println(board.findWords(new char[][]{
        {'o','a','a','n'},
        {'e','t','a','e'},
        {'i','h','k','r'},
        {'i','f','l','v'}
    }, new String[] {
        "oath","pea","eat","rain"
    }));
  }
}
