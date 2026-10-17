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

/** Adds a table for the details pages of albums. It starts empty and nothing that was stored changes. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
        )
    }
}

/** Adds a table for the photos the user chooses for artists. It starts empty and nothing that was stored changes. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artist_user_photos` (`artistId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
        )
    }
}

/** Adds a table for the public holidays of countries. It starts empty and nothing that was stored changes. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
        )
    }
}

/**
 * Adds the user's own order: a position for each favourite artist and a want position for each album.
 * Every row gets 0, which means "no order chosen yet", and the lists keep their old order (artists by
 * name, albums by release date) until the user moves something.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favourite_artists` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `wantPosition` INTEGER NOT NULL DEFAULT 0")
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

/** Adds the release statuses of an album. Existing rows keep every value and get NULL here, meaning not looked up yet. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `statuses` TEXT")
    }
}

/** Adds a table for the user's favourite shops. It starts empty and nothing that was stored changes. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favourite_shops` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `openingHours` TEXT, " +
                "`street` TEXT, `houseNumber` TEXT, `postcode` TEXT, `city` TEXT, `addressCountry` TEXT, `phone` TEXT, " +
                "`website` TEXT, `email` TEXT, `secondHandOnly` INTEGER NOT NULL, `regionCountry` TEXT, " +
                "`regionSubdivision` TEXT, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

/**
 * Adds two columns to the favourite shops, for the shops the user adds by hand: whether the position is only that of
 * the town, and the text to search a map app for. Shops kept before are exact and have no text.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favourite_shops` ADD COLUMN `approximatePosition` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `favourite_shops` ADD COLUMN `mapQuery` TEXT")
    }
}

/** Adds the Google Maps link of a shop the user added by hand. Shops kept before have none. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favourite_shops` ADD COLUMN `mapsLink` TEXT")
    }
}

/** Adds what the user says about their own copy of an album: formats MusicBrainz does not list, and a note. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userFormats` TEXT")
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userNote` TEXT")
    }
}
