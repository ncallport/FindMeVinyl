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

    private fun openMigrated(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    fun `an upgraded database keeps the favourite artists`() = runBlocking {
        createVersion1Database()
        val database = openMigrated()

        assertEquals("Genesis", database.artistDao().getById("artist-1")?.name)
        assertNull(database.artistDao().getById("missing"))
        database.close()
    }
}
