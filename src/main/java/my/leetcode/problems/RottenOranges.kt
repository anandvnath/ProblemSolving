fun main() {
    val rottenOranges = RottenOranges()
    val grid = arrayOf(
        intArrayOf(2,1,1),
        intArrayOf(1,1,0),
        intArrayOf(0,1,1)
    )
    val result = rottenOranges.orangesRotting(grid)
    println("Minutes to rot all oranges: $result")
}

class RottenOranges {
    fun orangesRotting(grid: Array<IntArray>): Int {
        val rows = grid.size
        val cols = grid[0].size




    }
}