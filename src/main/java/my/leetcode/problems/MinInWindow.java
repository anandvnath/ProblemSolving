
import java.util.HashMap;
import java.util.Map;



class Solution {

    public static void main(String[] args) {
        Solution solution = new Solution();
        String s = "OUZODYXAZV";
        String t = "XYZ";
        String result = solution.minWindow(s, t);
        System.out.println("Result: " + result); // Expected output: "BANC"
    }

    public String minWindow(String s, String t) {
        if (s.length() < t.length()) return "";

        Map<Character, Integer> freq = new HashMap<>();

        for (char c : t.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }

        int start = 0;
        int end = 0;
        String result = "";

        while (end < s.length()) {
            char endChar = s.charAt(end++);
            // Chase down all characters we need
            if (freq.containsKey(endChar)) {
                freq.put(endChar, freq.get(endChar) - 1);
            } 

            // Once we find all characters
            while (foundAllChars(freq)) {
                char startChar = s.charAt(start);
                while (!freq.containsKey(startChar)) {
                    start++;
                    startChar = s.charAt(start);
                }

                String localResult = s.substring(start, end);
                System.out.println("Found window: " + localResult);
                if (result.equals("") || localResult.length() < result.length()) {
                    result = localResult;
                }
                freq.put(startChar, freq.get(startChar) + 1);
                start++;
            }
        }

        return result;
    }

    public boolean foundAllChars(Map<Character, Integer> freq) {
        for (int v: freq.values()) {
            if (v > 0) return false;
        }

        return true;
    }
}
