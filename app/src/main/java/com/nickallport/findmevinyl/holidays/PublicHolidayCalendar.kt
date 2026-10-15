package com.nickallport.findmevinyl.holidays

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nickallport.findmevinyl.storage.PublicHolidaysDao
import com.nickallport.findmevinyl.storage.PublicHolidaysEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate

/**
 * Where a shop is, as far as public holidays go: a country ("GB") and, when known, the part of it
 * ("GB-SCT"), because some holidays are only kept in one part. Both are ISO 3166 codes.
 */
data class HolidayRegion(val country: String, val subdivision: String?)

/**
 * The public holidays that apply in a region, from Nager.Date, found once for each country and year
 * and kept. Stored holidays are asked for again after 90 days, which is how one-off days announced
 * later get picked up. If that fails, the old ones are used.
 */
class PublicHolidayCalendar(
    private val api: NagerApi,
    private val dao: PublicHolidaysDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshAfterMillis: Long = 90L * 24 * 60 * 60 * 1000
) {
    private data class StoredHoliday(val date: String? = null, val counties: List<String>? = null)

    /** What a stored row says: the holidays, or that the service has no calendar for the country. */
    private sealed class Stored {
        data class Holidays(val holidays: List<StoredHoliday>) : Stored()
        object NoCalendar : Stored()
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<StoredHoliday?>>() {}.type

    /**
     * The dates in [years] that are public holidays in [region]. The days of the whole country are
     * included, and the days of a part of it only when it is the region's own part. Null when it cannot
     * say: the country has no calendar, or the holidays of a year are not stored and could not be asked
     * for. Years that were found stay stored even then.
     */
    suspend fun datesFor(region: HolidayRegion, years: List<Int>): Set<LocalDate>? {
        val country = region.country.trim().uppercase()
        val subdivision = region.subdivision?.trim()?.uppercase()?.ifEmpty { null }
        val dates = mutableSetOf<LocalDate>()

        for (year in years) {
            val holidays = holidaysOf(country, year) ?: return null
            holidays
                .filter { it.counties == null || (subdivision != null && subdivision in it.counties) }
                .mapNotNullTo(dates) { holiday -> holiday.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
        }
        return dates
    }

    /**
     * The holidays that matter for the next week from [today]: this year's, and next year's too when
     * the week reaches into it. Shops are only looked at a week ahead, so that is all that is needed.
     */
    suspend fun datesNear(region: HolidayRegion, today: LocalDate): Set<LocalDate>? =
        datesFor(region, (today.year..today.plusDays(LOOK_AHEAD_DAYS).year).toList())

    private suspend fun holidaysOf(country: String, year: Int): List<StoredHoliday>? {
        val stored = dao.get(country, year)?.let { it to read(it.json) }
        if (stored != null && clock() - stored.first.fetchedAt <= refreshAfterMillis) {
            when (val row = stored.second) {
                is Stored.Holidays -> return row.holidays
                Stored.NoCalendar -> return null
                null -> Unit // unreadable, so ask again
            }
        }

        return try {
            // Once the request has started, finding the holidays and saving them are not cut off.
            withContext(NonCancellable) { fetchAndSave(country, year) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Old holidays are better than none.
            (stored?.second as? Stored.Holidays)?.holidays
        }
    }

    private suspend fun fetchAndSave(country: String, year: Int): List<StoredHoliday>? {
        val fetched = try {
            api.publicHolidays(year, country)
        } catch (e: HttpException) {
            if (e.code() != 404) throw e
            dao.save(PublicHolidaysEntity(country, year, NO_CALENDAR, clock()))
            return null
        }

        val holidays = fetched
            // The service also lists school days, observances and so on. Opening hours mean public holidays.
            .filter { it.types.isNullOrEmpty() || "Public" in it.types }
            .map { StoredHoliday(it.date, it.counties?.map { code -> code.uppercase() }) }
        dao.save(PublicHolidaysEntity(country, year, gson.toJson(holidays), clock()))
        return holidays
    }

    private fun read(json: String): Stored? {
        if (json == NO_CALENDAR) return Stored.NoCalendar
        val holidays: List<StoredHoliday?> = try {
            gson.fromJson(json, listType)
        } catch (e: Exception) {
            null
        } ?: return null
        return Stored.Holidays(holidays.filterNotNull())
    }

    private companion object {
        const val NO_CALENDAR = "no calendar"

        /** How far ahead the next opening of a shop is looked for, see `OpeningHours.nextOpening`. */
        const val LOOK_AHEAD_DAYS = 7L
    }
}
