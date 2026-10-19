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
    @Deprecated("Copied into album_copies by MIGRATION_19_20, no longer read or written.")
    val userFormats: String? = null,
    /** The user's own note about their copy, such as the country or the language of the sleeve. Null: none. */
    @Deprecated("Moved into a copy by MIGRATION_20_21, no longer read or written.")
    val userNote: String? = null,
    /** The MusicBrainz release the user says is their copy, chosen from the album's pressings. Null: none chosen. */
    val myPressingId: String? = null,
    /**
     * A line that describes [myPressingId] (date, country, label, format), as it was when the user chose it, so the
     * album can still say which pressing it is when MusicBrainz's list of pressings has changed.
     */
    val myPressingText: String? = null,
    /**
     * The format the user wants the album on (a label such as LP), while it is wanted. Null: any format, or not wanted.
     * It goes when the want does, and the want goes when the user says their copy is on this format.
     */
    val wantFormat: String? = null,
    /** The user's note for each format of their copy, as JSON with the label as the key (see `FormatNotes`). Null: none. */
    @Deprecated("Copied into album_copies by MIGRATION_19_20, no longer read or written.")
    val userFormatNotes: String? = null,
    /** A description the user typed for an album they added by hand. Null: none. */
    val userDescription: String? = null,
    /** The tracklist the user typed for an album added by hand, as a JSON list of titles (see `UserTracks`). Null: none. */
    val userTracks: String? = null
)
