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
 * The step from version 20 to 21: the note about an album is gone, so an album that had one gets one more copy, on LP
 * with the note as its description, and the note is cleared.
 */
@RunWith(RobolectricTestRunner::class)
class Migration21Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(20) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `owned` INTEGER NOT NULL, `userNote` TEXT, PRIMARY KEY(`id`))")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `album_copies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`albumId` TEXT NOT NULL, `format` TEXT NOT NULL, `note` TEXT, `position` INTEGER NOT NULL)"
                )
                db.execSQL("INSERT INTO albums VALUES ('with-copy', 1, 'Green sleeve')")
                db.execSQL("INSERT INTO albums VALUES ('no-copy', 1, '  BMG vinyl, 2018 ')")
                db.execSQL("INSERT INTO albums VALUES ('blank', 1, '   ')")
                db.execSQL("INSERT INTO albums VALUES ('none', 1, NULL)")
                db.execSQL("INSERT INTO album_copies (albumId, format, note, position) VALUES ('with-copy', 'LP', 'Blue sleeve', 0)")
                db.execSQL("INSERT INTO album_copies (albumId, format, note, position) VALUES ('with-copy', 'CD', NULL, 1)")
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
    fun `an album note becomes a copy on LP after the copies it has, and the note is cleared`() {
        MIGRATION_20_21.migrate(db)

        assertEquals(
            listOf(
                listOf("no-copy", "LP", "BMG vinyl, 2018", "0"),
                listOf("with-copy", "LP", "Blue sleeve", "0"),
                listOf("with-copy", "CD", null, "1"),
                listOf("with-copy", "LP", "Green sleeve", "2")
            ),
            rows("SELECT albumId, format, note, position FROM album_copies ORDER BY albumId, position")
        )
        assertEquals(emptyList<List<String?>>(), rows("SELECT id FROM albums WHERE userNote IS NOT NULL"))
    }

    @Test
    fun `a blank note, or none, makes no copy`() {
        MIGRATION_20_21.migrate(db)

        assertEquals(
            emptyList<List<String?>>(),
            rows("SELECT albumId FROM album_copies WHERE albumId IN ('blank', 'none')")
        )
    }

    @Test
    fun `the albums are otherwise left as they were`() {
        val before = rows("SELECT id, owned FROM albums ORDER BY id")

        MIGRATION_20_21.migrate(db)

        assertEquals(before, rows("SELECT id, owned FROM albums ORDER BY id"))
    }
}
