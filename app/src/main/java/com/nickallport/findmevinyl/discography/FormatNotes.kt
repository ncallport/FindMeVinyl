package com.nickallport.findmevinyl.discography

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * The note the user keeps for each format of their copy ("LP: scratched", "CD: from Mum"), stored in one column as
 * JSON with the label as the key. Only a label of a format the copy is on is kept, with a trimmed, non-blank note
 * of at most [MAX_NOTE_LENGTH] characters, and they are read back in the usual order of the labels.
 */
object FormatNotes {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, String>>() {}.type

    /** The notes for [formats] as stored, or null when there is nothing to keep. */
    fun encode(notes: Map<String, String>, formats: Collection<String>): String? {
        val kept = ReleaseFormats.displayOrder
            .filter { it in formats }
            .mapNotNull { label -> notes[label]?.trim()?.take(MAX_NOTE_LENGTH)?.takeIf { it.isNotEmpty() }?.let { label to it } }
            .toMap(LinkedHashMap())
        return if (kept.isEmpty()) null else gson.toJson(kept)
    }

    /** What [encode] stored, in the usual order of the labels. Nothing, or text that is not notes, is no notes. */
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
