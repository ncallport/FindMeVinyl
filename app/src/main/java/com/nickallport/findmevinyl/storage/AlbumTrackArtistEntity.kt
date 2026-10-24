package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.Index

/**
 * An artist on the tracklist of a Various Artists album, kept so the album can be listed on that artist's page (the
 * "Appears On" of a streaming app). Found once for each owned or wanted Various Artists album, by `AppearsOnChecker`.
 */
@Entity(tableName = "album_track_artists", primaryKeys = ["albumId", "artistId"], indices = [Index("artistId")])
data class AlbumTrackArtistEntity(
    val albumId: String,
    val artistId: String
)
