package com.nickallport.findmevinyl.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlbumEntity::class, ArtistEntity::class, ArtistImageEntity::class, AlbumDetailsEntity::class,
        PublicHolidaysEntity::class, ArtistUserPhotoEntity::class
    ],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun artistImageDao(): ArtistImageDao
    abstract fun albumDetailsDao(): AlbumDetailsDao
    abstract fun publicHolidaysDao(): PublicHolidaysDao
    abstract fun artistUserPhotoDao(): ArtistUserPhotoDao
}
