package com.nickallport.findmevinyl.ordering

/** Moving one item a place up or down a list the user orders by hand. */
object Reorder {
    /**
     * [ids] with [id] swapped with its neighbour: [delta] is -1 to move it up (towards the front) and
     * 1 to move it down. Null when nothing can move: the id is not in the list, or it is already at that end.
     */
    fun swapped(ids: List<String>, id: String, delta: Int): List<String>? {
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return null
        return ids.toMutableList().also {
            it[from] = it[to]
            it[to] = id
        }
    }
}
