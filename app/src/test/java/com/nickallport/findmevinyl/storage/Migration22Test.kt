package com.nickallport.findmevinyl.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The step from version 21 to 22: the want list was in an order for each artist, the artists in the order of the
 * favourites. Every wanted album is given its place in the whole list, in the order the user saw.
 */
@RunWith(RobolectricTestRunner::class)
class Migration22Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(21) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`firstReleaseDate` TEXT, `wanted` INTEGER NOT NULL, `wantPosition` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `artist_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `artist_group_members` (`artistId` TEXT NOT NULL, `groupId` TEXT NOT NULL, PRIMARY KEY(`artistId`))")
                // Roxy Music comes first in the favourites, then Dylan, then a group.
                db.execSQL("INSERT INTO favourite_artists VALUES ('roxy', 'Roxy Music', 0)")
                db.execSQL("INSERT INTO favourite_artists VALUES ('dylan', 'Bob Dylan', 1)")
                db.execSQL("INSERT INTO favourite_artists VALUES ('cave', 'Nick Cave', 5)")
                db.execSQL("INSERT INTO artist_groups VALUES ('group/1', 'Cave all of it', 2)")
                db.execSQL("INSERT INTO artist_group_members VALUES ('cave', 'group/1')")
                // Each artist's own order: Siren (0) then Avalon (1) for Roxy, and so on.
                db.execSQL("INSERT INTO albums VALUES ('avalon', 'roxy', 'Avalon', '1982', 1, 1)")
                db.execSQL("INSERT INTO albums VALUES ('siren', 'roxy', 'Siren', '1975', 1, 0)")
                db.execSQL("INSERT INTO albums VALUES ('desire', 'dylan', 'Desire', '1976', 1, 0)")
                db.execSQL("INSERT INTO albums VALUES ('carnage', 'cave', 'Carnage', '2021', 1, 0)")
                db.execSQL("INSERT INTO albums VALUES ('stranger', 'nobody', 'Stranger', '1990', 1, 0)")
                db.execSQL("INSERT INTO albums VALUES ('owned-only', 'roxy', 'Not wanted', '1972', 0, 7)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
                .name(null).callback(callback).build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    private fun positions(): Map<String, Int> = db.query("SELECT id, wantPosition FROM albums").use { c ->
        buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
    }

    @Test
    fun `every wanted album gets its place in the whole list, in the order the user saw`() {
        MIGRATION_21_22.migrate(db)

        val order = positions().filterKeys { it != "owned-only" }.entries.sortedBy { it.value }.map { it.key }
        // Roxy Music (Siren, Avalon), Bob Dylan, the group, and an album of an artist not kept goes last.
        assertEquals(listOf("siren", "avalon", "desire", "carnage", "stranger"), order)
        assertEquals(listOf(0, 1, 2, 3, 4), positions().filterKeys { it != "owned-only" }.values.sorted())
    }

    @Test
    fun `an album that is not wanted is left as it was`() {
        MIGRATION_21_22.migrate(db)

        assertEquals(7, positions().getValue("owned-only"))
    }
}
