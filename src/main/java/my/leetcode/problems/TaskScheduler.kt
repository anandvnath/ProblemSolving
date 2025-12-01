import java.util.PriorityQueue
import java.util.ArrayDeque

fun main() {
    val scheduler = TaskScheduler()
    val tasks = charArrayOf('A', 'A', 'A', 'B', 'B', 'B')
    val n = 2
    val result = scheduler.leastInterval(tasks, n)
    println("Least Interval: $result")
}

class TaskScheduler {

    fun leastInterval(tasks: CharArray, n: Int): Int {
        val freq = mutableMapOf<Char, Int>()
        val maxHeap = PriorityQueue<Int>(compareByDescending { it })
        val queue = ArrayDeque<Pair<Int, Int>>()
        var time = 0

        for (t in tasks) freq.put(t, freq.getOrDefault(t, 0) + 1)
        for (f in freq.values) maxHeap.offer(f)

        while (maxHeap.isNotEmpty()) {
            time++

            while (queue.isNotEmpty() && time >= queue.first().second) {
                maxHeap.offer(queue.removeFirst().first)
            }

            (maxHeap.poll() - 1).let {
                if (it > 0) queue.add(Pair(it, time + n + 1))
            }
        }

        return time
    }
}