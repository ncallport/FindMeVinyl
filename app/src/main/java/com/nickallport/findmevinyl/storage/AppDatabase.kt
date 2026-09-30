package com.nickallport.findmevinyl.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlbumEntity::class, ArtistEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
}
