package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtistPhotoViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var photos: File
    private var now = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        photos = File(folder.root, "artist_photos")
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun viewModel(directory: File = photos) =
        ArtistPhotoViewModel(database.artistUserPhotoDao(), ArtistPhotoStore(directory), clock = { now })

    private fun jpeg(width: Int, height: Int, color: Int = Color.RED): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
    }

    private fun files() = photos.listFiles().orEmpty().map { it.name }.sorted()

    @Test
    fun `saving a photo keeps a scaled copy in the photo folder and says so`() = runBlocking {
        val saved = viewModel().savePhoto("artist-1", jpeg(2000, 1000))

        assertTrue(saved)
        assertEquals(1, files().size)
        assertEquals("artist-1-1000.jpg", files().single())
        assertEquals("artist-1-1000.jpg", database.artistUserPhotoDao().get("artist-1")?.fileName)
        assertEquals(1_000L, database.artistUserPhotoDao().get("artist-1")?.savedAt)
    }

    @Test
    fun `a big photo is kept small`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(3000, 2000))

        val bitmap = viewModel.loadPhoto("artist-1")!!

        assertEquals(600, maxOf(bitmap.width, bitmap.height))
    }

    @Test
    fun `loading gives the photo that was saved, and nothing for an artist without one`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))

        val bitmap = viewModel.loadPhoto("artist-1")!!

        assertEquals(200 to 100, bitmap.width to bitmap.height)
        assertNull(viewModel.loadPhoto("artist-2"))
    }

    @Test
    fun `a photo belongs to its artist only`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))
        viewModel.savePhoto("artist-2", jpeg(100, 200))

        assertEquals(200, viewModel.loadPhoto("artist-1")!!.width)
        assertEquals(100, viewModel.loadPhoto("artist-2")!!.width)
    }

    @Test
    fun `a new photo replaces the old one, and the old file is deleted`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))
        now = 2_000L

        viewModel.savePhoto("artist-1", jpeg(100, 200))

        assertEquals(listOf("artist-1-2000.jpg"), files())
        assertEquals("artist-1-2000.jpg", database.artistUserPhotoDao().get("artist-1")?.fileName)
        assertEquals(100, viewModel.loadPhoto("artist-1")!!.width)
    }

    @Test
    fun `bytes that are not a picture are refused, and an earlier photo is kept`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))
        now = 2_000L

        val saved = viewModel.savePhoto("artist-1", "not a picture".toByteArray())

        assertFalse(saved)
        assertEquals(listOf("artist-1-1000.jpg"), files())
        assertEquals(200, viewModel.loadPhoto("artist-1")!!.width)
    }

    @Test
    fun `a photo whose file has gone missing shows nothing`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))

        // The file goes missing, for example when the phone clears it.
        File(photos, "artist-1-1000.jpg").delete()
        assertNull(viewModel.loadPhoto("artist-1"))
    }

    @Test
    fun `a file that cannot be read as a picture shows nothing`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))
        File(photos, "artist-1-1000.jpg").writeText("damaged")

        assertNull(viewModel.loadPhoto("artist-1"))
    }

    @Test
    fun `an artist id with odd characters cannot make a file outside the photo folder`() = runBlocking {
        val viewModel = viewModel()

        val saved = viewModel.savePhoto("../../evil/name", jpeg(200, 100))

        assertTrue(saved)
        assertEquals(1, files().size)
        assertTrue(files().single().endsWith("-1000.jpg"))
        assertFalse(files().single().contains("/") || files().single().contains(".."))
        assertEquals(0, folder.root.listFiles().orEmpty().count { it.isFile })
        assertNotNull(viewModel.loadPhoto("../../evil/name"))
    }

    @Test
    fun `a failure to write the file gives false and keeps the earlier photo`() = runBlocking {
        val viewModel = viewModel()
        viewModel.savePhoto("artist-1", jpeg(200, 100))
        // A folder that cannot exist, because a file is in its place.
        val blocked = folder.newFile("blocked")
        val failing = viewModel(File(blocked, "photos"))

        val saved = failing.savePhoto("artist-2", jpeg(200, 100))

        assertFalse(saved)
        assertNull(database.artistUserPhotoDao().get("artist-2"))
        assertNotNull(viewModel.loadPhoto("artist-1"))
    }
}
