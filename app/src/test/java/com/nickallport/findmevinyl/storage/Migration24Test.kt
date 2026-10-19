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

/** The step from version 23 to 24: an album added by hand can have a cover image the user chose. */
@RunWith(RobolectricTestRunner::class)
class Migration24Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(23) {
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
    fun `every album keeps what it had and has no cover image`() {
        val before = rows("SELECT id, title, owned FROM albums ORDER BY id")

        MIGRATION_23_24.migrate(db)

        assertEquals(before, rows("SELECT id, title, owned FROM albums ORDER BY id"))
        assertEquals(listOf(listOf(null), listOf(null)), rows("SELECT userCover FROM albums ORDER BY id"))
    }
}
