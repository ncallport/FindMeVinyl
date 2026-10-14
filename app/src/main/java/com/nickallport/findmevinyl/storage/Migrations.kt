package com.nickallport.findmevinyl.storage

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the release formats of an album. Existing rows keep every value and get NULL here. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `formats` TEXT")
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `formatsCheckedAt` INTEGER")
    }
}

/** Adds a table for the pictures of artists. Nothing that was stored changes. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
        )
    }
}
