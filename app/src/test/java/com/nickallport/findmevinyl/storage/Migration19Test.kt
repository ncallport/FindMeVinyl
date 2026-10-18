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
 * The step from version 18 to 19: albums get a note for each format the user owns. Every album keeps its flags and has
 * no format notes.
 */
@RunWith(RobolectricTestRunner::class)
class Migration19Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(18) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `owned` INTEGER NOT NULL, " +
                        "`wanted` INTEGER NOT NULL, `wantPosition` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL("INSERT INTO albums VALUES ('owned-only', 1, 0, 0)")
                db.execSQL("INSERT INTO albums VALUES ('wanted-only', 0, 1, 4)")
                db.execSQL("INSERT INTO albums VALUES ('old-both', 1, 1, 2)")
                db.execSQL("INSERT INTO albums VALUES ('neither', 0, 0, 0)")
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
    fun `every album keeps its flags and none has format notes`() {
        val before = rows("SELECT id, owned, wanted, wantPosition FROM albums ORDER BY id")

        MIGRATION_18_19.migrate(db)

        val after = rows("SELECT id, owned, wanted, wantPosition, userFormatNotes FROM albums ORDER BY id")
        assertEquals(before, after.map { it.dropLast(1) })
        assertEquals(listOf(null, null, null, null), after.map { it.last() })
    }
}
