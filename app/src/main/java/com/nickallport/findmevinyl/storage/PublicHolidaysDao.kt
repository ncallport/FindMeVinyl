package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PublicHolidaysDao {
    /** Replaces what was stored for the country and year, so holidays fetched again take the old ones' place. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(holidays: PublicHolidaysEntity): Long

    @Query("SELECT * FROM public_holidays WHERE country = :country AND year = :year")
    suspend fun get(country: String, year: Int): PublicHolidaysEntity?
}
