package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.favourites.SpecialArtists
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** The artist the user chooses for a track, over MusicBrainz's credit. Kept apart, so a refresh never overwrites it. */
@RunWith(RobolectricTestRunner::class)
class TrackCreditsTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder().baseUrl("http://localhost/").addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(), throttle = MusicBrainzThrottle(minIntervalMillis = 0),
            clock = { 1_800_000_000_000L }, groupDao = database.artistGroupDao(), pageDelayMillis = 0
        )
    }

    @After
    fun tearDown() = database.close()

    private val details = AlbumDetails(
        releaseDate = null, country = null, label = null, catalogNumber = null, firstReleaseDate = null, format = null,
        genres = emptyList(),
        discs = listOf(
            Disc(null, "CD", listOf(Track("1", "One", null, "Someone", listOf("s1")), Track("2", "Two", null))),
            Disc(null, "CD", listOf(Track("1", "Three", null)))
        )
    )

    @Test
    fun `a chosen artist replaces the credit on that track only`() {
        val shown = TrackCredits.apply(details, listOf(TrackCredit(0, 0, "One", "ub40", "UB40"), TrackCredit(1, 0, "Three", null, "A friend")))

        val tracks = shown.discs.flatMap { it.tracks }
        assertEquals(listOf("UB40", null, "A friend"), tracks.map { it.artistName })
        assertEquals(listOf("ub40"), tracks[0].artistIds)
        assertEquals(emptyList<String>(), tracks[2].artistIds)
    }

    @Test
    fun `a credit is not put on a track with another title`() {
        val shown = TrackCredits.apply(details, listOf(TrackCredit(0, 0, "Another song", "ub40", "UB40")))

        assertEquals(details, shown)
    }

    @Test
    fun `saving a credit keeps it, replaces the one on the same track, and clearing takes it away`() = runBlocking {
        viewModel.saveTrackCredit("a", TrackCredit(0, 1, "Two", "ub40", "UB40"))
        viewModel.saveTrackCredit("a", TrackCredit(0, 1, "Two", "duran", "  Duran Duran "))

        assertEquals(listOf(TrackCredit(0, 1, "Two", "duran", "Duran Duran")), viewModel.trackCredits("a"))

        viewModel.clearTrackCredit("a", 0, 1)
        assertEquals(emptyList<TrackCredit>(), viewModel.trackCredits("a"))
    }

    @Test
    fun `a blank name is not kept`() = runBlocking {
        viewModel.saveTrackCredit("a", TrackCredit(0, 1, "Two", null, "   "))

        assertEquals(emptyList<TrackCredit>(), viewModel.trackCredits("a"))
    }

    @Test
    fun `an album is on the page of an artist chosen for one of its tracks`() = runBlocking {
        database.albumDao().insert(
            AlbumEntity("now1", SpecialArtists.VARIOUS_ID, "Now", "COMPILATION", "1983", owned = true, wanted = false)
        )
        viewModel.saveTrackCredit("now1", TrackCredit(0, 0, "One", "ub40", "UB40"))

        val page = viewModel.loadSaved("ub40").groupedAlbums[DiscographyCategory.APPEARS_ON].orEmpty().map { it.id }

        assertEquals(listOf("now1"), page)
    }
}
