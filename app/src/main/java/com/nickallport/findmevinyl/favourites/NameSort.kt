package com.nickallport.findmevinyl.favourites

/**
 * How names are put A to Z: capitals do not count, and neither does a leading "The", so The Smiths is under S. Nothing
 * cleverer than that. Names that sort the same stay in the order they were given.
 */
object NameSort : Comparator<String> {
    /** The name to sort by: trimmed, without a leading "The ". */
    fun key(name: String): String {
        val trimmed = name.trim()
        return if (trimmed.length > 4 && trimmed.startsWith("the ", ignoreCase = true)) trimmed.substring(4).trimStart() else trimmed
    }

    override fun compare(a: String, b: String): Int = String.CASE_INSENSITIVE_ORDER.compare(key(a), key(b))
}
