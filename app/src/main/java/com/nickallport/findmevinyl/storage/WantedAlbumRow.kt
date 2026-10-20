package com.nickallport.findmevinyl.storage

data class WantedAlbumRow(
    val albumId: String,
    val artistId: String,
    val artistName: String,
    val albumTitle: String,
    val firstReleaseDate: String?,
    val artistPosition: Int,
    val wantPosition: Int,
    val wantFormat: String? = null,
    /** The place the user pinned the want to, or null when it has not been moved. */
    val wantPriority: Int? = null
)