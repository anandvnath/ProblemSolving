fun main() {
    val codec = Codec()
    val root = TreeNode("1")
    root.left = TreeNode("2")
    root.right = TreeNode("3")
    root.left?.left = TreeNode("4")
    root.left?.right = TreeNode("5")

    val serializedData = codec.serialize(root)
    println("Serialized Tree: $serializedData")

    // val deserializedRoot = codec.deserialize(serializedData)
    // println("Deserialized Tree Root Value: ${deserializedRoot?.`val`}")
}

class TreeNode(var `val`: String) {
    var left: TreeNode? = null
    var right: TreeNode? = null
}

class Codec() {
	// Encodes a URL to a shortened URL.
    val serialized = mutableListOf<String?>()

    fun serialize(root: TreeNode?): String {
        val q = mutableListOf<TreeNode?>()
        q.add(root)

        while (q.isNotEmpty()) {
            val len = q.size
            for (i in 0 until len) {
                val el = q.removeAt(0)
                
                el?.let {
                    q.add(it.left)
                    q.add(it.right)
                    serialized.add(it.`val`)
                } ?: serialized.add(null)
            }
        }

        return serialized.joinToString()
    }

    // Decodes your encoded data to tree.
    fun deserialize(data: String): TreeNode? {
        return null
    }
}
