package com.nickallport.findmevinyl.barcode

import com.nickallport.findmevinyl.discography.DiscographyCategorizer
import com.nickallport.findmevinyl.discography.DiscographyCategory
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.discography.ReleaseFormats
import com.nickallport.findmevinyl.discography.ReleaseGroupDto
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/** One album a barcode belongs to. Several releases of it (countries, reissues) are one match. */
data class BarcodeMatch(
    val barcode: String,
    val artistId: String,
    val artistName: String,
    /** The id of the release group, which is the album's id in the app. */
    val releaseGroupId: String,
    val albumTitle: String,
    /** The formats the releases with this barcode are on, as labels (LP, CD, ...), in the usual order. Can be empty. */
    val formats: List<String>
)

sealed class BarcodeLookup {
    /** What was typed or read is not a barcode. Nothing was asked. */
    object BadNumber : BarcodeLookup()
    /** MusicBrainz knows no release with this barcode. */
    object NotFound : BarcodeLookup()
    data class Found(val matches: List<BarcodeMatch>) : BarcodeLookup()
    data class Failed(val message: String) : BarcodeLookup()
}

/** What the app already has of a match. */
data class BarcodeStatus(
    val artistIsFavourite: Boolean = false,
    /** The name of the group the artist is in, if it is in one. */
    val groupName: String? = null,
    val albumSaved: Boolean = false,
    val owned: Boolean = false,
    val wanted: Boolean = false,
    /** The format the album is wanted on, or null for any. */
    val wantFormat: String? = null,
    /** The formats of the copies the user has already, one entry for each copy. */
    val copyFormats: List<String> = emptyList()
)

sealed class BarcodeAdd {
    data class Added(val albumId: String) : BarcodeAdd()
    data class Failed(val message: String) : BarcodeAdd()
}

/**
 * Adds albums to the owned list from their barcode. MusicBrainz is asked once to find the album, and once more for
 * its type and date when the album is not saved yet (a search result leaves those out). The artist is saved as a
 * favourite as owning an album does, an album already saved is only marked owned, and a copy on the chosen format is
 * added to the copies it has, so a second copy is a second copy and not a replacement.
 */
class BarcodeViewModel(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val groupDao: ArtistGroupDao,
    private val discography: DiscographyViewModel,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared
) {
    /** Finds the albums with the barcode in [text], which is checked first so a mistyped number costs no request. */
    suspend fun lookUp(text: String): BarcodeLookup {
        val barcode = BarcodeNumber.parse(text) ?: return BarcodeLookup.BadNumber
        val releases = try {
            throttle.run { api.searchReleasesByBarcode("barcode:$barcode").releases.orEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BarcodeLookup.Failed(failureMessage(e))
        }

        val matches = releases.mapNotNull { release ->
            val artist = release.artistCredit?.firstOrNull()?.artist
            val group = release.releaseGroup
            val artistId = artist?.id
            val groupId = group?.id
            if (artistId == null || groupId == null) return@mapNotNull null
            Triple(release, artistId to (artist.name ?: release.artistCredit.first().name.orEmpty()), groupId to group)
        }.groupBy { it.third.first }.map { (groupId, entries) ->
            val (first, artist, group) = entries.first()
            val category = DiscographyCategorizer.categorize(
                ReleaseGroupDto(groupId, group.second.title.orEmpty(), group.second.primaryType, emptyList(), null)
            )
            val names = entries.flatMap { it.first.media.orEmpty() }.mapNotNull { it.format }
            BarcodeMatch(
                barcode = barcode, artistId = artist.first, artistName = artist.second, releaseGroupId = groupId,
                albumTitle = group.second.title ?: first.title.orEmpty(), formats = ReleaseFormats.labels(names, category)
            )
        }
        return if (matches.isEmpty()) BarcodeLookup.NotFound else BarcodeLookup.Found(matches)
    }

    /** What the app has of [match]'s artist and album. */
    suspend fun statusOf(match: BarcodeMatch): BarcodeStatus {
        val groupId = groupDao.groupIdOf(match.artistId)
        val album = albumDao.getById(match.releaseGroupId)
        return BarcodeStatus(
            artistIsFavourite = artistDao.getById(match.artistId) != null,
            groupName = groupId?.let { groupDao.getGroup(it)?.name },
            albumSaved = album != null,
            owned = album?.owned == true,
            wanted = album?.wanted == true,
            wantFormat = album?.takeIf { it.wanted }?.wantFormat,
            copyFormats = if (album == null) emptyList() else albumDao.getCopies(album.id).map { it.format }
        )
    }

    /**
     * Marks the album owned with one more copy on [format], saving the artist and the album first if they are not saved.
     * [takeOffWant] also takes a want on any format off the want list (a want on the same format as the copy always goes).
     * Nothing is saved when the album has to be fetched and that fails.
     */
    suspend fun addOwned(match: BarcodeMatch, format: String, takeOffWant: Boolean = false): BarcodeAdd {
        if (format !in ReleaseFormats.displayOrder) return BarcodeAdd.Failed("Choose a format for the copy.")
        val id = match.releaseGroupId
        if (albumDao.getById(id) == null) {
            val group = try {
                throttle.run { api.getReleaseGroup(id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return BarcodeAdd.Failed(failureMessage(e))
            }
            val dto = ReleaseGroupDto(
                id, group.title, group.primaryType, group.secondaryTypes.orEmpty(),
                group.firstReleaseDate?.takeIf { it.isNotEmpty() }
            )
            discography.saveFavourite(match.artistId, match.artistName)
            albumDao.insertIfNotExists(
                AlbumEntity(
                    id = id, artistId = match.artistId, title = dto.title,
                    category = DiscographyCategorizer.categorize(dto).name, firstReleaseDate = dto.firstReleaseDate,
                    owned = false, wanted = false
                )
            )
        } else {
            discography.saveFavourite(match.artistId, match.artistName)
        }

        albumDao.setOwned(id, true)
        val copies = albumDao.getCopies(id).map { AlbumCopy(it.format, it.note) } + AlbumCopy(format, null)
        discography.saveCopies(id, copies)
        if (takeOffWant && albumDao.getById(id)?.wanted == true) albumDao.setWanted(id, false)
        return BarcodeAdd.Added(id)
    }

    private fun failureMessage(e: Exception): String =
        if (e is HttpException && e.code() in setOf(429, 503)) "MusicBrainz is busy. Try again in a minute."
        else "Could not reach MusicBrainz. Check the connection and try again."
}
