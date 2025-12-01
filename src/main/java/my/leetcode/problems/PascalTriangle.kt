fun main(args: Array<String>) {
    val triangle = PascalTriangle()
    val rowIndex = 3
    val row = triangle.getRow(rowIndex)
    println("Row $rowIndex of Pascal's Triangle: $row")
}


class PascalTriangle {

    val memo = mutableMapOf<Pair<Int, Int>, Int>()
    
    fun getRow(rowIndex: Int): List<Int> {
        return (0..rowIndex).map { pascal(rowIndex, it) }
    }
    
    fun pascal(row: Int, col: Int): Int {
        if (col == 0 || col == row) return 1
        
        val pair = Pair(row, col)
        return memo.getOrPut(pair) {
            pascal(row - 1, col - 1) + pascal(row - 1, col)
        }
    }
}