fun main() {
    val solution = Solution()
    val n = 4
    val k = 5
    val result = solution.kthGrammar(n, k - 1)
    println("The ${k}th symbol in grammar row ${n} is: $result")
}

class Solution {
    val reference = mapOf<Int, List<Int>>(0 to listOf(0, 1), 1 to listOf(1, 0))
    
    fun kthGrammar(n: Int, k: Int): Int {
        if (n == 0 || k == 0) return -1
        if (n == 1) {
            if (k == 1) return 0
            else return -1
        }
        
        var nthRow = listOf<Int>(0)
        for (i in 2..n) {
            nthRow = nextRow(nthRow)
        }
        
        return nthRow[k]
    }

    fun nextRow(row: List<Int>): List<Int> = row.flatMap { reference[it] ?: emptyList() }

}