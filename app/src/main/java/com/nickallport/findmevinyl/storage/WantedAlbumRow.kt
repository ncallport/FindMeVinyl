package com.nickallport.findmevinyl.storage

data class WantedAlbumRow(
    val albumId: String,
    val artistId: String,
    val artistName: String,
    val albumTitle: String,
    val firstReleaseDate: String?,
    val artistPosition: Int,
    val wantPosition: Int,
    val wantFormat: String? = null
)