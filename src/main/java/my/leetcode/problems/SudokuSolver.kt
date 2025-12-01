fun main() {
    val sudoku = SudokuSolver()
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
    sudoku.solveSudoku(board)
    println("Solved Sudoku Board:")
    for (row in board) {
        println(row.joinToString(" "))
    }
}

class SudokuSolver {
    val rows = Array(9) { mutableSetOf<Char>() }
    val cols = Array(9) { mutableSetOf<Char>() }
    val sqs = Array(9) { mutableSetOf<Char>() }
    var solvedBoard: Array<CharArray> = Array(9) { CharArray(9) }
    
    fun solveSudoku(board: Array<CharArray>): Unit  {
        setupContext(board)
        for (r in 0..8) {
            solveSudoku(board, r, 0)
        }

        for (i in 0..8) {
            for (j in 0..8) {
                    board[i][j] = solvedBoard[i][j]
            }
        }        
    }
    
    fun setupContext(board: Array<CharArray>) {
        for (r in 0..8) {
            for (c in 0..8) {
                val el = board[r][c]
                if (el == '.') continue

                val sq = (3 * (r / 3)) + (c % 3)

                rows[r].add(el)
                cols[c].add(el)
                sqs[sq].add(el)
            }
        }
    }
    
    fun solveSudoku(board: Array<CharArray>, r: Int, c: Int) {
        if (r == 8 && c > 8) {
            // solved case.
            for (i in 0..8) {
                for (j in 0..8) {
                    solvedBoard[i][j] = board[i][j]
                }
            }        
        }
        if (c > 8) return            
        if (board[r][c] != '.') solveSudoku(board, r, c + 1)
        
        for (e in 1..9) {
            val el = e.toChar()
            val sq = (3 * (r / 3)) + (c % 3)
            if (rows[r].contains(el) || cols[c].contains(el) || sqs[sq].contains(el)) continue
            board[r][c] = el
            rows[r].add(el)
            cols[c].add(el)
            sqs[sq].add(el)
            
            solveSudoku(board, r, c + 1)
            
            board[r][c] = '.'
            rows[r].remove(el)
            cols[c].remove(el)
            sqs[sq].remove(el)
        }
    }
}