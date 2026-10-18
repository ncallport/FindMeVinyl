package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One copy of an album that the user owns: the format it is on and a note about it (where it came from, the colour of
 * the sleeve). An album can have several, including several on the same format. [position] is the order they were
 * listed in.
 */
@Entity(tableName = "album_copies", indices = [Index("albumId")])
data class AlbumCopyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: String,
    /** A label from `ReleaseFormats.displayOrder`, such as LP or CD. */
    val format: String,
    val note: String? = null,
    val position: Int = 0
)
