package com.nickallport.findmevinyl.backup

import com.nickallport.findmevinyl.discography.FormatNotes
import com.nickallport.findmevinyl.genre.GenreNames
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.favourites.ArtistGroupViewModel
import com.nickallport.findmevinyl.discography.ReleaseFormats
import com.nickallport.findmevinyl.discography.MAX_NOTE_LENGTH
import android.graphics.BitmapFactory
import androidx.room.withTransaction
import com.google.gson.Gson
import com.nickallport.findmevinyl.artist.ArtistPhotoStore
import com.nickallport.findmevinyl.artist.LimitedReader
import com.nickallport.findmevinyl.artist.safeFileName
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.shops.SharedPlaceParser
import com.nickallport.findmevinyl.shops.ShopCategory
import com.nickallport.findmevinyl.storage.ArtistUserPhotoEntity
import com.nickallport.findmevinyl.storage.FavouriteShopEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_JSON = "backup.json"
private const val PHOTO_PREFIX = "photos/"
private const val MAX_JSON_BYTES = 20 * 1024 * 1024
private const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
private const val MAX_ENTRIES = 5_000
private const val MAX_GROUP_NAME = 200

/**
 * Backs up what the user chose and cannot get back from MusicBrainz, and restores it.
 *
 * A backup is a zip: `backup.json` (favourite artists with their order, the albums owned or wanted with
 * the want order, which photo belongs to which artist, and the settings) and the photos. Albums that are
 * neither owned nor wanted are left out, since the discography loads them again.
 *
 * Restoring only adds. An owned flag is never cleared, an artist or a photo already on the phone is kept
 * as it is, and an album already wanted keeps its place. The whole file is read and checked before
 * anything is changed, and the database changes are one transaction, so a file that is wrong, or one
 * that fails half way, leaves the phone as it was.
 */
