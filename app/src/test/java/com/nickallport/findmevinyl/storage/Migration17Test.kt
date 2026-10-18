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
 * The step from version 16 to 17. Until then an owned album was never wanted, and older rows that had both flags
 * were hidden from the want list. Now an album can be both on purpose, so those old flags must not show up as wants.
 */
@RunWith(RobolectricTestRunner::class)
class Migration17Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val callback = object : SupportSQLiteOpenHelper.Callback(16) {
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

    private fun flags(): Map<String, String> = db.query("SELECT id, owned, wanted, wantPosition FROM albums").use { c ->
        buildMap {
            while (c.moveToNext()) put(c.getString(0), "${c.getInt(1)}${c.getInt(2)}:${c.getInt(3)}")
        }
    }

    @Test
    fun `an old row with both flags is owned only, and every other album is left as it was`() {
        MIGRATION_16_17.migrate(db)

        assertEquals(
            mapOf("owned-only" to "10:0", "wanted-only" to "01:4", "old-both" to "10:2", "neither" to "00:0"),
            flags()
        )
    }
}
