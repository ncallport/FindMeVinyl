package com.nickallport.findmevinyl.storage

import androidx.room.Entity

/**
 * The public holidays of one country in one year, found once and kept: the dates and regions as JSON
 * (see `PublicHolidayCalendar`), and when they were fetched, in epoch milliseconds.
 */
@Entity(tableName = "public_holidays", primaryKeys = ["country", "year"])
data class PublicHolidaysEntity(
    val country: String,
    val year: Int,
    val json: String,
    val fetchedAt: Long
)
