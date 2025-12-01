fun main() {
    val sudoku = Sudoku()
    val board = arrayOf(
        charArrayOf('5', '3', '.', '.', '7', '.', '.', '.', '.'),
        charArrayOf('6', '.', '.', '1', '9', '5', '.', '.', '.'),
        charArrayOf('.', '9', '8', '.', '.', '.', '.', '6', '.'),
        charArrayOf('8', '.', '.', '.', '6', '.', '.', '.', '3'),
        charArrayOf('4', '.', '6', '8', '.', '3', '.', '.', '1'),
        charArrayOf('7', '.', '.', '.', '2', '.', '.', '.', '6'),
        charArrayOf('.', '6', '.', '.', '.', '.', '2', '8', '.'),
        charArrayOf('.', '.', '.', '4', '1', '9', '.', '.', '5'),
        charArrayOf('.', '.', '.', '.', '8', '.', '.', '7', '9')
    )
    val isValid = sudoku.isValidSudoku(board)
    println("Is the Sudoku board valid? $isValid")
}

class Sudoku {
    fun isValidSudoku(board: Array<CharArray>): Boolean {
        val rowSeen: Array<MutableSet<Int>> = Array(9) { mutableSetOf() }
        val colSeen: Array<MutableSet<Int>> = Array(9) { mutableSetOf() }
        val sqSeen: Array<MutableSet<Int>> = Array(9) { mutableSetOf() }

        for (row in 0..8) {
            for (col in 0..8) {
                val cell = board[row][col]
                
                if (cell == '.') continue
                val num = cell.digitToInt()
                
                if (rowSeen[row].contains(num)) return false
                rowSeen[row].add(num)

                if (colSeen[col].contains(num)) return false
                colSeen[col].add(num)

                val sqNum = (row - row % 3) + (col / 3)

                if (sqSeen[sqNum].contains(num)) return false
                sqSeen[sqNum].add(num)

            }
        }

        return true
    }
}
