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
 * The step from version 19 to 20: the formats the user ticked, and the note for each, become copies of the album, one each,
 * in a table of their own. The old columns are left as they were.
 */
@RunWith(RobolectricTestRunner::class)
class Migration20Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(19) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `owned` INTEGER NOT NULL, `userFormats` TEXT, " +
                        "`userNote` TEXT, `userFormatNotes` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL("INSERT INTO albums VALUES ('two-formats', 1, 'LP|CD', 'BMG, 2018', '{\"LP\":\"scratched\",\"CD\":\"from Mum\"}')")
                db.execSQL("INSERT INTO albums VALUES ('one-format', 1, 'LP', NULL, NULL)")
                db.execSQL("INSERT INTO albums VALUES ('note-only', 1, NULL, 'sold in 2020', NULL)")
                db.execSQL("INSERT INTO albums VALUES ('none', 0, NULL, NULL, NULL)")
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

    private fun copies() = rows("SELECT albumId, format, note, position FROM album_copies ORDER BY albumId, position")

    @Test
    fun `each ticked format becomes a copy with its note, in the usual order`() {
        MIGRATION_19_20.migrate(db)

        assertEquals(
            listOf(
                listOf("one-format", "LP", null, "0"),
                listOf("two-formats", "LP", "scratched", "0"),
                listOf("two-formats", "CD", "from Mum", "1")
            ),
            copies()
        )
    }

    @Test
    fun `an album with no ticked formats gets no copy`() {
        MIGRATION_19_20.migrate(db)

        assertEquals(emptyList<List<String?>>(), copies().filter { it[0] in listOf("note-only", "none") })
    }

    @Test
    fun `the old columns and the note about the album are left as they were`() {
        val before = rows("SELECT id, owned, userFormats, userNote, userFormatNotes FROM albums ORDER BY id")

        MIGRATION_19_20.migrate(db)

        assertEquals(before, rows("SELECT id, owned, userFormats, userNote, userFormatNotes FROM albums ORDER BY id"))
    }
}