class BackupViewModel(
    private val database: AppDatabase,
    private val photoStore: ArtistPhotoStore,
    private val currentSettings: () -> Settings,
    private val applySettings: (Settings) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val gson = Gson()

    /** Writes the backup zip to [out], which the caller closes. Throws when [out] cannot be written. */
    suspend fun createBackup(out: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val artists = database.artistDao().getAllFavourites().sortedWith(compareBy({ it.position }, { it.name }))
        val albums = database.albumDao().getOwnedOrWanted().sortedBy { it.id }
        val photos = database.artistUserPhotoDao().getAll().sortedBy { it.artistId }
            .mapNotNull { record -> photoStore.read(record.fileName)?.let { record to it } }

        val shops = database.favouriteShopDao().getAll().sortedBy { it.id }
        val groups = database.artistGroupDao().getGroups().sortedWith(compareBy({ it.position }, { it.name }))
        val groupMembers = database.artistGroupDao().getAllMembers().groupBy { it.groupId }

        val file = BackupFile(
            app = BackupFile.APP,
            version = BackupFile.VERSION,
            exportedAt = clock(),
            favourites = artists.map { BackupArtist(it.id, it.name, it.type, it.country, it.position, it.genre) },
            albums = albums.map {
                BackupAlbum(
                    it.id, it.artistId, it.title, it.category, it.firstReleaseDate, it.owned, it.wanted, it.wantPosition,
                    it.userFormats, it.userNote, it.myPressingId, it.myPressingText, it.wantFormat, it.userFormatNotes
                )
            },
            photos = photos.map { (record, _) -> BackupPhoto(record.artistId, record.fileName) },
            shops = shops.map {
                BackupShop(
                    it.id, it.name, it.category, it.latitude, it.longitude, it.openingHours, it.street, it.houseNumber,
                    it.postcode, it.city, it.addressCountry, it.phone, it.website, it.email, it.secondHandOnly,
                    it.regionCountry, it.regionSubdivision, it.savedAt, it.approximatePosition, it.mapQuery, it.mapsLink
                )
            },
            groups = groups.map { group ->
                BackupGroup(
                    group.id, group.name, group.position,
                    groupMembers[group.id].orEmpty().sortedBy { it.ordinal }.map { BackupGroupMember(it.artistId, it.name) },
                    group.genre
                )
            },
            settings = currentSettings()
        )

        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry(BACKUP_JSON))
        zip.write(gson.toJson(file).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        for ((record, bytes) in photos) {
            zip.putNextEntry(ZipEntry(PHOTO_PREFIX + record.fileName))
            zip.write(bytes)
            zip.closeEntry()
        }
        zip.finish()
        zip.flush()
        BackupSummary(artists.size, albums.size, photos.size, shops.size, groups.size)
    }

    /** Adds what is in the backup read from [input] to the phone. See the class comment for the rules. */
    suspend fun restore(input: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val read = try {
            readZip(input)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext RestoreResult.Failed("The file could not be read as a backup.")
        }
        val json = read.json ?: return@withContext RestoreResult.Failed("This is not a FindMeVinyl backup.")
        val file = try {
            gson.fromJson(String(json, Charsets.UTF_8), BackupFile::class.java)
        } catch (e: Exception) {
            null
        }
        if (file == null || file.app != BackupFile.APP) {
            return@withContext RestoreResult.Failed("This is not a FindMeVinyl backup.")
        }
        val version = file.version ?: 0
        if (version > BackupFile.VERSION) {
            return@withContext RestoreResult.Failed(
                "This backup was made by a newer version of the app. Update the app and try again."
            )
        }
        if (version < 1) return@withContext RestoreResult.Failed("This is not a FindMeVinyl backup.")

        val written = mutableListOf<String>()
        val counts = try {
            database.withTransaction { merge(file, read.photos, written) }
        } catch (e: CancellationException) {
            written.forEach(photoStore::delete)
            throw e
        } catch (e: Exception) {
            written.forEach(photoStore::delete)
            return@withContext RestoreResult.Failed("The backup could not be restored, and nothing was changed.")
        }

        file.settings?.let(applySettings)
        RestoreResult.Done(counts.artists, counts.albums, counts.photos, file.settings != null, counts.shops, counts.groups)
    }

    private class ReadZip(val json: ByteArray?, val photos: Map<String, ByteArray>)

    private fun readZip(input: InputStream): ReadZip {
        var json: ByteArray? = null
        val photos = mutableMapOf<String, ByteArray>()
        val zip = ZipInputStream(input)
        var entries = 0
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > MAX_ENTRIES) throw IllegalStateException("Too many files in the zip")
            when {
                entry.name == BACKUP_JSON ->
                    json = LimitedReader.read(zip, MAX_JSON_BYTES) ?: throw IllegalStateException("backup.json is too big")
                entry.name.startsWith(PHOTO_PREFIX) ->
                    LimitedReader.read(zip, MAX_PHOTO_BYTES)?.let { photos[entry.name] = it }
            }
        }
        return ReadZip(json, photos)
    }

    private class Counts(val artists: Int, val albums: Int, val photos: Int, val shops: Int, val groups: Int)

    // Runs inside the transaction. Everything here only adds to what the phone has.
    private suspend fun merge(file: BackupFile, photoBytes: Map<String, ByteArray>, written: MutableList<String>): Counts {
        val artistDao = database.artistDao()
        val albumDao = database.albumDao()
        val photoDao = database.artistUserPhotoDao()

        var artists = 0
        for (artist in file.favourites.orEmpty()) {
            val id = artist.id?.takeIf { it.isNotBlank() } ?: continue
            val genre = GenreNames.normalise(artist.genre)
            val onPhone = artistDao.getById(id)
            if (onPhone != null) {
                // An artist on the phone is kept as it is, but takes the genre from the file when it has none.
                if (onPhone.genre == null && genre != null) artistDao.setGenre(id, genre)
                continue
            }
            artistDao.insert(ArtistEntity(id, artist.name.orEmpty(), artist.type, artist.country, artist.position ?: 0, genre))
            artists++
        }

        var albums = 0
        for (album in file.albums.orEmpty()) {
            val id = album.id?.takeIf { it.isNotBlank() } ?: continue
            val artistId = album.artistId?.takeIf { it.isNotBlank() } ?: continue
            val owned = album.owned == true
            val wanted = album.wanted == true
            // The details of a copy are checked as they are when the user saves them, whatever the file says.
            val copyFormats = ReleaseFormats.encodeUserOrNull(ReleaseFormats.decodeUser(album.userFormats))
            val copyNote = album.userNote?.trim()?.take(MAX_NOTE_LENGTH)?.ifEmpty { null }
            // Notes are kept for the formats the copy is on, as when the user saves them.
            val copyFormatNotes = FormatNotes.encode(FormatNotes.decode(album.userFormatNotes), ReleaseFormats.decodeUser(copyFormats))
            val hasCopy = copyFormats != null || copyNote != null || copyFormatNotes != null
            // Only a label is kept, and only for an album that is wanted.
            val wantFormat = album.wantFormat?.takeIf { wanted && it in ReleaseFormats.displayOrder }
            // A chosen pressing needs its id, and its line is cut to the length the app keeps.
            val pressingId = album.myPressingId?.trim()?.takeIf { it.isNotEmpty() }
            val pressingText = if (pressingId == null) null else album.myPressingText?.trim()?.take(MAX_NOTE_LENGTH)?.ifEmpty { null }
            if (!owned && !wanted && !hasCopy && pressingId == null) continue

            val existing = albumDao.getById(id)
            var changed = false
            if (existing == null) {
                albumDao.insertIfNotExists(
                    AlbumEntity(
                        id, artistId, album.title.orEmpty(), album.category ?: "OTHER", album.firstReleaseDate,
                        owned, wanted, wantPosition = album.wantPosition ?: 0,
                        userFormats = copyFormats, userNote = copyNote, userFormatNotes = copyFormatNotes,
                        myPressingId = pressingId, myPressingText = pressingText, wantFormat = wantFormat
                    )
                )
                changed = true
            } else {
                if (owned && !existing.owned) {
                    albumDao.setOwned(id, true)
                    changed = true
                }
                if (wanted && !existing.wanted) {
                    albumDao.setWantedAt(id, album.wantPosition ?: 0)
                    changed = true
                }
                if (wantFormat != null && (existing.wanted || wanted) && existing.wantFormat == null) {
                    albumDao.setWantFormat(id, wantFormat)
                    changed = true
                }
                // Details already on the phone are kept: restoring only adds.
                if (copyFormatNotes != null && existing.userFormatNotes == null) {
                    albumDao.setFormatNotes(id, copyFormatNotes)
                    changed = true
                }
                if (hasCopy && existing.userFormats == null && existing.userNote == null) {
                    albumDao.setCopy(id, copyFormats, copyNote)
                    changed = true
                }
                if (pressingId != null && existing.myPressingId == null) {
                    albumDao.setMyPressing(id, pressingId, pressingText)
                    changed = true
                }
            }
            if (changed) albums++
        }

        // A group that is on the phone already is kept as it is. An artist is in one group at most, so one already
        // in another group on the phone is left out, and a group left with fewer than two artists is not restored.
        var groups = 0
        val groupDao = database.artistGroupDao()
        for (group in file.groups.orEmpty()) {
            val id = group.id?.takeIf { ArtistGroupViewModel.isGroupId(it) } ?: continue
            val genre = GenreNames.normalise(group.genre)
            val onPhone = groupDao.getGroup(id)
            if (onPhone != null) {
                if (onPhone.genre == null && genre != null) groupDao.setGenre(id, genre)
                continue
            }
            val members = group.members.orEmpty()
                .mapNotNull { m -> m.artistId?.trim()?.takeIf { it.isNotEmpty() }?.let { GroupMember(it, m.name.orEmpty()) } }
                .distinctBy { it.artistId }
                .filter { groupDao.groupIdOf(it.artistId) == null }
            if (members.size < 2) continue
            val name = group.name?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_GROUP_NAME) ?: members.first().name
            groupDao.insertGroup(ArtistGroupEntity(id, name, group.position ?: 0, genre))
            groupDao.insertMembers(members.mapIndexed { i, m -> ArtistGroupMemberEntity(m.artistId, id, m.name, i) })
            groups++
        }

        var photos = 0
        for (photo in file.photos.orEmpty()) {
            val artistId = photo.artistId?.takeIf { it.isNotBlank() } ?: continue
            // The photo of a group only comes back with the group.
            if (ArtistGroupViewModel.isGroupId(artistId) && groupDao.getGroup(artistId) == null) continue
            val bytes = photoBytes[PHOTO_PREFIX + photo.fileName] ?: continue
            if (photoDao.get(artistId) != null || !isPicture(bytes)) continue
            // The name in the file is never used as a path: the file gets a name made here.
            val fileName = "${safeFileName(artistId)}-${clock()}.jpg"
            photoStore.write(fileName, bytes)
            written.add(fileName)
            photoDao.save(ArtistUserPhotoEntity(artistId, fileName, clock()))
            photos++
        }

        var shops = 0
        for (shop in file.shops.orEmpty()) {
            val id = shop.id?.takeIf { it.isNotBlank() } ?: continue
            val latitude = shop.latitude?.takeIf { it in -90.0..90.0 } ?: continue
            val longitude = shop.longitude?.takeIf { it in -180.0..180.0 } ?: continue
            val category = runCatching { ShopCategory.valueOf(shop.category.orEmpty()) }.getOrDefault(ShopCategory.RECORD_SHOP)
            val added = database.favouriteShopDao().insertIfNotExists(
                FavouriteShopEntity(
                    id, shop.name.orEmpty(), category.name, latitude, longitude, shop.openingHours, shop.street,
                    shop.houseNumber, shop.postcode, shop.city, shop.addressCountry, shop.phone, shop.website, shop.email,
                    shop.secondHandOnly == true, shop.regionCountry, shop.regionSubdivision, shop.savedAt ?: clock(),
                    shop.approximatePosition == true, shop.mapQuery,
                    shop.mapsLink?.takeIf { SharedPlaceParser.isMapsLink(it) }
                )
            )
            if (added > 0) shops++
        }
        return Counts(artists, albums, photos, shops, groups)
    }

    private fun isPicture(bytes: ByteArray): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}
