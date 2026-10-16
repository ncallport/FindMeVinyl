package com.nickallport.findmevinyl.backup

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.artist.ArtistPhotoStore
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistUserPhotoEntity
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Backing up the user's own data to a file, and restoring from one without losing what is on the phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var photoDir: File
    private var settings = Settings()
    private var now = 5_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        photoDir = folder.newFolder("photos")
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun viewModel(
        db: AppDatabase = database,
        dir: File = photoDir,
        currentSettings: () -> Settings = { settings },
        applySettings: (Settings) -> Unit = { settings = it }
    ) = BackupViewModel(db, ArtistPhotoStore(dir), currentSettings, applySettings, clock = { now })

    private fun album(
        id: String,
        artist: String = "artist-1",
        owned: Boolean = false,
        wanted: Boolean = false,
        wantPosition: Int = 0
    ) = AlbumEntity(id, artist, "Album $id", "STUDIO_ALBUM", "1972-10-06", owned, wanted, wantPosition = wantPosition)

    private fun jpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.RED) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    private fun backup(vm: BackupViewModel = viewModel()): ByteArray =
        ByteArrayOutputStream().also { runBlocking { vm.createBackup(it) } }.toByteArray()

    private fun restore(bytes: ByteArray, vm: BackupViewModel = viewModel()): RestoreResult =
        runBlocking { vm.restore(ByteArrayInputStream(bytes)) }

    private fun entries(zip: ByteArray): Map<String, ByteArray> {
        val found = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                found[entry.name] = input.readBytes()
            }
        }
        return found
    }

    private fun zipOf(vararg files: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun json(text: String) = "backup.json" to text.toByteArray()

    private fun emptyDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    // Making a backup.

    @Test
    fun `a backup is a zip with one json file`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", "Group", "GB", position = 2))

        val files = entries(backup())

        assertTrue("backup.json" in files)
        assertTrue(files.keys.all { it == "backup.json" || it.startsWith("photos/") })
    }

    @Test
    fun `the backup says what made it and which version of the format it is`() {
        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"app\":\"FindMeVinyl\""))
        assertTrue(text.contains("\"version\":1"))
    }

    @Test
    fun `reports how much it saved`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", null, null))
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().insert(album("2", wanted = true))
        database.albumDao().insert(album("3"))

        val summary = viewModel().createBackup(ByteArrayOutputStream())

        assertEquals(1, summary.artists)
        assertEquals(2, summary.albums)
        assertEquals(0, summary.photos)
    }

    @Test
    fun `an album that is neither owned nor wanted is left out, because the discography brings it back`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().insert(album("2"))

        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"id\":\"1\""))
        assertFalse(text.contains("\"id\":\"2\""))
    }

    @Test
    fun `a photo the user chose is saved in the zip`() = runBlocking {
        File(photoDir, "artist-1-1.jpg").writeBytes(jpeg())
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "artist-1-1.jpg", 1))

        val files = entries(backup())

        assertTrue(files.getValue("photos/artist-1-1.jpg").contentEquals(File(photoDir, "artist-1-1.jpg").readBytes()))
    }

    @Test
    fun `a photo whose file is missing is left out without failing the backup`() = runBlocking {
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "gone.jpg", 1))

        val summary = viewModel().createBackup(ByteArrayOutputStream())

        assertEquals(0, summary.photos)
    }

    // Restoring into an empty phone: what was saved comes back as it was.

    @Test
    fun `restoring into an empty phone brings back the artists with their order`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", "Group", "GB", position = 2))
        database.artistDao().insert(ArtistEntity("artist-2", "Roxy Music", "Group", "GB", position = 0))
        val saved = backup()
        val fresh = emptyDatabase()

        val result = restore(saved, viewModel(fresh))

        assertTrue(result is RestoreResult.Done)
        assertEquals(2, fresh.artistDao().getById("artist-1")?.position)
        assertEquals(0, fresh.artistDao().getById("artist-2")?.position)
        assertEquals("Genesis", fresh.artistDao().getById("artist-1")?.name)
        assertEquals("GB", fresh.artistDao().getById("artist-1")?.country)
        fresh.close()
    }

    @Test
    fun `restoring into an empty phone brings back owned and wanted albums with the want order`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().insert(album("2", wanted = true, wantPosition = 3))
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        val owned = fresh.albumDao().getById("1")!!
        val wanted = fresh.albumDao().getById("2")!!
        assertTrue(owned.owned)
        assertFalse(owned.wanted)
        assertTrue(wanted.wanted)
        assertEquals(3, wanted.wantPosition)
        assertEquals("Album 2", wanted.title)
        assertEquals("1972-10-06", wanted.firstReleaseDate)
        assertEquals("artist-1", wanted.artistId)
        fresh.close()
    }

    @Test
    fun `restoring brings back the photo as the artist's photo`() = runBlocking {
        File(photoDir, "artist-1-1.jpg").writeBytes(jpeg())
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "artist-1-1.jpg", 1))
        val saved = backup()
        val fresh = emptyDatabase()
        val freshDir = folder.newFolder("fresh-photos")

        val result = restore(saved, viewModel(fresh, freshDir)) as RestoreResult.Done

        assertEquals(1, result.photos)
        val record = fresh.artistUserPhotoDao().get("artist-1")!!
        assertTrue(File(freshDir, record.fileName).readBytes().contentEquals(jpeg()))
        fresh.close()
    }

    @Test
    fun `restoring brings back the settings`() {
        settings = Settings(hideCds = true, hideDigital = false, hideMcs = true)
        val saved = backup()
        settings = Settings()

        val result = restore(saved) as RestoreResult.Done

        assertTrue(result.settingsRestored)
        assertEquals(Settings(hideCds = true, hideDigital = false, hideMcs = true), settings)
    }

    @Test
    fun `reports what it added`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", null, null))
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().insert(album("2", wanted = true))
        val saved = backup()
        val fresh = emptyDatabase()

        val result = restore(saved, viewModel(fresh)) as RestoreResult.Done

        assertEquals(1, result.artists)
        assertEquals(2, result.albums)
        fresh.close()
    }

    // Restoring into a phone that already has data: nothing is lost.

    @Test
    fun `an owned flag on the phone is never cleared by a backup that does not have it`() = runBlocking {
        database.albumDao().insert(album("1", owned = false))
        val saved = backup()
        database.albumDao().insert(album("2", owned = true))
        database.albumDao().setOwned("1", true)

        restore(saved)

        assertTrue(database.albumDao().getById("1")!!.owned)
        assertTrue(database.albumDao().getById("2")!!.owned)
    }

    @Test
    fun `an album on the phone that the backup does not mention is kept as it is`() = runBlocking {
        val saved = backup()
        database.albumDao().insert(album("9", wanted = true, wantPosition = 4))

        restore(saved)

        val kept = database.albumDao().getById("9")!!
        assertTrue(kept.wanted)
        assertEquals(4, kept.wantPosition)
    }

    @Test
    fun `an album owned in the backup becomes owned on the phone and is no longer wanted`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        val saved = backup()
        database.albumDao().setOwned("1", false)
        database.albumDao().setWanted("1", true)

        restore(saved)

        val merged = database.albumDao().getById("1")!!
        assertTrue(merged.owned)
        assertFalse(merged.wanted)
    }

    @Test
    fun `an album wanted in the backup is wanted on the phone, at the saved place`() = runBlocking {
        database.albumDao().insert(album("1", wanted = true, wantPosition = 5))
        val saved = backup()
        database.albumDao().setWanted("1", false)
        database.albumDao().setWantPosition("1", 0)

        restore(saved)

        val merged = database.albumDao().getById("1")!!
        assertTrue(merged.wanted)
        assertEquals(5, merged.wantPosition)
    }

    @Test
    fun `an album already wanted on the phone keeps its place`() = runBlocking {
        database.albumDao().insert(album("1", wanted = true, wantPosition = 5))
        val saved = backup()
        database.albumDao().setWantPosition("1", 1)

        restore(saved)

        assertEquals(1, database.albumDao().getById("1")!!.wantPosition)
    }

    @Test
    fun `an album owned on the phone is not made wanted by a backup that wanted it`() = runBlocking {
        database.albumDao().insert(album("1", wanted = true))
        val saved = backup()
        database.albumDao().setWanted("1", false)
        database.albumDao().setOwned("1", true)

        restore(saved)

        val merged = database.albumDao().getById("1")!!
        assertTrue(merged.owned)
        assertFalse(merged.wanted)
    }

    @Test
    fun `an artist already on the phone keeps its place and name`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", "Group", "GB", position = 2))
        val saved = backup()
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis (renamed)", "Group", "GB", position = 7))

        restore(saved)

        val kept = database.artistDao().getById("artist-1")!!
        assertEquals(7, kept.position)
        assertEquals("Genesis (renamed)", kept.name)
    }

    @Test
    fun `an artist who has a photo on the phone keeps it`() = runBlocking {
        File(photoDir, "artist-1-1.jpg").writeBytes(jpeg())
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "artist-1-1.jpg", 1))
        val saved = backup()
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("artist-1", "mine.jpg", 9))

        val result = restore(saved) as RestoreResult.Done

        assertEquals("mine.jpg", database.artistUserPhotoDao().get("artist-1")!!.fileName)
        assertEquals(0, result.photos)
    }

    @Test
    fun `restoring the same backup twice changes nothing the second time`() = runBlocking {
        database.artistDao().insert(ArtistEntity("artist-1", "Genesis", null, null, position = 1))
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().insert(album("2", wanted = true, wantPosition = 2))
        val saved = backup()
        val fresh = emptyDatabase()
        val vm = viewModel(fresh, folder.newFolder("again"))
        restore(saved, vm)

        val second = restore(saved, vm) as RestoreResult.Done

        assertEquals(0, second.artists)
        assertEquals(0, second.albums)
        assertEquals(1, fresh.artistDao().getAllFavourites().size)
        fresh.close()
    }

    // Files that are not a backup, or are not understood: the phone is left alone.

    @Test
    fun `a file that is not a zip is refused`() {
        database.let { runBlocking { it.albumDao().insert(album("1", owned = true)) } }

        val result = restore("this is not a backup".toByteArray())

        assertTrue(result is RestoreResult.Failed)
        assertTrue(runBlocking { database.albumDao().getById("1")!!.owned })
    }

    @Test
    fun `a zip with no backup file is refused`() {
        assertTrue(restore(zipOf("other.txt" to "hi".toByteArray())) is RestoreResult.Failed)
    }

    @Test
    fun `a backup file that is not json is refused`() {
        assertTrue(restore(zipOf(json("not json at all"))) is RestoreResult.Failed)
    }

    @Test
    fun `a json file from another app is refused`() {
        assertTrue(restore(zipOf(json("""{"app":"Other","version":1}"""))) is RestoreResult.Failed)
    }

    @Test
    fun `a backup from a newer version of the app is refused, with a reason that says so`() {
        val result = restore(zipOf(json("""{"app":"FindMeVinyl","version":99}""")))

        assertTrue(result is RestoreResult.Failed)
        assertTrue((result as RestoreResult.Failed).reason.contains("newer"))
    }

    @Test
    fun `a refused file changes nothing, not even the settings`() {
        settings = Settings(hideCds = true)

        restore(zipOf(json("""{"app":"Other","version":1,"settings":{"hideCds":false}}""")))

        assertEquals(Settings(hideCds = true), settings)
    }

    @Test
    fun `a backup with only some parts restores those parts`() = runBlocking {
        val result = restore(
            zipOf(json("""{"app":"FindMeVinyl","version":1,"favourites":[{"id":"a","name":"Genesis","position":1}]}"""))
        ) as RestoreResult.Done

        assertEquals(1, result.artists)
        assertEquals(0, result.albums)
        assertFalse(result.settingsRestored)
        assertEquals("Genesis", database.artistDao().getById("a")?.name)
    }

    @Test
    fun `an entry without an id is skipped, not stored`() = runBlocking {
        val result = restore(
            zipOf(json("""{"app":"FindMeVinyl","version":1,"favourites":[{"name":"No id"}],"albums":[{"title":"No id"}]}"""))
        ) as RestoreResult.Done

        assertEquals(0, result.artists)
        assertEquals(0, result.albums)
        assertEquals(0, database.artistDao().getAllFavourites().size)
    }

    @Test
    fun `a photo that is not a picture is skipped, and the rest is restored`() = runBlocking {
        val result = restore(
            zipOf(
                json(
                    """{"app":"FindMeVinyl","version":1,
                    "favourites":[{"id":"a","name":"Genesis","position":0}],
                    "photos":[{"artistId":"a","fileName":"a-1.jpg"}]}"""
                ),
                "photos/a-1.jpg" to "not a picture".toByteArray()
            )
        ) as RestoreResult.Done

        assertEquals(0, result.photos)
        assertNull(database.artistUserPhotoDao().get("a"))
        assertNotNull(database.artistDao().getById("a"))
    }

    @Test
    fun `a photo name that tries to leave the photo folder is never used as a path`() = runBlocking {
        val result = restore(
            zipOf(
                json(
                    """{"app":"FindMeVinyl","version":1,
                    "photos":[{"artistId":"../evil","fileName":"../../evil.jpg"}]}"""
                ),
                "photos/../../evil.jpg" to jpeg()
            )
        ) as RestoreResult.Done

        val written = photoDir.listFiles().orEmpty().map { it.name }
        assertTrue(written.all { !it.contains("..") && !it.contains("/") })
        assertFalse(File(photoDir.parentFile.parentFile, "evil.jpg").exists())
        assertTrue(result.photos <= 1)
    }
}
