package com.nickallport.findmevinyl.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val title: String,
    val category: String,
    val firstReleaseDate: String?,
    val owned: Boolean,
    val wanted: Boolean,
    /** MusicBrainz's format names for the album's releases, joined with `|`. Null: never fetched. */
    val formats: String? = null,
    /** When [formats] was fetched, in epoch milliseconds. Null: never fetched. */
    val formatsCheckedAt: Long? = null,
    /** Where the album sits in its artist's want list, lowest first. Equal values fall back to release date. */
    @ColumnInfo(defaultValue = "0") val wantPosition: Int = 0,
    /**
     * The statuses of the album's releases (`Official`, `Promotion`, `Bootleg`, `Other`), each once, sorted,
     * joined with `|`. Null: never fetched. Empty: fetched and no releases were listed.
     */
    val statuses: String? = null,
    /**
     * The formats the user says their own copy is on, as labels (LP, CD, ...) joined with `|`, for when
     * MusicBrainz does not list the release. Added to [formats], never replacing them, and never touched
     * by a lookup. Null: none added.
     */
    val userFormats: String? = null,
    /** The user's own note about their copy, such as the country or the language of the sleeve. Null: none. */
    val userNote: String? = null,
    /** The MusicBrainz release the user says is their copy, chosen from the album's pressings. Null: none chosen. */
    val myPressingId: String? = null,
    /**
     * A line that describes [myPressingId] (date, country, label, format), as it was when the user chose it, so the
     * album can still say which pressing it is when MusicBrainz's list of pressings has changed.
     */
    val myPressingText: String? = null
)
