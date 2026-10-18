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

    // Favourite shops.

    private val louie = com.nickallport.findmevinyl.storage.FavouriteShopEntity(
        id = "node/1", name = "Louie Louie", category = "RECORD_SHOP", latitude = 38.71, longitude = -9.14,
        openingHours = "Mo-Sa 11:00-19:30", street = "Escadinhas", houseNumber = "3", postcode = "1100-225",
        city = "Lisboa", addressCountry = "PT", phone = "+351 213 472 232", website = "https://www.louielouie.biz/",
        email = "a@b.pt", secondHandOnly = true, regionCountry = "PT", regionSubdivision = null, savedAt = 5
    )

    @Test
    fun `favourite shops are saved in the backup and counted`() = runBlocking {
        database.favouriteShopDao().save(louie)

        val summary = viewModel().createBackup(ByteArrayOutputStream())

        assertEquals(1, summary.shops)
        assertTrue(String(entries(backup()).getValue("backup.json")).contains("Louie Louie"))
    }

    @Test
    fun `restoring into an empty phone brings back the favourite shops, every part of them`() = runBlocking {
        database.favouriteShopDao().save(louie)
        val saved = backup()
        val fresh = emptyDatabase()

        val result = restore(saved, viewModel(fresh)) as RestoreResult.Done

        assertEquals(1, result.shops)
        assertEquals(louie, fresh.favouriteShopDao().get("node/1"))
        fresh.close()
    }

    @Test
    fun `a favourite shop already on the phone is kept as it is`() = runBlocking {
        database.favouriteShopDao().save(louie)
        val saved = backup()
        database.favouriteShopDao().save(louie.copy(name = "Louie Louie (mine)", phone = "+351 111"))

        val result = restore(saved) as RestoreResult.Done

        assertEquals(0, result.shops)
        assertEquals("Louie Louie (mine)", database.favouriteShopDao().get("node/1")?.name)
    }

    @Test
    fun `a favourite shop with no id, or a position that is not one, is skipped`() = runBlocking {
        val result = restore(
            zipOf(
                json(
                    """{"app":"FindMeVinyl","version":1,"shops":[
                    {"name":"No id","latitude":1.0,"longitude":2.0},
                    {"id":"node/2","name":"No position"},
                    {"id":"node/3","name":"Out of range","latitude":123.0,"longitude":2.0},
                    {"id":"node/4","name":"Good","category":"RECORD_SHOP","latitude":38.7,"longitude":-9.1}]}"""
                )
            )
        ) as RestoreResult.Done

        assertEquals(1, result.shops)
        assertEquals(listOf("node/4"), database.favouriteShopDao().getIds())
    }

    @Test
    fun `a favourite shop of a kind the app does not know becomes a record shop`() = runBlocking {
        restore(
            zipOf(json("""{"app":"FindMeVinyl","version":1,"shops":[{"id":"node/5","name":"X","category":"MYSTERY","latitude":1.0,"longitude":2.0}]}"""))
        )

        assertEquals("RECORD_SHOP", database.favouriteShopDao().get("node/5")?.category)
    }

    @Test
    fun `a shop added by hand is restored with its approximate position and map search text`() = runBlocking {
        database.favouriteShopDao().save(
            louie.copy(id = "custom/abc", approximatePosition = true, mapQuery = "Silver Tentacle, Caldas da Rainha")
        )
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        val kept = fresh.favouriteShopDao().get("custom/abc")!!
        assertTrue(kept.approximatePosition)
        assertEquals("Silver Tentacle, Caldas da Rainha", kept.mapQuery)
        fresh.close()
    }

    @Test
    fun `a shop's Maps link is restored with it`() = runBlocking {
        database.favouriteShopDao().save(louie.copy(id = "custom/abc", mapsLink = "https://maps.app.goo.gl/Q4Xy2CQNv29QYFyCA"))
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        assertEquals("https://maps.app.goo.gl/Q4Xy2CQNv29QYFyCA", fresh.favouriteShopDao().get("custom/abc")?.mapsLink)
        fresh.close()
    }

    // Details of the user's own copy of an album.

    @Test
    fun `the backup carries the formats and note of a copy`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setCopy("1", "LP", "BMG vinyl")

        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"userFormats\":\"LP\""))
        assertTrue(text.contains("\"userNote\":\"BMG vinyl\""))
    }

    @Test
    fun `restoring into an empty phone brings the copy details back`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setCopy("1", "LP|CD", "BMG vinyl")
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        val restored = fresh.albumDao().getById("1")!!
        assertEquals("LP|CD", restored.userFormats)
        assertEquals("BMG vinyl", restored.userNote)
        fresh.close()
    }

    @Test
    fun `an album that was sold but has details of a copy is kept by the backup`() = runBlocking {
        database.albumDao().insert(album("1"))
        database.albumDao().setCopy("1", null, "Sold in 2020")
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        val restored = fresh.albumDao().getById("1")!!
        assertEquals("Sold in 2020", restored.userNote)
        assertFalse(restored.owned)
        fresh.close()
    }

    @Test
    fun `copy details already on the phone are not replaced by the ones in the backup`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setCopy("1", "LP", "from the backup")
        val saved = backup()
        database.albumDao().setCopy("1", "CD", "written on the phone")

        restore(saved)

        val kept = database.albumDao().getById("1")!!
        assertEquals("CD", kept.userFormats)
        assertEquals("written on the phone", kept.userNote)
    }

    @Test
    fun `an album on the phone with no copy details takes them from the backup`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setCopy("1", "LP", "from the backup")
        val saved = backup()
        database.albumDao().setCopy("1", null, null)

        val result = restore(saved) as RestoreResult.Done

        assertEquals("LP", database.albumDao().getById("1")!!.userFormats)
        assertEquals(1, result.albums)
    }

    @Test
    fun `formats that are not formats are dropped and a long note is cut when restoring`() = runBlocking {
        val note = "x".repeat(2000)
        val text = """{"app":"FindMeVinyl","version":1,"albums":[{"id":"1","artistId":"artist-1","title":"A","category":"STUDIO_ALBUM","owned":true,"userFormats":"LP|nonsense","userNote":"$note"}]}"""
        val fresh = emptyDatabase()

        restore(zipOf(json(text)), viewModel(fresh))

        val restored = fresh.albumDao().getById("1")!!
        assertEquals("LP", restored.userFormats)
        assertEquals(500, restored.userNote!!.length)
        fresh.close()
    }

    @Test
    fun `restoring the same file again changes nothing about the copy`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setCopy("1", "LP", "BMG vinyl")
        val saved = backup()

        val second = restore(saved) as RestoreResult.Done

        assertEquals(0, second.albums)
        assertEquals("LP", database.albumDao().getById("1")!!.userFormats)
    }

    // The pressing the user chose as theirs.

    @Test
    fun `the backup carries the chosen pressing`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setMyPressing("1", "release-9", "1989, Portugal")

        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"myPressingId\":\"release-9\""))
        assertTrue(text.contains("\"myPressingText\":\"1989, Portugal\""))
    }

    @Test
    fun `restoring into an empty phone brings the chosen pressing back`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setMyPressing("1", "release-9", "1989, Portugal")
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        val restored = fresh.albumDao().getById("1")!!
        assertEquals("release-9", restored.myPressingId)
        assertEquals("1989, Portugal", restored.myPressingText)
        fresh.close()
    }

    @Test
    fun `an album that was sold but has a chosen pressing is kept by the backup`() = runBlocking {
        database.albumDao().insert(album("1"))
        database.albumDao().setMyPressing("1", "release-9", "1989, Portugal")
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        assertEquals("release-9", fresh.albumDao().getById("1")!!.myPressingId)
        assertFalse(fresh.albumDao().getById("1")!!.owned)
        fresh.close()
    }

    @Test
    fun `a pressing already chosen on the phone is not replaced by the one in the backup`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setMyPressing("1", "from-backup", "from the backup")
        val saved = backup()
        database.albumDao().setMyPressing("1", "on-phone", "on the phone")

        restore(saved)

        assertEquals("on-phone", database.albumDao().getById("1")!!.myPressingId)
        assertEquals("on the phone", database.albumDao().getById("1")!!.myPressingText)
    }

    @Test
    fun `an album on the phone with no pressing chosen takes it from the backup, whatever copy details it has`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setMyPressing("1", "release-9", "1989, Portugal")
        val saved = backup()
        database.albumDao().setMyPressing("1", null, null)
        database.albumDao().setCopy("1", "LP", "a note written on the phone")

        val result = restore(saved) as RestoreResult.Done

        val merged = database.albumDao().getById("1")!!
        assertEquals("release-9", merged.myPressingId)
        assertEquals("a note written on the phone", merged.userNote)
        assertEquals(1, result.albums)
    }

    @Test
    fun `restoring the same file again changes nothing about the pressing`() = runBlocking {
        database.albumDao().insert(album("1", owned = true))
        database.albumDao().setMyPressing("1", "release-9", "1989, Portugal")
        val saved = backup()

        val second = restore(saved) as RestoreResult.Done

        assertEquals(0, second.albums)
        assertEquals("release-9", database.albumDao().getById("1")!!.myPressingId)
    }

    @Test
    fun `a pressing with no id in the file is not kept, and a long line is cut`() = runBlocking {
        val line = "x".repeat(2000)
        val text = """{"app":"FindMeVinyl","version":1,"albums":[
            {"id":"1","artistId":"a","title":"A","category":"STUDIO_ALBUM","owned":true,"myPressingText":"orphan"},
            {"id":"2","artistId":"a","title":"B","category":"STUDIO_ALBUM","owned":true,"myPressingId":"r","myPressingText":"$line"}]}"""
        val fresh = emptyDatabase()

        restore(zipOf(json(text)), viewModel(fresh))

        assertNull(fresh.albumDao().getById("1")!!.myPressingText)
        assertEquals(500, fresh.albumDao().getById("2")!!.myPressingText!!.length)
        fresh.close()
    }

    // Groups of artists.

    private suspend fun group(db: AppDatabase = database, id: String = "group/1", name: String = "Nick Cave, all of it", position: Int = 3, vararg members: Pair<String, String>) {
        db.artistGroupDao().insertGroup(com.nickallport.findmevinyl.storage.ArtistGroupEntity(id, name, position))
        db.artistGroupDao().insertMembers(members.mapIndexed { i, (artistId, memberName) ->
            com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity(artistId, id, memberName, i)
        })
    }

    private val caveAndSeeds = arrayOf("cave" to "Nick Cave", "seeds" to "Nick Cave & The Bad Seeds")

    @Test
    fun `the backup carries each group with its name, place and members in order`() = runBlocking {
        group(members = caveAndSeeds)

        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"name\":\"Nick Cave, all of it\""))
        assertTrue(text.indexOf("\"artistId\":\"cave\"") < text.indexOf("\"artistId\":\"seeds\""))
    }

    @Test
    fun `the summary of a backup counts the groups`() = runBlocking {
        group(members = caveAndSeeds)

        val summary = runBlocking { viewModel().createBackup(ByteArrayOutputStream()) }

        assertEquals(1, summary.groups)
    }

    @Test
    fun `restoring into an empty phone brings a group back as it was`() = runBlocking {
        group(members = caveAndSeeds)
        val saved = backup()
        val fresh = emptyDatabase()

        val result = restore(saved, viewModel(fresh)) as RestoreResult.Done

        val restored = fresh.artistGroupDao().getGroup("group/1")!!
        assertEquals("Nick Cave, all of it", restored.name)
        assertEquals(3, restored.position)
        assertEquals(
            listOf("cave" to "Nick Cave", "seeds" to "Nick Cave & The Bad Seeds"),
            fresh.artistGroupDao().getMembers("group/1").map { it.artistId to it.name }
        )
        assertEquals(1, result.groups)
        fresh.close()
    }

    @Test
    fun `a group already on the phone is kept as it is`() = runBlocking {
        group(members = caveAndSeeds)
        val saved = backup()
        database.artistGroupDao().setName("group/1", "Renamed on the phone")

        val result = restore(saved) as RestoreResult.Done

        assertEquals("Renamed on the phone", database.artistGroupDao().getGroup("group/1")!!.name)
        assertEquals(0, result.groups)
    }

    @Test
    fun `an artist already in another group on the phone is left out of the restored group`() = runBlocking {
        group(members = arrayOf("cave" to "Nick Cave", "seeds" to "Seeds", "grinderman" to "Grinderman"))
        val saved = backup()
        val fresh = emptyDatabase()
        group(fresh, "group/other", "Other", 0, "seeds" to "Seeds", "x" to "X")

        restore(saved, viewModel(fresh))

        assertEquals(listOf("cave", "grinderman"), fresh.artistGroupDao().getMembers("group/1").map { it.artistId })
        assertEquals("group/other", fresh.artistGroupDao().groupIdOf("seeds"))
        fresh.close()
    }

    @Test
    fun `a group left with fewer than two artists is not restored`() = runBlocking {
        group(members = caveAndSeeds)
        val saved = backup()
        val fresh = emptyDatabase()
        group(fresh, "group/other", "Other", 0, "seeds" to "Seeds", "x" to "X")

        val result = restore(saved, viewModel(fresh)) as RestoreResult.Done

        assertNull(fresh.artistGroupDao().getGroup("group/1"))
        assertEquals(0, result.groups)
        fresh.close()
    }

    @Test
    fun `restoring the same file again adds no group`() = runBlocking {
        group(members = caveAndSeeds)
        val saved = backup()
        val fresh = emptyDatabase()
        restore(saved, viewModel(fresh))

        val again = restore(saved, viewModel(fresh)) as RestoreResult.Done

        assertEquals(0, again.groups)
        assertEquals(2, fresh.artistGroupDao().getMembers("group/1").size)
        fresh.close()
    }

    @Test
    fun `entries with no id, a blank name or a blank member are made sense of or skipped`() = runBlocking {
        val text = """{"app":"FindMeVinyl","version":1,"groups":[
            {"name":"No id","members":[{"artistId":"a","name":"A"},{"artistId":"b","name":"B"}]},
            {"id":"group/1","name":"   ","members":[{"artistId":"a","name":"First"},{"artistId":"","name":"Blank"},{"artistId":"b","name":"B"}]},
            {"id":"group/2","name":"Not a group","members":[{"artistId":"c","name":"C"}]},
            {"id":"not-a-group-id","name":"Wrong id","members":[{"artistId":"d","name":"D"},{"artistId":"e","name":"E"}]}]}"""
        val fresh = emptyDatabase()

        val result = restore(zipOf(json(text)), viewModel(fresh)) as RestoreResult.Done

        assertEquals(1, result.groups)
        assertEquals("First", fresh.artistGroupDao().getGroup("group/1")!!.name)
        assertEquals(listOf("a", "b"), fresh.artistGroupDao().getMembers("group/1").map { it.artistId })
        assertEquals(1, fresh.artistGroupDao().getGroups().size)
        fresh.close()
    }

    @Test
    fun `the photo chosen for a group comes back with the group`() = runBlocking {
        group(members = caveAndSeeds)
        File(photoDir, "group-1.jpg").writeBytes(jpeg())
        database.artistUserPhotoDao().save(ArtistUserPhotoEntity("group/1", "group-1.jpg", 1))
        val saved = backup()
        val fresh = emptyDatabase()
        val freshDir = folder.newFolder("fresh-group-photos")

        restore(saved, viewModel(fresh, freshDir))

        val record = fresh.artistUserPhotoDao().get("group/1")!!
        assertTrue(File(freshDir, record.fileName).exists())
        assertFalse(record.fileName.contains("/"))
        fresh.close()
    }

    @Test
    fun `a photo for a group that is not on the phone and was not restored is left out`() = runBlocking {
        val photo = jpeg()
        val text = """{"app":"FindMeVinyl","version":1,"photos":[{"artistId":"group/ghost","fileName":"g.jpg"}]}"""
        val fresh = emptyDatabase()
        val freshDir = folder.newFolder("ghost-photos")

        restore(zipOf(json(text), "photos/g.jpg" to photo), viewModel(fresh, freshDir))

        assertNull(fresh.artistUserPhotoDao().get("group/ghost"))
        fresh.close()
    }

    // The genre the user chose.

    @Test
    fun `the backup carries the genre of an artist and of a group`() = runBlocking {
        database.artistDao().insert(ArtistEntity("genesis", "Genesis", null, null, 0, "progressive rock"))
        group(members = caveAndSeeds)
        database.artistGroupDao().setGenre("group/1", "post-punk")

        val text = String(entries(backup()).getValue("backup.json"))

        assertTrue(text.contains("\"genre\":\"progressive rock\""))
        assertTrue(text.contains("\"genre\":\"post-punk\""))
    }

    @Test
    fun `restoring into an empty phone brings the genres back`() = runBlocking {
        database.artistDao().insert(ArtistEntity("genesis", "Genesis", null, null, 0, "progressive rock"))
        group(members = caveAndSeeds)
        database.artistGroupDao().setGenre("group/1", "post-punk")
        val saved = backup()
        val fresh = emptyDatabase()

        restore(saved, viewModel(fresh))

        assertEquals("progressive rock", fresh.artistDao().getById("genesis")!!.genre)
        assertEquals("post-punk", fresh.artistGroupDao().getGroup("group/1")!!.genre)
        fresh.close()
    }

    @Test
    fun `an artist on the phone with no genre takes the one in the backup, and one with a genre keeps its own`() = runBlocking {
        database.artistDao().insert(ArtistEntity("a", "A", null, null, 0, "rock"))
        database.artistDao().insert(ArtistEntity("b", "B", null, null, 1, "punk"))
        val saved = backup()
        database.artistDao().setGenre("a", null)
        database.artistDao().setGenre("b", "jazz")

        restore(saved)

        assertEquals("rock", database.artistDao().getById("a")!!.genre)
        assertEquals("jazz", database.artistDao().getById("b")!!.genre)
    }

    @Test
    fun `a group on the phone with no genre takes the one in the backup, and one with a genre keeps its own`() = runBlocking {
        group(id = "group/1", members = caveAndSeeds)
        database.artistGroupDao().setGenre("group/1", "post-punk")
        group(id = "group/2", name = "Other", position = 4, members = arrayOf("x" to "X", "y" to "Y"))
        database.artistGroupDao().setGenre("group/2", "punk")
        val saved = backup()
        database.artistGroupDao().setGenre("group/1", null)
        database.artistGroupDao().setGenre("group/2", "jazz")

        restore(saved)

        assertEquals("post-punk", database.artistGroupDao().getGroup("group/1")!!.genre)
        assertEquals("jazz", database.artistGroupDao().getGroup("group/2")!!.genre)
    }

    @Test
    fun `a genre in the file is tidied the way a chosen one is, and a blank one is no genre`() = runBlocking {
        val text = """{"app":"FindMeVinyl","version":1,"favourites":[
            {"id":"a","name":"A","genre":"  Post   Punk "},{"id":"b","name":"B","genre":"   "}],
            "groups":[{"id":"group/1","name":"G","genre":"Gothic Rock","members":[{"artistId":"x","name":"X"},{"artistId":"y","name":"Y"}]}]}"""
        val fresh = emptyDatabase()

        restore(zipOf(json(text)), viewModel(fresh))

        assertEquals("post punk", fresh.artistDao().getById("a")!!.genre)
        assertNull(fresh.artistDao().getById("b")!!.genre)
        assertEquals("gothic rock", fresh.artistGroupDao().getGroup("group/1")!!.genre)
        fresh.close()
    }

    @Test
    fun `a backup made before genres existed restores with no genres`() = runBlocking {
        val text = """{"app":"FindMeVinyl","version":1,"favourites":[{"id":"a","name":"A"}]}"""
        val fresh = emptyDatabase()

        restore(zipOf(json(text)), viewModel(fresh))

        assertNull(fresh.artistDao().getById("a")!!.genre)
        fresh.close()
    }
}
