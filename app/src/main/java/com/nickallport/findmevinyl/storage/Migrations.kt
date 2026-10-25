package com.nickallport.findmevinyl.storage

import com.nickallport.findmevinyl.discography.ReleaseDates
import com.nickallport.findmevinyl.discography.FormatNotes
import com.nickallport.findmevinyl.discography.ReleaseFormats
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

/** Adds the pressing the user chose as their copy of an album, and a line that describes it. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `myPressingId` TEXT")
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `myPressingText` TEXT")
    }
}

/** Adds the groups of artists the user keeps together, and their members. Nothing is grouped to begin with. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artist_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artist_group_members` (`artistId` TEXT NOT NULL, `groupId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `ordinal` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`artistId`))"
        )
    }
}

/** Adds the genre the user chooses for an artist or a group, and the genres MusicBrainz lists, kept for choosing. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favourite_artists` ADD COLUMN `genre` TEXT")
        db.execSQL("ALTER TABLE `artist_groups` ADD COLUMN `genre` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artist_genres` (`artistId` TEXT NOT NULL, `genres` TEXT NOT NULL, " +
                "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
        )
    }
}

/** Adds the genre the app suggests for an artist or a group, apart from the one the user chooses. */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favourite_artists` ADD COLUMN `autoGenre` TEXT")
        db.execSQL("ALTER TABLE `artist_groups` ADD COLUMN `autoGenre` TEXT")
    }
}

/**
 * An album can now be owned and wanted at once. Until now an owned album was never wanted, and older rows with both
 * flags were left off the want list, so their want is cleared: only wants the user makes from now on show up.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE `albums` SET `wanted` = 0 WHERE `owned` = 1 AND `wanted` = 1")
    }
}

/** Adds the format the user wants an album on, such as the LP of an album owned on CD. */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `wantFormat` TEXT")
    }
}

/** Adds a note for each format of the user's copy of an album. */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userFormatNotes` TEXT")
    }
}

/**
 * An album can have any number of copies, each with a format and a note, in a table of their own. The formats the user
 * had ticked, and the note they had for each, become one copy each. The old columns are left as they were, unused.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `album_copies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`albumId` TEXT NOT NULL, `format` TEXT NOT NULL, `note` TEXT, `position` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_copies_albumId` ON `album_copies` (`albumId`)")
        val old = db.query("SELECT id, userFormats, userFormatNotes FROM albums WHERE userFormats IS NOT NULL").use { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getString(2))) }
        }
        for ((albumId, formats, notes) in old) {
            val noteOf = FormatNotes.decode(notes)
            ReleaseFormats.decodeUser(formats).forEachIndexed { position, label ->
                db.execSQL(
                    "INSERT INTO `album_copies` (`albumId`, `format`, `note`, `position`) VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(albumId, label, noteOf[label], position)
                )
            }
        }
    }
}

/**
 * The note about the album is gone: a copy has its own description. An album that had a note gets one more copy, on LP
 * with the note as its description, for the user to change if the format is wrong, and the note is cleared.
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val notes = db.query("SELECT id, userNote FROM albums WHERE userNote IS NOT NULL AND TRIM(userNote) != ''").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1).trim()) }
        }
        for ((albumId, note) in notes) {
            val next = db.query("SELECT COALESCE(MAX(position) + 1, 0) FROM album_copies WHERE albumId = ?", arrayOf<Any?>(albumId)).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
            db.execSQL(
                "INSERT INTO `album_copies` (`albumId`, `format`, `note`, `position`) VALUES (?, 'LP', ?, ?)",
                arrayOf<Any?>(albumId, note, next)
            )
        }
        db.execSQL("UPDATE `albums` SET `userNote` = NULL")
    }
}

private class WantMigrationRow(
    val id: String, val artistId: String, val artistName: String, val artistPosition: Int,
    val wantPosition: Int, val date: String?, val title: String
)

/**
 * The want list is one list in the user's own order, not a list for each artist. Until now `wantPosition` was the place
 * among the wanted albums of the same artist, and the artists came in the order of the favourites. Every wanted album
 * is given its place in the whole list, in the order the user saw: artists as ordered in the favourites (a group in its
 * own place), then each artist's albums as ordered, with the old ways of settling a tie.
 */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = db.query(
            """
            SELECT albums.id, COALESCE(artist_groups.id, albums.artistId),
                   COALESCE(artist_groups.name, favourite_artists.name, ''),
                   COALESCE(artist_groups.position, favourite_artists.position, 2147483647),
                   albums.wantPosition, albums.firstReleaseDate, albums.title
            FROM albums
            LEFT JOIN artist_group_members ON albums.artistId = artist_group_members.artistId
            LEFT JOIN artist_groups ON artist_groups.id = artist_group_members.groupId
            LEFT JOIN favourite_artists ON albums.artistId = favourite_artists.id
            WHERE albums.wanted = 1
            """.trimIndent()
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(WantMigrationRow(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getString(5), c.getString(6)))
                }
            }
        }
        rows.sortedWith(
            compareBy<WantMigrationRow> { it.artistPosition }
                .thenBy { it.artistName.lowercase() }
                .thenBy { it.artistId }
                .thenBy { it.wantPosition }
                .thenBy(nullsLast<String>()) { ReleaseDates.sortKey(it.date) }
                .thenBy { it.title }
        ).forEachIndexed { place, row ->
            db.execSQL("UPDATE `albums` SET `wantPosition` = ? WHERE `id` = ?", arrayOf<Any?>(place, row.id))
        }
    }
}

/** An album added by hand can have a description and a tracklist the user typed. */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userDescription` TEXT")
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userTracks` TEXT")
    }
}

/** An album added by hand can have a cover image the user chose. */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `userCover` TEXT")
    }
}

/**
 * A want can be pinned to the place the user gave it by moving it. Nothing is pinned to begin with, so the want list
 * starts again A to Z by artist and then release date, as the user asked.
 */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `wantPriority` INTEGER")
    }
}

/**
 * Soundtracks became a category of their own, read from MusicBrainz's secondary type, which albums saved before did not
 * keep. Every album starts unchecked and is asked about once, if it is owned or wanted.
 */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `categoryChecked` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * A Various Artists album the user owns or wants is listed on the page of each artist on its tracklist. The artists are kept
 * in a table of their own, and every album starts as not read.
 */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `albums` ADD COLUMN `trackArtistsChecked` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `album_track_artists` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, PRIMARY KEY(`albumId`, `artistId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_track_artists_artistId` ON `album_track_artists` (`artistId`)")
    }
}

/** The artist the user chose for a track of an album, kept apart from what MusicBrainz credits. */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `album_track_credits` (`albumId` TEXT NOT NULL, `discIndex` INTEGER NOT NULL, `trackIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, `artistId` TEXT, `artistName` TEXT NOT NULL, PRIMARY KEY(`albumId`, `discIndex`, `trackIndex`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_track_credits_artistId` ON `album_track_credits` (`artistId`)")
    }
}
