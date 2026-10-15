package com.nickallport.findmevinyl.holidays

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.PublicHolidaysEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PublicHolidayCalendarTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var api: NagerApi

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        api = Retrofit.Builder().baseUrl(server.url("/api/v3/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(NagerApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun calendar() = PublicHolidayCalendar(api, database.publicHolidaysDao(), clock = { now })

    private val england = HolidayRegion("GB", "GB-ENG")
    private val scotland = HolidayRegion("GB", "GB-SCT")

    // The UK in 2026, cut down.
    private val ukJson = """
        [
          { "date": "2026-01-01", "name": "New Year's Day", "global": false, "counties": ["GB-ENG","GB-NIR","GB-SCT","GB-WLS"], "types": ["Public"] },
          { "date": "2026-01-02", "name": "2 January", "global": false, "counties": ["GB-SCT"], "types": ["Public"] },
          { "date": "2026-04-03", "name": "Good Friday", "global": true, "counties": null, "types": ["Public"] },
          { "date": "2026-08-03", "name": "Summer Bank Holiday", "global": false, "counties": ["GB-SCT"], "types": ["Public"] },
          { "date": "2026-08-31", "name": "Summer Bank Holiday", "global": false, "counties": ["GB-ENG","GB-WLS","GB-NIR"], "types": ["Public"] },
          { "date": "2026-12-25", "name": "Christmas Day", "global": true, "counties": null, "types": ["Public"] }
        ]
    """.trimIndent()

    private fun datesOf(region: HolidayRegion, vararg years: Int) =
        runBlocking { calendar().datesFor(region, years.toList()) }

    @Test
    fun `gives the national holidays and the ones for the region`() {
        server.enqueue(MockResponse().setBody(ukJson))

        val england = datesOf(england, 2026)!!

        assertEquals(
            setOf(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 8, 31), LocalDate.of(2026, 12, 25)
            ),
            england
        )
    }

    @Test
    fun `a region gets its own days and not another region's`() {
        server.enqueue(MockResponse().setBody(ukJson))

        val scotland = datesOf(scotland, 2026)!!

        assertEquals(
            setOf(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 25)
            ),
            scotland
        )
    }

    @Test
    fun `without a region only the national holidays are certain`() {
        server.enqueue(MockResponse().setBody(ukJson))

        val unknownRegion = datesOf(HolidayRegion("GB", null), 2026)!!

        assertEquals(setOf(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 12, 25)), unknownRegion)
    }

    @Test
    fun `leaves out days that are not public holidays`() {
        server.enqueue(
            MockResponse().setBody(
                """
                [ { "date": "2026-05-05", "counties": null, "types": ["School"] },
                  { "date": "2026-05-06", "counties": null, "types": ["Observance", "Optional"] },
                  { "date": "2026-05-07", "counties": null, "types": ["Public", "Bank"] },
                  { "date": "2026-05-08", "counties": null, "types": null } ]
                """.trimIndent()
            )
        )

        // No types at all is taken as a public holiday, since that is what the service lists.
        assertEquals(setOf(LocalDate.of(2026, 5, 7), LocalDate.of(2026, 5, 8)), datesOf(england, 2026))
    }

    @Test
    fun `reads the country code as upper case, however it is given`() {
        server.enqueue(MockResponse().setBody("[]"))

        datesOf(HolidayRegion("gb", "gb-eng"), 2026)

        assertEquals("/api/v3/PublicHolidays/2026/GB", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun `a subdivision in lower case still matches`() {
        server.enqueue(MockResponse().setBody(ukJson))

        assertEquals(true, LocalDate.of(2026, 1, 2) in datesOf(HolidayRegion("gb", "gb-sct"), 2026)!!)
    }

    @Test
    fun `asks for each year it needs, and keeps what it found`() {
        server.enqueue(MockResponse().setBody(ukJson))
        server.enqueue(MockResponse().setBody("""[ { "date": "2027-01-01", "counties": null, "types": ["Public"] } ]"""))

        val dates = datesOf(england, 2026, 2027)!!

        assertEquals("/api/v3/PublicHolidays/2026/GB", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        assertEquals("/api/v3/PublicHolidays/2027/GB", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        assertEquals(true, LocalDate.of(2027, 1, 1) in dates)
        assertNotNull(runBlocking { database.publicHolidaysDao().get("GB", 2026) })
        assertNotNull(runBlocking { database.publicHolidaysDao().get("GB", 2027) })
    }

    @Test
    fun `uses what is stored without asking again`() {
        server.enqueue(MockResponse().setBody(ukJson))
        datesOf(england, 2026)

        val again = datesOf(scotland, 2026)!!

        assertEquals(1, server.requestCount)
        assertEquals(true, LocalDate.of(2026, 1, 2) in again)
    }

    @Test
    fun `the holidays of different regions of one country share one request`() {
        server.enqueue(MockResponse().setBody(ukJson))

        datesOf(england, 2026)
        datesOf(scotland, 2026)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `asks again once what is stored is 90 days old, and replaces it`() = runBlocking {
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2026, """[{"date":"2026-02-02","counties":null}]""", now - 91 * day))
        server.enqueue(MockResponse().setBody(ukJson))

        val dates = calendar().datesFor(england, listOf(2026))!!

        assertEquals(1, server.requestCount)
        assertEquals(true, LocalDate.of(2026, 4, 3) in dates)
        assertEquals(false, LocalDate.of(2026, 2, 2) in dates)
        assertEquals(now, database.publicHolidaysDao().get("GB", 2026)?.fetchedAt)
    }

    @Test
    fun `does not ask again before 90 days`() = runBlocking {
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2026, """[{"date":"2026-02-02","counties":null}]""", now - 89 * day))

        val dates = calendar().datesFor(england, listOf(2026))!!

        assertEquals(0, server.requestCount)
        assertEquals(setOf(LocalDate.of(2026, 2, 2)), dates)
    }

    @Test
    fun `uses old stored holidays when it cannot ask for new ones`() = runBlocking {
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2026, """[{"date":"2026-02-02","counties":null}]""", now - 200 * day))
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(setOf(LocalDate.of(2026, 2, 2)), calendar().datesFor(england, listOf(2026)))
    }

    @Test
    fun `gives nothing when it cannot ask and has nothing stored, and stores nothing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        assertNull(calendar().datesFor(england, listOf(2026)))
        assertNull(database.publicHolidaysDao().get("GB", 2026))
    }

    @Test
    fun `gives nothing when it has the first year but not the second`() = runBlocking {
        server.enqueue(MockResponse().setBody(ukJson))
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(calendar().datesFor(england, listOf(2026, 2027)))
        // The year it did get is kept for next time.
        assertNotNull(database.publicHolidaysDao().get("GB", 2026))
    }

    @Test
    fun `a country with no calendar gives nothing, and is not asked about again for 90 days`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        val calendar = calendar()

        assertNull(calendar.datesFor(HolidayRegion("XX", null), listOf(2026)))
        assertNull(calendar.datesFor(HolidayRegion("XX", null), listOf(2026)))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a stored row that cannot be read is asked for again`() = runBlocking {
        database.publicHolidaysDao().save(PublicHolidaysEntity("GB", 2026, "not json", now - day))
        server.enqueue(MockResponse().setBody(ukJson))

        assertEquals(true, LocalDate.of(2026, 12, 25) in calendar().datesFor(england, listOf(2026))!!)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a day in the response that is not a date is skipped`() {
        server.enqueue(
            MockResponse().setBody(
                """[ { "date": "not a date", "counties": null, "types": ["Public"] }, { "date": "2026-12-25", "counties": null, "types": ["Public"] } ]"""
            )
        )

        assertEquals(setOf(LocalDate.of(2026, 12, 25)), datesOf(england, 2026))
    }

    @Test
    fun `no years asked for gives an empty set with no request`() {
        assertEquals(emptySet<LocalDate>(), datesOf(england))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a lookup that has started is still saved when the caller is cancelled`() = runBlocking {
        server.enqueue(MockResponse().setBody(ukJson).setHeadersDelay(300, TimeUnit.MILLISECONDS))

        val job = launch(Dispatchers.IO) { calendar().datesFor(england, listOf(2026)) }
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        job.cancelAndJoin()

        assertNotNull(database.publicHolidaysDao().get("GB", 2026))
    }

    // Which years to ask for, when only the next week matters.

    @Test
    fun `in the middle of a year only that year is asked for`() = runBlocking {
        server.enqueue(MockResponse().setBody(ukJson))

        calendar().datesNear(england, LocalDate.of(2026, 10, 15))

        assertEquals(1, server.requestCount)
        assertEquals("/api/v3/PublicHolidays/2026/GB", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun `a week before the end of the year is still only that year`() = runBlocking {
        server.enqueue(MockResponse().setBody(ukJson))

        calendar().datesNear(england, LocalDate.of(2026, 12, 24))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `when the next week reaches into the new year, that year is asked for too`() = runBlocking {
        server.enqueue(MockResponse().setBody(ukJson))
        server.enqueue(MockResponse().setBody("""[ { "date": "2027-01-01", "counties": null, "types": ["Public"] } ]"""))

        val dates = calendar().datesNear(england, LocalDate.of(2026, 12, 25))!!

        assertEquals(2, server.requestCount)
        assertEquals(true, LocalDate.of(2027, 1, 1) in dates)
        assertEquals(true, LocalDate.of(2026, 12, 25) in dates)
    }
}
