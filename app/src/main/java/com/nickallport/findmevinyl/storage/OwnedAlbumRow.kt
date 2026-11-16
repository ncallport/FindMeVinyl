package com.nickallport.findmevinyl.storage

/** An album the user owns, with the name of its artist, for the Owned list. */
data class OwnedAlbumRow(
    val albumId: String,
    /** The artist the album is filed under, which is what its page is opened with. */
    val artistId: String,
    val artistName: String,
    val albumTitle: String,
    val firstReleaseDate: String?
)
