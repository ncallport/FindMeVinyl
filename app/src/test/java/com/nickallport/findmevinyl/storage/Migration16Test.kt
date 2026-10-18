package com.nickallport.findmevinyl.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The step from version 15 to 16 on a database that already holds artists and groups with genres the user
 * chose. The database is built with the version 15 columns and data, then only this step is run on it.
 */
@RunWith(RobolectricTestRunner::class)
class Migration16Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(15) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favourite_artists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT, " +
                        "`country` TEXT, `position` INTEGER NOT NULL DEFAULT 0, `genre` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artist_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL DEFAULT 0, `genre` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL("INSERT INTO favourite_artists VALUES ('minds', 'Simple Minds', 'Group', 'GB', 3, 'new wave')")
                db.execSQL("INSERT INTO favourite_artists VALUES ('genesis', 'Genesis', 'Group', 'GB', -1, NULL)")
                db.execSQL("INSERT INTO artist_groups VALUES ('group/1', 'Nick Cave, all of it', 4, 'post-punk')")
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
        buildList {
            while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) })
        }
    }

    @Test
    fun `every artist keeps all it had, the genre the user chose too, and has no suggested genre`() {
        val before = rows("SELECT id, name, type, country, position, genre FROM favourite_artists ORDER BY id")

        MIGRATION_15_16.migrate(db)

        val after = rows("SELECT id, name, type, country, position, genre, autoGenre FROM favourite_artists ORDER BY id")
        assertEquals(before, after.map { it.dropLast(1) })
        assertEquals("new wave", after.single { it[0] == "minds" }[5])
        assertNull(after[0].last())
        assertNull(after[1].last())
    }

    @Test
    fun `every group keeps all it had, the genre the user chose too, and has no suggested genre`() {
        val before = rows("SELECT id, name, position, genre FROM artist_groups")

        MIGRATION_15_16.migrate(db)

        val after = rows("SELECT id, name, position, genre, autoGenre FROM artist_groups")
        assertEquals(before, after.map { it.dropLast(1) })
        assertNull(after.single().last())
    }
}
