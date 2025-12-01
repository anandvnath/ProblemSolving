
fun main() {
    val array = Array(3) { IntArray(3) }

    array[0][0] = 1
    array[0][1] = 2
    array[0][2] = 0
    array[1][0] = 0
    array[1][1] = 1
    array[1][2] = 0
    array[2][0] = 2
    array[2][1] = 1
    array[2][2] = 1

    println("Max value in array: ${array.flatten().maxOrNull()}")

    // val testArray = TestArray()
    // val visited = Array(3) { BooleanArray(3) }
    // visited[0][0] = true
    // visited[0][1] = true
    // visited[1][0] = true
    // visited[2][2] = true
    // val areaSoFar = 0
    // val result = testArray.currentIslandArea(visited, areaSoFar)
    // println("Current Island Area: $result")
}

class TestArray {
    fun currentIslandArea(visited: Array<BooleanArray>, areaSoFar: Int): Int {
            return (visited.sumOf { bArray ->
                bArray.count { it }
            }) - areaSoFar
        }
}
