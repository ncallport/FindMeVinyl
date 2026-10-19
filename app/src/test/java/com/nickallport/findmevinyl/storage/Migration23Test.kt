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

/** The step from version 22 to 23: an album can have a description and a tracklist the user typed. */
@RunWith(RobolectricTestRunner::class)
class Migration23Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(22) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `owned` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO albums VALUES ('custom/1', 'Live in a Pub', 1)")
                db.execSQL("INSERT INTO albums VALUES ('mb-1', 'Steeltown', 0)")
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
    fun `every album keeps what it had and has no description and no tracks`() {
        val before = rows("SELECT id, title, owned FROM albums ORDER BY id")

        MIGRATION_22_23.migrate(db)

        assertEquals(before, rows("SELECT id, title, owned FROM albums ORDER BY id"))
        assertEquals(
            listOf(listOf(null, null), listOf(null, null)),
            rows("SELECT userDescription, userTracks FROM albums ORDER BY id")
        )
    }
}
