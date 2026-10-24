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
 * The step from version 26 to 27: the artists on each Various Artists album's tracklist are kept in a table of their own,
 * and every album starts as not read. Albums keep what they had.
 */
@RunWith(RobolectricTestRunner::class)
class Migration27Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(26) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `wanted` INTEGER NOT NULL, `wantPosition` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO albums VALUES ('a', 1, 0)")
                db.execSQL("INSERT INTO albums VALUES ('b', 1, 1)")
                db.execSQL("INSERT INTO albums VALUES ('c', 0, 5)")
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

    private fun rows(sql: String): List<List<String?>> = db.query(sql).use { c ->
        buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) }
    }

    @Test
    fun `every album keeps what it had, none is read, and the table is there and empty`() {
        val before = rows("SELECT id, wanted, wantPosition FROM albums ORDER BY id")

        MIGRATION_26_27.migrate(db)

        assertEquals(before, rows("SELECT id, wanted, wantPosition FROM albums ORDER BY id"))
        assertEquals(listOf(listOf("0"), listOf("0"), listOf("0")), rows("SELECT trackArtistsChecked FROM albums ORDER BY id"))
        assertEquals(emptyList<List<String?>>(), rows("SELECT albumId, artistId FROM album_track_artists"))
        db.execSQL("INSERT INTO album_track_artists VALUES ('a', 'phil')")
        assertEquals(listOf(listOf("a", "phil")), rows("SELECT albumId, artistId FROM album_track_artists"))
    }
}
