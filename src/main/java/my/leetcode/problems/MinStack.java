package my.leetcode.problems;

import java.util.Stack;

public class MinStack {
  private Stack<Integer> stack = new Stack<>();
  private Stack<Integer> minStack = new Stack<>();

  /**
   * initialize your data structure here.
   */
  public MinStack() {

  }

  public void push(int x) {
    if (stack.isEmpty()) {
      stack.push(x);
      minStack.push(x);
      return;
    }

    stack.push(x);
    int minTop = minStack.peek();
    if (x < minTop) minStack.push(x);
    else minStack.push(minTop);
  }

  public void pop() {
    stack.pop();
    minStack.pop();
  }

  public int top() {
    return stack.peek();
  }

  public int getMin() {
    return minStack.peek();
  }

  public static void main(String[] args) {
    MinStack stack = new MinStack();
    stack.push(-2);
    stack.push(0);
    stack.push(-3);
    System.out.println(stack.getMin());
    stack.pop();
    System.out.println(stack.top());
    System.out.println(stack.getMin());
  }
}
