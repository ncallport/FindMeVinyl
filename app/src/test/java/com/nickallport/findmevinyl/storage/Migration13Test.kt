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
 * The step from version 12 to 13 on a database that already holds what the user recorded about their copies.
 * The database is built with the version 12 columns and data, then only this step is run on it.
 */
@RunWith(RobolectricTestRunner::class)
class Migration13Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(12) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `artistId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `firstReleaseDate` TEXT, `owned` INTEGER NOT NULL, `wanted` INTEGER NOT NULL, " +
                        "`formats` TEXT, `formatsCheckedAt` INTEGER, `wantPosition` INTEGER NOT NULL DEFAULT 0, `statuses` TEXT, " +
                        "`userFormats` TEXT, `userNote` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL("INSERT INTO albums VALUES ('1', 'a', 'Olympia', 'STUDIO_ALBUM', '2010-10-20', 1, 0, 'CD', 5, 2, 'Official', 'LP|CD', 'BMG vinyl')")
                db.execSQL("INSERT INTO albums VALUES ('2', 'a', 'Boys and Girls', 'STUDIO_ALBUM', '1985', 0, 1, NULL, NULL, 7, NULL, NULL, NULL)")
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

    private fun row(id: String): List<Any?> = db.query("SELECT * FROM albums WHERE id = '$id'").use { c ->
        c.moveToFirst()
        (0 until c.columnCount).map { i ->
            when (c.getType(i)) {
                android.database.Cursor.FIELD_TYPE_NULL -> null
                android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                else -> c.getString(i)
            }
        }
    }

    @Test
    fun `every value an album had is still there after the step`() {
        val before = row("1")

        MIGRATION_12_13.migrate(db)

        assertEquals(before, row("1").take(before.size))
        assertEquals("LP|CD", row("1")[11])
        assertEquals("BMG vinyl", row("1")[12])
    }

    @Test
    fun `an album with nothing recorded is unchanged and has no pressing`() {
        val before = row("2")

        MIGRATION_12_13.migrate(db)

        assertEquals(before, row("2").take(before.size))
        assertNull(row("2")[13])
        assertNull(row("2")[14])
    }

    @Test
    fun `both new columns are there, empty`() {
        MIGRATION_12_13.migrate(db)

        assertEquals(listOf(null, null), row("1").takeLast(2))
    }

    @Test
    fun `no album is lost`() {
        MIGRATION_12_13.migrate(db)

        db.query("SELECT COUNT(*) FROM albums").use { c ->
            c.moveToFirst()
            assertEquals(2, c.getInt(0))
        }
    }
}
