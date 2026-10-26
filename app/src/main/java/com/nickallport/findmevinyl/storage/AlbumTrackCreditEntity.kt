package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.Index

/**
 * The artist the user chose for one track of an album, over what MusicBrainz credits. A track is found by its disc and
 * place on it, and [title] is kept to check it is still the same track when the pressing shown is another. A null
 * [artistId] is a name typed by hand, which MusicBrainz does not have.
 */
@Entity(tableName = "album_track_credits", primaryKeys = ["albumId", "discIndex", "trackIndex"], indices = [Index("artistId")])
data class AlbumTrackCreditEntity(
    val albumId: String,
    val discIndex: Int,
    val trackIndex: Int,
    val title: String,
    val artistId: String?,
    val artistName: String
)
