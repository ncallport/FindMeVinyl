package com.nickallport.findmevinyl.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlbumEntity::class, ArtistEntity::class, ArtistImageEntity::class, AlbumDetailsEntity::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun artistImageDao(): ArtistImageDao
    abstract fun albumDetailsDao(): AlbumDetailsDao
}
