package com.nickallport.findmevinyl.discography

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * The note for each format as an earlier version stored them, in one column as JSON with the label as the key. Only
 * read now, by the step that moved them into copies (`MIGRATION_19_20`) and when an older backup is restored.
 */
object FormatNotes {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, String>>() {}.type

    /** The notes as an older version stored them, in the usual order of the labels. Nothing, or text that is not notes, is no notes. */
    fun decode(stored: String?): Map<String, String> {
        if (stored.isNullOrBlank()) return emptyMap()
        val read: Map<String, String?> = try {
            gson.fromJson<Map<String, String?>>(stored, type) ?: return emptyMap()
        } catch (e: JsonParseException) {
            return emptyMap()
        } catch (e: ClassCastException) {
            return emptyMap()
        }
        return ReleaseFormats.displayOrder
            .mapNotNull { label -> read[label]?.takeIf { it.isNotBlank() }?.let { label to it } }
            .toMap(LinkedHashMap())
    }
}
