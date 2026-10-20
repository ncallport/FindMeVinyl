package com.nickallport.findmevinyl.barcode

import com.nickallport.findmevinyl.discography.AlbumDraft
import com.nickallport.findmevinyl.discography.CustomAlbums
import com.nickallport.findmevinyl.discogs.DiscogsApi
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
    /**
     * The id of the release group, which is the album's id in the app. For an album [byHand] it is only a key for the
     * page, made from the names, because the album has no id until it is added.
     */
    val releaseGroupId: String,
    val albumTitle: String,
    /** The formats the releases with this barcode are on, as labels (LP, CD, ...), in the usual order. Can be empty. */
    val formats: List<String>,
    /** MusicBrainz has no release group of this name under the artist, so the album is added by hand under the artist. */
    val byHand: Boolean = false,
    /** MusicBrainz did not know the barcode: the album was found on Discogs and matched to MusicBrainz by name. */
    val fromDiscogs: Boolean = false,
    /** The year, for an album added by hand. */
    val year: String? = null,
    /** The kind of release, for an album added by hand. */
    val category: DiscographyCategory = DiscographyCategory.STUDIO_ALBUM
)

sealed class BarcodeLookup {
    /** What was typed or read is not a barcode. Nothing was asked. */
    object BadNumber : BarcodeLookup()
    /** MusicBrainz knows no release with this barcode. */
    object NotFound : BarcodeLookup()
    data class Found(val matches: List<BarcodeMatch>) : BarcodeLookup()
    /** Discogs lists the barcode, but no artist of that name is on MusicBrainz (or it is a compilation by various artists). */
    data class OnlyOnDiscogs(val finds: List<DiscogsFind>) : BarcodeLookup()
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
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    /** Asked when MusicBrainz does not know a barcode. Without it that is the end of the search. */
    private val discogs: DiscogsApi? = null
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
        if (matches.isNotEmpty()) return BarcodeLookup.Found(matches)
        return if (discogs == null) BarcodeLookup.NotFound else lookUpOnDiscogs(barcode, discogs)
    }

    // Discogs knows more barcodes of records than MusicBrainz, but not the MusicBrainz ids the app files albums under, so
    // each album it lists is looked for on MusicBrainz by name: the artist first, then the artist's album of that title.
    private suspend fun lookUpOnDiscogs(barcode: String, discogs: DiscogsApi): BarcodeLookup {
        val finds = try {
            DiscogsReader.parse(discogs.searchByBarcode(barcode).results.orEmpty()).take(MAX_DISCOGS_FINDS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BarcodeLookup.Failed("MusicBrainz has no album with that barcode, and Discogs could not be asked. Try again in a minute.")
        }
        if (finds.isEmpty()) return BarcodeLookup.NotFound

        val matches = mutableListOf<BarcodeMatch>()
        for (find in finds) {
            val match = try {
                matchOnMusicBrainz(barcode, find)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return BarcodeLookup.Failed(failureMessage(e))
            }
            if (match != null) matches.add(match)
        }
        return if (matches.isEmpty()) BarcodeLookup.OnlyOnDiscogs(finds) else BarcodeLookup.Found(matches)
    }

    private suspend fun matchOnMusicBrainz(barcode: String, find: DiscogsFind): BarcodeMatch? {
        if (find.variousArtists) return null
        val artist = throttle.run { api.searchArtists("artist:\"${phrase(find.artist)}\"", limit = 5).artists }
            .firstOrNull { MatchNames.same(find.artist, it.name) } ?: return null
        val group = throttle.run {
            api.searchReleaseGroups("releasegroup:\"${phrase(find.album)}\" AND arid:${artist.id}").releaseGroups
        }.firstOrNull { MatchNames.same(find.album, it.title) }
        val formats = listOfNotNull(find.format)
        return if (group != null) {
            BarcodeMatch(barcode, artist.id, artist.name, group.id, group.title, formats, fromDiscogs = true)
        } else {
            BarcodeMatch(
                barcode, artist.id, artist.name, "byhand:" + MatchNames.normalise(artist.name) + "|" + MatchNames.normalise(find.album),
                find.album, formats, byHand = true, fromDiscogs = true, year = find.year, category = find.category
            )
        }
    }

    // A name inside quotes in a search: only the quote and the backslash have a meaning there.
    private fun phrase(name: String): String = name.replace("\\", "\\\\").replace("\"", "\\\"")

    /** The album added by hand that stands for [match], when an earlier scan added it. */
    private suspend fun existingByHand(match: BarcodeMatch): AlbumEntity? =
        albumDao.getAlbumsForArtist(match.artistId).firstOrNull {
            CustomAlbums.isCustom(it.id) && MatchNames.same(it.title, match.albumTitle)
        }

    private suspend fun savedAlbum(match: BarcodeMatch): AlbumEntity? =
        if (match.byHand) existingByHand(match) else albumDao.getById(match.releaseGroupId)

    /** What the app has of [match]'s artist and album. */
    suspend fun statusOf(match: BarcodeMatch): BarcodeStatus {
        val groupId = groupDao.groupIdOf(match.artistId)
        val album = savedAlbum(match)
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
        if (match.byHand) return addByHand(match, format)
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

    // An album MusicBrainz does not list goes in as one added by hand under the artist, unless an earlier scan added it.
    private suspend fun addByHand(match: BarcodeMatch, format: String): BarcodeAdd {
        val existing = existingByHand(match)
        val id = existing?.id ?: discography.addAlbum(
            match.artistId, match.artistName,
            AlbumDraft(match.albumTitle, match.year.orEmpty(), match.category, bootleg = false)
        ) ?: return BarcodeAdd.Failed("Could not save that album.")
        if (existing != null) {
            discography.saveFavourite(match.artistId, match.artistName)
            albumDao.setOwned(id, true)
        }
        val copies = albumDao.getCopies(id).map { AlbumCopy(it.format, it.note) } + AlbumCopy(format, null)
        discography.saveCopies(id, copies)
        return BarcodeAdd.Added(id)
    }

    private fun failureMessage(e: Exception): String =
        if (e is HttpException && e.code() in setOf(429, 503)) "MusicBrainz is busy. Try again in a minute."
        else "Could not reach MusicBrainz. Check the connection and try again."
}

/** The most albums Discogs lists for one barcode that are looked for on MusicBrainz. */
private const val MAX_DISCOGS_FINDS = 3
