package com.nickallport.findmevinyl.backup

/** What the settings page says after a backup or a restore. */
object BackupMessages {

    fun backedUp(summary: BackupSummary): String {
        val things = counts(summary.artists, summary.albums, summary.photos, summary.shops)
        return if (things.isEmpty()) {
            "Backed up your settings. There were no artists, albums, photos or shops yet."
        } else {
            "Backed up ${join(things)}."
        }
    }

    fun restored(result: RestoreResult): String = when (result) {
        is RestoreResult.Failed -> result.reason
        is RestoreResult.Done -> {
            val things = counts(result.artists, result.albums, result.photos, result.shops)
            when {
                things.isEmpty() && !result.settingsRestored ->
                    "Nothing new to restore. This phone already has everything in the file."
                things.isEmpty() -> "Restored your settings."
                result.settingsRestored -> "Restored ${join(things)}, and your settings."
                else -> "Restored ${join(things)}."
            }
        }
    }

    private fun counts(artists: Int, albums: Int, photos: Int, shops: Int): List<String> = buildList {
        if (artists > 0) add(count(artists, "artist"))
        if (albums > 0) add(count(albums, "album"))
        if (photos > 0) add(count(photos, "photo"))
        if (shops > 0) add(count(shops, "shop"))
    }

    private fun count(n: Int, noun: String) = if (n == 1) "1 $noun" else "$n ${noun}s"

    // "a", "a and b", "a, b and c".
    private fun join(items: List<String>): String =
        if (items.size == 1) items[0] else items.dropLast(1).joinToString(", ") + " and " + items.last()
}
