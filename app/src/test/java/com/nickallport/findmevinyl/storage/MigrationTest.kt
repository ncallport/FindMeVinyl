package com.nickallport.findmevinyl.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a version 1 database the way the released app had it (the statements come from
 * schemas/.../1.json), puts a user's data in it, and opens it with the current Room database.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion1Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, PRIMARY KEY(`id`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0)"
            )
            db.execSQL(
                "INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1)"
            )
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB')")
        }
        helper.close()
    }

    // Version 2, from schemas/.../2.json: albums have their formats columns, and there is no artist_images table.
    private fun createVersion2Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 2) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, PRIMARY KEY(`id`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, " +
                    "'12\" Vinyl|CD', 1700000000000)"
            )
            db.execSQL(
                "INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL)"
            )
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB')")
        }
        helper.close()
    }

    // Version 3, from schemas/.../3.json: albums with formats, favourites, and the artist_images table.
    private fun createVersion3Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 3) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000)"
            )
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB')")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
        }
        helper.close()
    }

    // Version 4, from schemas/.../4.json: everything of version 3 plus the album_details table.
    private fun createVersion4Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 4) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL(
                "INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000)"
            )
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB')")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-2', 'Ramones', NULL, NULL)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
        }
        helper.close()
    }

    // Version 5, from schemas/.../5.json: everything of version 4 plus the priority columns.
    private fun createVersion5Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 5) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3)")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
        }
        helper.close()
    }

    // Version 6, from schemas/.../6.json: everything of version 5 plus the public_holidays table.
    private fun createVersion6Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 6) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3)")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
            db.execSQL("INSERT INTO public_holidays VALUES ('GB', 2026, '[]', 9)")
        }
        helper.close()
    }

    // Version 7, from schemas/.../7.json: everything of version 6 plus the artist_user_photos table.
    private fun createVersion7Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 7) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_user_photos` (`artistId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                        "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3)")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
            db.execSQL("INSERT INTO public_holidays VALUES ('GB', 2026, '[]', 9)")
            db.execSQL("INSERT INTO artist_user_photos VALUES ('artist-1', 'artist-1-7.jpg', 7)")
        }
        helper.close()
    }

    // Version 8, from schemas/.../8.json: everything of version 7 plus the statuses column of albums.
    private fun createVersion8Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 8) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, `statuses` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_user_photos` (`artistId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                        "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3, 'Official')")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1, NULL)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
            db.execSQL("INSERT INTO public_holidays VALUES ('GB', 2026, '[]', 9)")
            db.execSQL("INSERT INTO artist_user_photos VALUES ('artist-1', 'artist-1-7.jpg', 7)")
        }
        helper.close()
    }

    // Version 9, from schemas/.../9.json: everything of version 8 plus the favourite_shops table.
    private fun createVersion9Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 9) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, `statuses` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_shops` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `openingHours` TEXT, " +
                        "`street` TEXT, `houseNumber` TEXT, `postcode` TEXT, `city` TEXT, `addressCountry` TEXT, `phone` TEXT, " +
                        "`website` TEXT, `email` TEXT, `secondHandOnly` INTEGER NOT NULL, `regionCountry` TEXT, " +
                        "`regionSubdivision` TEXT, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_user_photos` (`artistId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                        "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3, 'Official')")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1, NULL)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
            db.execSQL("INSERT INTO public_holidays VALUES ('GB', 2026, '[]', 9)")
            db.execSQL("INSERT INTO artist_user_photos VALUES ('artist-1', 'artist-1-7.jpg', 7)")
            db.execSQL(
                "INSERT INTO favourite_shops VALUES ('node/1', 'Louie Louie', 'RECORD_SHOP', 38.71, -9.14, 'Mo-Sa 11:00-19:30', " +
                    "'Escadinhas', '3', '1100-225', 'Lisboa', 'PT', '+351 213 472 232', 'https://www.louielouie.biz/', NULL, 1, 'PT', NULL, 5)"
            )
        }
        helper.close()
    }

    // Version 10, from schemas/.../10.json: everything of version 9 plus two columns of favourite_shops.
    private fun createVersion10Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 10) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `firstReleaseDate` TEXT, " +
                        "`owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, `formats` TEXT, " +
                        "`formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, `statuses` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`type` TEXT, `country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_images` (`artistId` TEXT NOT NULL, `imageUrl` TEXT, " +
                        "`checkedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_details` (`albumId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`albumId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `public_holidays` (`country` TEXT NOT NULL, `year` INTEGER NOT NULL, " +
                        "`json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`country`, `year`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_shops` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `openingHours` TEXT, " +
                        "`street` TEXT, `houseNumber` TEXT, `postcode` TEXT, `city` TEXT, `addressCountry` TEXT, `phone` TEXT, " +
                        "`website` TEXT, `email` TEXT, `secondHandOnly` INTEGER NOT NULL, `regionCountry` TEXT, " +
                        "`regionSubdivision` TEXT, `savedAt` INTEGER NOT NULL, `approximatePosition` INTEGER NOT NULL DEFAULT 0, `mapQuery` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_user_photos` (`artistId` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                        "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`artistId`))"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.use { db ->
            db.execSQL("INSERT INTO albums VALUES ('1', 'artist-1', 'Foxtrot', 'STUDIO_ALBUM', '1972-10-06', 1, 0, 'CD', 1700000000000, 3, 'Official')")
            db.execSQL("INSERT INTO albums VALUES ('2', 'artist-1', 'Trespass', 'STUDIO_ALBUM', NULL, 0, 1, NULL, NULL, 1, NULL)")
            db.execSQL("INSERT INTO favourite_artists VALUES ('artist-1', 'Genesis', 'Group', 'GB', 2)")
            db.execSQL("INSERT INTO artist_images VALUES ('artist-1', 'https://commons.test/a.jpg', 99)")
            db.execSQL("INSERT INTO album_details VALUES ('1', '{}', 5)")
            db.execSQL("INSERT INTO public_holidays VALUES ('GB', 2026, '[]', 9)")
            db.execSQL("INSERT INTO artist_user_photos VALUES ('artist-1', 'artist-1-7.jpg', 7)")
            db.execSQL(
                "INSERT INTO favourite_shops VALUES ('node/1', 'Louie Louie', 'RECORD_SHOP', 38.71, -9.14, 'Mo-Sa 11:00-19:30', " +
                    "'Escadinhas', '3', '1100-225', 'Lisboa', 'PT', '+351 213 472 232', 'https://www.louielouie.biz/', NULL, 1, 'PT', NULL, 5, 1, 'Louie Louie, Lisboa')"
            )
        }
        helper.close()
    }

    private fun openMigrated(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `an upgraded database keeps every album with its owned and wanted flags`() = runBlocking {
        createVersion1Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertEquals(false, albums[0].wanted)
        assertEquals(false, albums[1].owned)
        assertTrue(albums[1].wanted)
        assertEquals("1972-10-06", albums[0].firstReleaseDate)
        database.close()
    }

    @Test
    fun `an upgraded database has no formats stored yet`() = runBlocking {
        createVersion1Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1")

        assertTrue(albums.all { it.formats == null && it.formatsCheckedAt == null })
        database.close()
    }

    @Test
    fun `a version 2 database keeps its albums, flags and stored formats when it gets the picture table`() = runBlocking {
        createVersion2Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals("12\" Vinyl|CD", albums[0].formats)
        assertEquals(1_700_000_000_000, albums[0].formatsCheckedAt)
        assertNull(albums[1].formats)
        assertEquals("Genesis", database.artistDao().getById("artist-1")?.name)
        database.close()
    }

    @Test
    fun `a version 2 database can store artist pictures after the upgrade`() = runBlocking {
        createVersion2Database()
        val database = openMigrated()

        assertNull(database.artistImageDao().get("artist-1"))
        database.artistImageDao().save(ArtistImageEntity("artist-1", "https://commons.test/a.jpg", 7))

        assertEquals("https://commons.test/a.jpg", database.artistImageDao().get("artist-1")?.imageUrl)
        database.close()
    }

    @Test
    fun `a version 3 database keeps everything when it gets the album details table`() = runBlocking {
        createVersion3Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals("CD", albums[0].formats)
        assertEquals("Genesis", database.artistDao().getById("artist-1")?.name)
        assertEquals("https://commons.test/a.jpg", database.artistImageDao().get("artist-1")?.imageUrl)
        database.close()
    }

    @Test
    fun `a version 3 database can store album details after the upgrade`() = runBlocking {
        createVersion3Database()
        val database = openMigrated()

        assertNull(database.albumDetailsDao().get("1"))
        database.albumDetailsDao().save(AlbumDetailsEntity("1", "{}", 7))

        assertEquals("{}", database.albumDetailsDao().get("1")?.json)
        database.close()
    }

    @Test
    fun `an upgraded database keeps the favourite artists`() = runBlocking {
        createVersion1Database()
        val database = openMigrated()

        assertEquals("Genesis", database.artistDao().getById("artist-1")?.name)
        assertNull(database.artistDao().getById("missing"))
        database.close()
    }

    @Test
    fun `a version 4 database keeps everything when it gets the priority columns`() = runBlocking {
        createVersion4Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals("CD", albums[0].formats)
        assertEquals(listOf("Genesis", "Ramones"), database.artistDao().getAllFavourites().map { it.name }.sorted())
        assertEquals("https://commons.test/a.jpg", database.artistImageDao().get("artist-1")?.imageUrl)
        assertEquals("{}", database.albumDetailsDao().get("1")?.json)
        database.close()
    }

    @Test
    fun `a version 4 database starts with every priority at zero, so the old order still applies`() = runBlocking {
        createVersion4Database()
        val database = openMigrated()

        assertTrue(database.artistDao().getAllFavourites().all { it.position == 0 })
        assertTrue(database.albumDao().getAlbumsForArtist("artist-1").all { it.wantPosition == 0 })
        database.close()
    }

    @Test
    fun `a version 4 database can store priorities after the upgrade`() = runBlocking {
        createVersion4Database()
        val database = openMigrated()

        database.artistDao().setPosition("artist-2", 3)
        database.albumDao().setWantPosition("2", 5)

        assertEquals(3, database.artistDao().getById("artist-2")?.position)
        assertEquals(5, database.albumDao().getById("2")?.wantPosition)
        database.close()
    }

    @Test
    fun `a version 5 database keeps everything when it gets the public holidays table`() = runBlocking {
        createVersion5Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals(listOf(3, 1), albums.map { it.wantPosition })
        assertEquals(2, database.artistDao().getById("artist-1")?.position)
        assertEquals("https://commons.test/a.jpg", database.artistImageDao().get("artist-1")?.imageUrl)
        assertEquals("{}", database.albumDetailsDao().get("1")?.json)
        database.close()
    }

    @Test
    fun `a version 5 database can store public holidays after the upgrade`() = runBlocking {
        createVersion5Database()
        val database = openMigrated()

        assertNull(database.publicHolidaysDao().get("GB", 2026))
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2026, "[]", 7))
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2027, "[1]", 8))

        assertEquals("[]", database.publicHolidaysDao().get("GB", 2026)?.json)
        assertEquals("[1]", database.publicHolidaysDao().get("GB", 2027)?.json)
        assertNull(database.publicHolidaysDao().get("DE", 2026))
        database.close()
    }

    @Test
    fun `a version 6 database keeps everything when it gets the artist photos table`() = runBlocking {
        createVersion6Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals(listOf(3, 1), albums.map { it.wantPosition })
        assertEquals(2, database.artistDao().getById("artist-1")?.position)
        assertEquals("https://commons.test/a.jpg", database.artistImageDao().get("artist-1")?.imageUrl)
        assertEquals("{}", database.albumDetailsDao().get("1")?.json)
        assertEquals("[]", database.publicHolidaysDao().get("GB", 2026)?.json)
        database.close()
    }

    @Test
    fun `a version 6 database can store a photo of an artist after the upgrade`() = runBlocking {
        createVersion6Database()
        val database = openMigrated()

        assertNull(database.artistUserPhotoDao().get("artist-1"))
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "artist-1-7.jpg", 7))

        assertEquals("artist-1-7.jpg", database.artistUserPhotoDao().get("artist-1")?.fileName)
        assertNull(database.artistUserPhotoDao().get("artist-2"))
        database.close()
    }

    @Test
    fun `a version 7 database keeps everything when albums get their release statuses`() = runBlocking {
        createVersion7Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals("CD", albums[0].formats)
        assertEquals(listOf(3, 1), albums.map { it.wantPosition })
        assertEquals(2, database.artistDao().getById("artist-1")?.position)
        assertEquals("artist-1-7.jpg", database.artistUserPhotoDao().get("artist-1")?.fileName)
        database.close()
    }

    @Test
    fun `a version 7 database starts with no statuses stored, and can store them after the upgrade`() = runBlocking {
        createVersion7Database()
        val database = openMigrated()

        assertNull(database.albumDao().getById("1")?.statuses)
        database.albumDao().setChecked("1", "CD", "Bootleg|Official", checkedAt = 9)

        val album = database.albumDao().getById("1")
        assertEquals("Bootleg|Official", album?.statuses)
        assertEquals(9L, album?.formatsCheckedAt)
        database.close()
    }

    @Test
    fun `a version 8 database keeps everything when it gets the favourite shops table`() = runBlocking {
        createVersion8Database()
        val database = openMigrated()

        val albums = database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }

        assertEquals(listOf("Foxtrot", "Trespass"), albums.map { it.title })
        assertTrue(albums[0].owned)
        assertTrue(albums[1].wanted)
        assertEquals("Official", albums[0].statuses)
        assertEquals(2, database.artistDao().getById("artist-1")?.position)
        assertEquals("artist-1-7.jpg", database.artistUserPhotoDao().get("artist-1")?.fileName)
        database.close()
    }

    @Test
    fun `a version 8 database starts with no favourite shops, and can keep one after the upgrade`() = runBlocking {
        createVersion8Database()
        val database = openMigrated()

        assertEquals(emptyList<FavouriteShopEntity>(), database.favouriteShopDao().getAll())
        database.favouriteShopDao().save(
            FavouriteShopEntity(
                "node/1", "Louie Louie", "RECORD_SHOP", 38.71, -9.14, null, null, null, null, "Lisboa", "PT",
                null, null, null, false, "PT", null, 5
            )
        )

        assertEquals("Louie Louie", database.favouriteShopDao().get("node/1")?.name)
        database.close()
    }

    @Test
    fun `a version 9 database keeps its favourite shops when it gets the new columns`() = runBlocking {
        createVersion9Database()
        val database = openMigrated()

        val shop = database.favouriteShopDao().get("node/1")!!

        assertEquals("Louie Louie", shop.name)
        assertEquals("+351 213 472 232", shop.phone)
        assertTrue(shop.secondHandOnly)
        assertEquals(5L, shop.savedAt)
        assertEquals(listOf("Foxtrot", "Trespass"), database.albumDao().getAlbumsForArtist("artist-1").sortedBy { it.id }.map { it.title })
        database.close()
    }

    @Test
    fun `shops that were kept before are exact and have no map search text`() = runBlocking {
        createVersion9Database()
        val database = openMigrated()

        val shop = database.favouriteShopDao().get("node/1")!!

        assertEquals(false, shop.approximatePosition)
        assertNull(shop.mapQuery)
        database.close()
    }

    @Test
    fun `a version 9 database can keep a shop added by hand after the upgrade`() = runBlocking {
        createVersion9Database()
        val database = openMigrated()

        database.favouriteShopDao().save(
            FavouriteShopEntity(
                "custom/abc", "Silver Tentacle", "RECORD_SHOP", 39.4, -9.1, null, "R. Raul Proença 56a", null, null, null, null,
                null, null, null, false, null, null, 9, approximatePosition = true, mapQuery = "Silver Tentacle, Caldas da Rainha"
            )
        )

        assertEquals("Silver Tentacle, Caldas da Rainha", database.favouriteShopDao().get("custom/abc")?.mapQuery)
        database.close()
    }

    @Test
    fun `a version 10 database keeps its shops when they get the Maps link column`() = runBlocking {
        createVersion10Database()
        val database = openMigrated()

        val shop = database.favouriteShopDao().get("node/1")!!

        assertEquals("Louie Louie", shop.name)
        assertEquals(true, shop.approximatePosition)
        assertEquals("Louie Louie, Lisboa", shop.mapQuery)
        assertNull(shop.mapsLink)
        database.close()
    }

    @Test
    fun `a version 10 database can keep a Maps link after the upgrade`() = runBlocking {
        createVersion10Database()
        val database = openMigrated()

        database.favouriteShopDao().save(database.favouriteShopDao().get("node/1")!!.copy(mapsLink = "https://maps.app.goo.gl/x"))

        assertEquals("https://maps.app.goo.gl/x", database.favouriteShopDao().get("node/1")?.mapsLink)
        database.close()
    }

    @Test
    fun `a version 10 database keeps its albums when they get the copy columns`() = runBlocking {
        createVersion10Database()
        val database = openMigrated()

        val foxtrot = database.albumDao().getById("1")!!

        assertTrue(foxtrot.owned)
        assertEquals("CD", foxtrot.formats)
        assertEquals("Official", foxtrot.statuses)
        assertNull(foxtrot.userFormats)
        assertNull(foxtrot.userNote)
        database.close()
    }

    @Test
    fun `an upgraded album can keep details of the user's copy`() = runBlocking {
        createVersion10Database()
        val database = openMigrated()

        database.albumDao().setCopy("1", "LP", "Portuguese pressing")

        val foxtrot = database.albumDao().getById("1")!!
        assertEquals("LP", foxtrot.userFormats)
        assertEquals("Portuguese pressing", foxtrot.userNote)
        assertEquals("CD", foxtrot.formats)
        database.close()
    }
}
