import java.util.Stack;

class Histogram {

    public static void main(String[] args) {
        Histogram histogram = new Histogram();
        int[] heights = {2,1,5,6,2,3};
        int result = histogram.largestRectangleArea(heights);
        System.out.println("Largest Rectangle Area: " + result); // Expected output: 10
    }
    
    public int largestRectangleArea(int[] heights) {
        int max = 0;
        Stack<int[]> stack = new Stack<>();

        for (int i = 0; i < heights.length; i++) {
            int start = i;
            int h = heights[i];
            while (!stack.isEmpty() && h <= stack.peek()[1]) {
                int[] popped = stack.pop();
                max = Math.max(max, (i - popped[0]) * popped[1]);
                System.out.println("Popped: " + popped[1] + " at index " + popped[0] + ", Max area: " + max);
                start = popped[0];
            } 
            stack.push(new int[] { start, h }); 
            System.out.println("stack is: " + stack.toString());
        }

        while (!stack.isEmpty()) {
            int[] popped = stack.pop();
            max = Math.max(max, (heights.length - popped[0]) * popped[1]);
            System.out.println("Popped: " + popped[1] + " at index " + popped[0] + ", Max area: " + max);
        }


        return max;
    }
}
