package com.nickallport.findmevinyl.holidays

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NagerApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NagerApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v3/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NagerApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `asks for the public holidays of a country and a year`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        api.publicHolidays(year = 2026, countryCode = "GB")

        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/api/v3/PublicHolidays/2026/GB", request?.path)
    }

    @Test
    fun `sends a user agent, and nothing that identifies the person`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        api.publicHolidays(year = 2026, countryCode = "GB")

        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("FindMeVinyl/1.0", request.getHeader("User-Agent"))
        assertNull(request.getHeader("Cookie"))
    }

    @Test
    fun `reads each holiday's date, regions and types, ignoring other fields`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "date": "2026-01-02", "localName": "2 January", "name": "2 January", "countryCode": "GB",
                    "fixed": false, "global": false, "counties": ["GB-SCT"], "launchYear": null, "types": ["Public"] },
                  { "date": "2026-04-03", "localName": "Good Friday", "name": "Good Friday", "countryCode": "GB",
                    "fixed": false, "global": true, "counties": null, "types": ["Public"] }
                ]
                """.trimIndent()
            )
        )

        val holidays = api.publicHolidays(2026, "GB")

        assertEquals(2, holidays.size)
        assertEquals("2026-01-02", holidays[0].date)
        assertEquals(listOf("GB-SCT"), holidays[0].counties)
        assertEquals(listOf("Public"), holidays[0].types)
        assertNull(holidays[1].counties)
    }

    @Test
    fun `a holiday with missing fields still reads`() = runBlocking {
        server.enqueue(MockResponse().setBody("""[ { "date": "2026-12-25" } ]"""))

        val holiday = api.publicHolidays(2026, "GB").single()

        assertEquals("2026-12-25", holiday.date)
        assertNull(holiday.counties)
        assertNull(holiday.types)
    }

    @Test
    fun `a country it has no calendar for is a 404`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        val code = try {
            api.publicHolidays(2026, "XX")
            0
        } catch (e: HttpException) {
            e.code()
        }

        assertEquals(404, code)
    }
}
