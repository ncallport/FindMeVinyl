package com.nickallport.findmevinyl.discography

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.artist.ArtistPhotoStore
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

/** The cover image the user chooses for an album they added by hand. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlbumCoverViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var covers: File
    private var now = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        covers = File(folder.root, "album_covers")
        runBlocking {
            database.albumDao().insert(AlbumEntity("custom/1", "artist-1", "Live in a Pub", "LIVE_ALBUM", "1983", owned = true, wanted = false, formats = "", statuses = "Bootleg"))
            database.albumDao().insert(AlbumEntity("mb-1", "artist-1", "Steeltown", "STUDIO_ALBUM", "1984", owned = true, wanted = false))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun viewModel() = AlbumCoverViewModel(database.albumDao(), ArtistPhotoStore(covers), clock = { now })

    private fun jpeg(width: Int, height: Int, color: Int = Color.RED): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
    }

    private fun files() = covers.listFiles().orEmpty().map { it.name }.sorted()

    private suspend fun stored(id: String) = database.albumDao().getById(id)!!

    @Test
    fun `a cover is scaled, kept in the folder and recorded against the album`() = runBlocking {
        assertTrue(viewModel().saveCover("custom/1", jpeg(2000, 1000)))

        assertEquals(listOf("custom_1-1000.jpg"), files())
        assertEquals("custom_1-1000.jpg", stored("custom/1").userCover)
    }

    @Test
    fun `a big picture is kept small`() = runBlocking {
        viewModel().saveCover("custom/1", jpeg(3000, 2000))

        val bitmap = viewModel().loadCover("custom/1")!!
        assertTrue(maxOf(bitmap.width, bitmap.height) <= PHOTO_MAX)
    }

    @Test
    fun `the cover can be loaded again`() = runBlocking {
        viewModel().saveCover("custom/1", jpeg(200, 200))

        assertNotNull(viewModel().loadCover("custom/1"))
    }

    @Test
    fun `an album with no cover loads none`() = runBlocking {
        assertNull(viewModel().loadCover("custom/1"))
    }

    @Test
    fun `a new cover replaces the old one, and the old file is deleted`() = runBlocking {
        val viewModel = viewModel()
        viewModel.saveCover("custom/1", jpeg(200, 200))
        now = 2_000L

        assertTrue(viewModel.saveCover("custom/1", jpeg(100, 100, Color.BLUE)))

        assertEquals(listOf("custom_1-2000.jpg"), files())
        assertEquals("custom_1-2000.jpg", stored("custom/1").userCover)
    }

    @Test
    fun `something that is not a picture is refused, and an earlier cover stays`() = runBlocking {
        val viewModel = viewModel()
        viewModel.saveCover("custom/1", jpeg(200, 200))

        assertFalse(viewModel.saveCover("custom/1", "not a picture".toByteArray()))

        assertEquals("custom_1-1000.jpg", stored("custom/1").userCover)
        assertEquals(listOf("custom_1-1000.jpg"), files())
    }

    @Test
    fun `an album from MusicBrainz takes no cover, and an album that is not there neither`() = runBlocking {
        assertFalse(viewModel().saveCover("mb-1", jpeg(200, 200)))
        assertFalse(viewModel().saveCover("custom/gone", jpeg(200, 200)))

        assertNull(stored("mb-1").userCover)
        assertEquals(emptyList<String>(), files())
    }

    @Test
    fun `removing the cover clears it and deletes the file`() = runBlocking {
        val viewModel = viewModel()
        viewModel.saveCover("custom/1", jpeg(200, 200))

        assertTrue(viewModel.removeCover("custom/1"))

        assertNull(stored("custom/1").userCover)
        assertEquals(emptyList<String>(), files())
        assertNull(viewModel.loadCover("custom/1"))
    }

    @Test
    fun `removing a cover that is not there changes nothing`() = runBlocking {
        assertFalse(viewModel().removeCover("custom/1"))
        assertFalse(viewModel().removeCover("mb-1"))
    }

    @Test
    fun `a cover whose file has gone loads none`() = runBlocking {
        viewModel().saveCover("custom/1", jpeg(200, 200))
        covers.listFiles()!!.forEach { it.delete() }

        assertNull(viewModel().loadCover("custom/1"))
    }

    private companion object {
        const val PHOTO_MAX = 600
    }
}
