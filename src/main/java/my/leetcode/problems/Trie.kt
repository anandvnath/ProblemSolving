fun main() {
    val wordDictionary = WordDictionary()
    wordDictionary.addWord("bad")
    wordDictionary.addWord("d")
    println(wordDictionary.search(".")) // return True
}

class WordDictionary() {

    data class Trie(val c: Char) {
        var center: Trie? = null
        var left: Trie? = null
        var right: Trie? = null
        var isEnd = false
    }

    var root: Trie? = null

    fun addWord(word: String) {
        root = insert(root, word, 0)
    }

    fun insert(trie: Trie?, word: String, index: Int): Trie {
        val c = word[index]
        val node = trie ?: Trie(c)

        when {
            c < node.c -> node.left = insert(node.left, word, index)
            c > node.c -> node.right = insert(node.right, word, index)
            else -> {
                if (index == word.lastIndex) {
                    println("Marking word as done for word $word char: ${node.c} ")
                    node.isEnd = true
                } else {
                    node.center = insert(node.center, word, index + 1)
                }
            }
        }

        return node
    }

    fun search(word: String): Boolean {
        return search(root, word, 0)
    }

    fun search(node: Trie?, word: String, index: Int): Boolean {
        if (node == null) return false
        if (index >= word.length) return false
        val c = word[index]
        if (index == word.lastIndex) {
            if (c == '.') {
                // Otherwise wait for any isEnd node in any part of the tree
                if (node.isEnd)
                    return true
            } else {
                return c == node.c && node.isEnd
            }
        }

        if (c == '.') {
            return search(node.center, word, index + 1) || search(node.left, word, index) || search(node.right, word, index)
        } 

        return when {
            c < node.c -> search(node.left, word, index)
            c > node.c -> search(node.right, word, index)
            else -> search(node.center, word, index + 1)
        }
    }

}

/**
 * Your WordDictionary object will be instantiated and called as such:
 * var obj = WordDictionary()
 * obj.addWord(word)
 * var param_2 = obj.search(word)
 */