package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzArtistReleasesResponse
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzReleaseGroupMapper.toDomain
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumCopyEntity
import com.nickallport.findmevinyl.storage.AlbumDao
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.ArtistDao
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGroupDao
import com.nickallport.findmevinyl.favourites.ArtistGroupViewModel
import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import java.util.concurrent.ConcurrentHashMap
import retrofit2.HttpException

sealed class DiscographyUiState {
    data class Success(
        val groupedAlbums: Map<DiscographyCategory, List<AlbumUiModel>>,
        /** More pages are still coming, so the list may grow. */
        val loadingMore: Boolean = false,
        /** A later page failed. The list is what had loaded by then. */
        val loadError: String? = null
    ) : DiscographyUiState()

    data class Error(val message: String) : DiscographyUiState()
}

// How often a page of the format check is asked for when MusicBrainz says it is busy.
private const val FORMAT_CHECK_ATTEMPTS = 3
private val BUSY_CODES = setOf(429, 503)

class DiscographyViewModel(
    private val api: MusicBrainzApi,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val pageSize: Int = 100,
    private val pageDelayMillis: Long = 1_100,
    private val maxPages: Int = 10,
    private val throttle: MusicBrainzThrottle = MusicBrainzThrottle.shared,
    private val clock: () -> Long = System::currentTimeMillis,
    private val formatRefreshAfterMillis: Long = 30L * 24 * 60 * 60 * 1000,
    /** An artist with more releases than this many pages hold is not checked in one go. */
    private val formatCheckMaxPages: Int = 60,
    /**
     * Where the groups of artists are kept. The id of a group stands in for an artist's id: its discography is
     * every member's albums together. Without it every id is an artist.
     */
    private val groupDao: ArtistGroupDao? = null
) {
    /** The artists whose albums make up [artistId]: its members when it is a group, and just itself otherwise. */
    private suspend fun memberIdsOf(artistId: String): List<String> =
        if (ArtistGroupViewModel.isGroupId(artistId)) groupDao?.getMembers(artistId)?.map { it.artistId }.orEmpty() else listOf(artistId)

    // What has been read so far of an artist's releases, so a check that failed or was left carries on.
    private class FormatCheckProgress(val total: Int) {
        var nextOffset = 0
        val formatsByAlbum = mutableMapOf<String, MutableSet<String>>()
        val statusesByAlbum = mutableMapOf<String, MutableSet<String>>()

        fun add(page: MusicBrainzArtistReleasesResponse, pageSize: Int) {
            for (release in page.releases.orEmpty()) {
                val album = release.releaseGroup?.id ?: continue
                val formats = formatsByAlbum.getOrPut(album) { mutableSetOf() }
                release.media.orEmpty().mapNotNullTo(formats) { it.format }
                statusesByAlbum.getOrPut(album) { mutableSetOf() }.add(ReleaseStatuses.token(release.status))
            }
            nextOffset += pageSize
        }
    }

    private val formatChecks = ConcurrentHashMap<String, FormatCheckProgress>()
    private val artistsTooBig = ConcurrentHashMap.newKeySet<String>()

    /**
     * Finds the formats of all of an artist's albums from the artist's releases, a hundred a request,
     * which is far fewer requests than one for each album. A page is asked for in its own turn in the
     * [throttle]. [onProgress] is told after each page how many pages are read and how many there are.
     *
     * The formats are saved at the end, in one go, for the albums the releases belong to: an album's
     * releases can be on any page, so formats read part way would be incomplete. Albums none of the
     * releases belong to stay unchecked. If a page fails, nothing is saved ([FormatCheckResult.Failed]),
     * and the pages read so far are kept for the next try. They are kept too if the caller is cancelled.
     * An artist with more than [formatCheckMaxPages] pages of releases is [FormatCheckResult.TooBig],
     * and is not asked about again in this run of the app.
     */
    suspend fun checkAllFormats(
        artistId: String,
        onProgress: (donePages: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): FormatCheckResult {
        if (!ArtistGroupViewModel.isGroupId(artistId)) return checkArtistFormats(artistId, onProgress)

        // A group is checked one member after another, each as an artist. A member whose albums have all been
        // checked is left out, so a group is not asked about again for what is already known. Progress runs on
        // across the members, and its total grows as each member's first page tells how big that member is.
        var doneBefore = 0
        var totalBefore = 0
        var updated = 0
        var tooBig = false
        for (member in memberIdsOf(artistId)) {
            val albums = albumDao.getAlbumsForArtist(member)
            if (albums.none { needsCheck(it) }) continue
            var memberTotal = 0
            when (val result = checkArtistFormats(member) { done, total ->
                memberTotal = total
                onProgress(doneBefore + done, totalBefore + total)
            }) {
                is FormatCheckResult.Done -> updated += result.updated
                FormatCheckResult.TooBig -> tooBig = true
                FormatCheckResult.Failed -> return FormatCheckResult.Failed
            }
            doneBefore += memberTotal
            totalBefore += memberTotal
        }
        return if (tooBig) FormatCheckResult.TooBig else FormatCheckResult.Done(updated)
    }

    // Formats or statuses never looked up, or looked up too long ago.
    private fun needsCheck(album: AlbumEntity): Boolean =
        album.formats == null || album.statuses == null ||
            album.formatsCheckedAt?.let { clock() - it > formatRefreshAfterMillis } != false

    private suspend fun checkArtistFormats(
        artistId: String,
        onProgress: (donePages: Int, totalPages: Int) -> Unit
    ): FormatCheckResult {
        if (artistId in artistsTooBig) return FormatCheckResult.TooBig
        return try {
            val progress = formatChecks[artistId] ?: run {
                val first = releasesPage(artistId, offset = 0)
                val total = first.releaseCount ?: first.releases.orEmpty().size
                if (pagesFor(total) > formatCheckMaxPages) {
                    artistsTooBig.add(artistId)
                    return FormatCheckResult.TooBig
                }
                FormatCheckProgress(total).also {
                    it.add(first, pageSize)
                    formatChecks[artistId] = it
                    onProgress(pagesFor(it.nextOffset), pagesFor(total))
                }
            }

            while (progress.nextOffset < progress.total) {
                val offset = progress.nextOffset
                val page = releasesPage(artistId, offset)
                progress.add(page, pageSize)
                onProgress(minOf(pagesFor(progress.nextOffset), pagesFor(progress.total)), pagesFor(progress.total))
            }

            val updated = saveChecked(artistId, progress)
            formatChecks.remove(artistId)
            FormatCheckResult.Done(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FormatCheckResult.Failed
        }
    }

    private fun pagesFor(releases: Int): Int = (releases + pageSize - 1) / pageSize

    // A page can come back "busy" (429 or 503), which MusicBrainz sends when it is asked too often. The
    // throttle then holds every request back for a few seconds, so asking again after it works. A check
    // is dozens of requests, so one busy answer must not end it. Any other failure does.
    private suspend fun releasesPage(artistId: String, offset: Int): MusicBrainzArtistReleasesResponse {
        var attempt = 1
        while (true) {
            try {
                return throttle.run { api.getReleasesForArtist(artistId, limit = pageSize, offset = offset) }
            } catch (e: HttpException) {
                if (attempt >= FORMAT_CHECK_ATTEMPTS || e.code() !in BUSY_CODES) throw e
                attempt++
            }
        }
    }

    private suspend fun saveChecked(artistId: String, progress: FormatCheckProgress): Int {
        val checkedAt = clock()
        var updated = 0
        for (album in albumDao.getAlbumsForArtist(artistId)) {
            val names = progress.formatsByAlbum[album.id] ?: continue
            val statuses = progress.statusesByAlbum[album.id].orEmpty()
            albumDao.setChecked(album.id, ReleaseFormats.encode(names), ReleaseStatuses.encode(statuses), checkedAt)
            updated++
        }
        return updated
    }

    /**
     * Looks up the formats of an album's releases on MusicBrainz, saves them, and returns the
     * labels to show. Null means the lookup failed, and nothing is saved, so it can be tried again.
     * The request waits its turn in the throttle. Once it has started, the save is part of it, so a
     * caller that is cancelled meanwhile still leaves the formats stored.
     */
    suspend fun loadFormats(albumId: String): List<String>? {
        return try {
            val names = throttle.run {
                val releases = api.getReleasesForReleaseGroup(releaseGroupId = albumId)
                val found = releases.formatNames()
                albumDao.setChecked(albumId, ReleaseFormats.encode(found), ReleaseStatuses.encode(releases.statusTokens()), clock())
                found
            }
            ReleaseFormats.labels(names, categoryOf(albumId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun categoryOf(albumId: String): DiscographyCategory =
        albumDao.getById(albumId)?.category?.let(::categoryFrom) ?: DiscographyCategory.OTHER

    private fun categoryFrom(stored: String): DiscographyCategory =
        runCatching { DiscographyCategory.valueOf(stored) }.getOrDefault(DiscographyCategory.OTHER)

    /**
     * The discography as it is saved on the phone, with no request to MusicBrainz. Taps on Owned
     * and Want use this, so they show at once. Albums are grouped by their stored category and
     * sorted by release date with undated last, then by title.
     */
    suspend fun loadSaved(artistId: String): DiscographyUiState.Success {
        val now = clock()
        // A group says which member an album is filed under. A single artist's page already has the name.
        val memberNames = if (ArtistGroupViewModel.isGroupId(artistId)) {
            groupDao?.getMembers(artistId).orEmpty().associate { it.artistId to it.name }
        } else {
            emptyMap()
        }
        val copies = albumDao.getAllCopies().groupBy { it.albumId }
        val grouped = memberIdsOf(artistId).flatMap { albumDao.getAlbumsForArtist(it) }.distinctBy { it.id }
            .groupBy { categoryFrom(it.category) }
            .mapValues { (category, albums) ->
                albums
                    // Albums saved before the mapper dropped empty dates still have "", so both mean undated.
                    .sortedWith(
                        compareBy<AlbumEntity, String?>(nullsLast()) { ReleaseDates.sortKey(it.firstReleaseDate) }
                            .thenBy { it.title }
                    )
                    .map { toUiModel(it, category, now, copies[it.id].orEmpty()).copy(artistName = memberNames[it.artistId]) }
            }
        return DiscographyUiState.Success(grouped)
    }

    /** One saved album as the screens show it, or null when it is not saved. */
    suspend fun loadAlbum(albumId: String): AlbumUiModel? =
        albumDao.getById(albumId)?.let { toUiModel(it, categoryFrom(it.category), clock(), albumDao.getCopies(albumId)) }

    private fun toUiModel(album: AlbumEntity, category: DiscographyCategory, now: Long, copies: List<AlbumCopyEntity>) = AlbumUiModel(
        id = album.id,
        title = album.title,
        owned = album.owned,
        wanted = album.wanted,
        formats = album.formats?.let { ReleaseFormats.labels(ReleaseFormats.decode(it), category) },
        formatsStale = album.formatsCheckedAt?.let { now - it > formatRefreshAfterMillis } ?: false,
        statuses = album.statuses?.let(ReleaseStatuses::decode),
        userFormats = copies.map { it.format }.distinct().sortedBy { ReleaseFormats.displayOrder.indexOf(it) },
        note = album.userNote,
        myPressingId = album.myPressingId,
        myPressingText = album.myPressingText,
        wantFormat = album.wantFormat,
        copies = copies.map { AlbumCopy(it.format, it.note) }
    )

    /**
     * Keeps [pressing], one of the album's MusicBrainz pressings, as the user's own copy, with a line that
     * describes it as it is now. Null takes it away.
     */
    suspend fun saveMyPressing(albumId: String, pressing: Pressing?) {
        albumDao.setMyPressing(albumId, pressing?.releaseId, pressing?.let { PressingSummary.oneLine(it) })
    }

    /**
     * Keeps the copies of an album the user owns, each with a format and a note, and a note about the album. They
     * replace the copies kept before. A format that is not a label is dropped, a note is trimmed and cut, a blank note
     * is not kept, and at most [MAX_COPIES] are kept. Several copies on the same format are fine. A lookup never
     * touches them. An album wanted on a format is no longer wanted once one of the copies is on that format.
     */
    suspend fun saveCopies(albumId: String, copies: List<AlbumCopy>, note: String) {
        val kept = copies.filter { it.format in ReleaseFormats.displayOrder }.take(MAX_COPIES)
            .mapIndexed { position, copy ->
                AlbumCopyEntity(
                    albumId = albumId, format = copy.format,
                    note = copy.note?.trim()?.take(MAX_NOTE_LENGTH)?.ifEmpty { null }, position = position
                )
            }
        // The new copies go in before the old ones go, so an interruption leaves too many, never too few.
        val ids = if (kept.isEmpty()) emptyList() else albumDao.insertCopies(kept)
        albumDao.deleteCopiesExcept(albumId, ids)
        albumDao.setNote(albumId, note.trim().take(MAX_NOTE_LENGTH).ifEmpty { null })

        val album = albumDao.getById(albumId) ?: return
        val wantedOn = album.wantFormat
        if (album.wanted && wantedOn != null && kept.any { it.format == wantedOn }) {
            albumDao.setWanted(albumId, false)
        }
    }

    /**
     * Chooses the format a wanted album is wanted on, or null for any. Only a label (LP, CD, ...) is kept, and only
     * while the album is wanted.
     */
    suspend fun saveWantFormat(albumId: String, format: String?) {
        val album = albumDao.getById(albumId) ?: return
        if (!album.wanted) return
        albumDao.setWantFormat(albumId, format?.takeIf { it in ReleaseFormats.displayOrder })
    }

    /**
     * The final state of [discography]: the whole discography once every page has loaded.
     */
    suspend fun loadDiscography(artistId: String): DiscographyUiState = discography(artistId).last()

    /**
     * Loads an artist's discography from MusicBrainz a page at a time. Each page is saved as it
     * arrives, and a state is emitted after it, so the first page can be shown while the rest load.
     * A state with [DiscographyUiState.Success.loadingMore] set is followed by another.
     *
     * An artist opened before starts with the saved albums, before any request. If the first page
     * fails and nothing is saved, the only state is an error. In every other case a failure leaves
     * the list so far as the last state, with [DiscographyUiState.Success.loadError], so the list is
     * never silently short.
     */
    fun discography(artistId: String): Flow<DiscographyUiState> =
        if (ArtistGroupViewModel.isGroupId(artistId)) groupPages(artistId) else singleArtist(artistId)

    private fun singleArtist(artistId: String): Flow<DiscographyUiState> =
        pages(artistId).catch { e ->
            if (e is CancellationException) throw e
            emit(DiscographyUiState.Error(e.message ?: "Something went wrong"))
        }

    /**
     * A group's discography: each member's, one after another (MusicBrainz allows about a request a second, so
     * they are not asked for at once), each shown with the others' as it arrives. The list is always the saved
     * albums of every member, so an album credited to two members is listed once. Only the state after the
     * last member's last page says nothing more is coming. A member that fails does not stop the others: the list
     * keeps what there is and says some albums could not be loaded, or, with nothing at all, is an error.
     */
    private fun groupPages(groupId: String): Flow<DiscographyUiState> = flow {
        val members = memberIdsOf(groupId)
        if (members.isEmpty()) {
            emit(DiscographyUiState.Success(emptyMap()))
            return@flow
        }
        val saved = loadSaved(groupId)
        if (saved.groupedAlbums.isNotEmpty()) emit(saved.copy(loadingMore = true))

        var failure: String? = null
        members.forEachIndexed { index, member ->
            if (index > 0) delay(pageDelayMillis)
            val last = index == members.lastIndex
            singleArtist(member).collect { state ->
                when (state) {
                    is DiscographyUiState.Success -> {
                        if (failure == null) failure = state.loadError
                        emit(loadSaved(groupId).copy(loadingMore = state.loadingMore || !last, loadError = failure))
                    }
                    is DiscographyUiState.Error -> if (failure == null) failure = state.message
                }
            }
        }
        val problem = failure
        if (problem != null) {
            val merged = loadSaved(groupId)
            emit(if (merged.groupedAlbums.isEmpty()) DiscographyUiState.Error(problem) else merged.copy(loadError = problem))
        }
    }

    /**
     * MusicBrainz allows about one request a second, so each page is asked for at least
     * [pageDelayMillis] after the one before it was asked for. A slow response already used that time.
     */
    private fun pages(artistId: String): Flow<DiscographyUiState> = flow {
        // Albums saved on an earlier visit show at once, before any request. With none saved there
        // is nothing to show yet, so no empty list is shown first.
        val saved = loadSaved(artistId)
        val showedSaved = saved.groupedAlbums.isNotEmpty()
        if (showedSaved) emit(saved.copy(loadingMore = true))

        var fetched = 0
        var pages = 0
        var lastStartNanos = 0L
        while (true) {
            if (pages > 0) {
                val sinceLastStartMillis = (System.nanoTime() - lastStartNanos) / 1_000_000
                val remaining = pageDelayMillis - sinceLastStartMillis
                if (remaining > 0) delay(remaining)
            }
            lastStartNanos = System.nanoTime()
            val response = try {
                api.getReleaseGroupsForArtist(artistId = artistId, limit = pageSize, offset = fetched)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Something went wrong"
                // With nothing to show, a failed first page is an error. Otherwise keep the list.
                emit(
                    if (pages == 0 && !showedSaved) DiscographyUiState.Error(message)
                    else loadSaved(artistId).copy(loadError = message)
                )
                return@flow
            }

            albumDao.insertAllIfNotExist(response.releaseGroups.map { newAlbum(artistId, it.toDomain()) })
            fetched += response.releaseGroups.size
            pages++

            // Every saved album came from MusicBrainz, so once as many are saved as it lists, there
            // is nothing new in the pages still to come. That makes reopening an artist one request.
            val nowSaved = loadSaved(artistId)
            val savedCount = nowSaved.groupedAlbums.values.sumOf { it.size }

            val total = response.releaseGroupCount
            val finished = total == null ||
                fetched >= total ||
                savedCount >= total ||
                response.releaseGroups.isEmpty() ||
                pages >= maxPages
            emit(nowSaved.copy(loadingMore = !finished))
            if (finished) return@flow
        }
    }

    private fun newAlbum(artistId: String, releaseGroup: ReleaseGroupDto) = AlbumEntity(
        id = releaseGroup.id,
        artistId = artistId,
        title = releaseGroup.title,
        category = DiscographyCategorizer.categorize(releaseGroup).name,
        firstReleaseDate = releaseGroup.firstReleaseDate,
        owned = false,
        wanted = false
    )

    /**
     * Marking an album owned also saves the artist as a favourite, as wanting an album does. An album can be owned
     * and wanted at once (a worn copy to replace, or a CD owned and the LP wanted), so a want is never cleared here.
     */
    suspend fun toggleOwned(
        albumId: String,
        currentlyOwned: Boolean,
        artistId: String,
        artistName: String
    ) {
        albumDao.setOwned(albumId, !currentlyOwned)
        if (!currentlyOwned) {
            saveFavourite(artistId, artistName)
        }
    }

    suspend fun toggleWanted(
        albumId: String,
        currentlyWanted: Boolean,
        artistId: String,
        artistName: String
    ) {
        if (currentlyWanted) {
            albumDao.setWanted(albumId, false)
        } else {
            albumDao.markWanted(albumId)
            saveFavourite(artistId, artistName)
        }
    }

    /** An artist is a favourite when it is saved. A group of artists always is one, as long as it exists. */
    suspend fun isFavourite(artistId: String): Boolean {
        if (ArtistGroupViewModel.isGroupId(artistId)) return groupDao?.getGroup(artistId) != null
        return artistDao.getById(artistId) != null
    }

    suspend fun saveFavourite(artistId: String, artistName: String) {
        // A group is a favourite already, so owning or wanting an album on its page saves no artist.
        if (ArtistGroupViewModel.isGroupId(artistId)) return
        // A favourite that is already saved keeps its place in the user's order. A new one goes first,
        // one place before the lowest position, which can be below 0. The first ever favourite gets 0.
        if (artistDao.getById(artistId) != null) return
        val position = (artistDao.minPosition() ?: 1) - 1
        artistDao.insert(ArtistEntity(id = artistId, name = artistName, type = null, country = null, position = position))
    }

    suspend fun removeFavourite(artistId: String) {
        albumDao.clearWantedForArtist(artistId)
        artistDao.deleteById(artistId)
    }
}

/** The longest note about a copy that is kept. */
const val MAX_NOTE_LENGTH = 500

/** The most copies of one album the app keeps. */
const val MAX_COPIES = 20
