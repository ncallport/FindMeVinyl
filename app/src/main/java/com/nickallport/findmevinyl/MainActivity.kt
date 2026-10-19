package com.nickallport.findmevinyl

import com.nickallport.findmevinyl.discography.CustomAlbums
import com.nickallport.findmevinyl.discography.AddAlbumRoute
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.nickallport.findmevinyl.ui.brand.Masthead
import com.nickallport.findmevinyl.ui.home.HomeScreen
import com.nickallport.findmevinyl.discography.GroupingStore
import com.nickallport.findmevinyl.favourites.FavouritesLayoutStore
import com.nickallport.findmevinyl.backup.BackupMessages
import com.nickallport.findmevinyl.backup.BackupViewModel
import com.nickallport.findmevinyl.settings.AppSettings
import com.nickallport.findmevinyl.settings.SettingsStore
import com.nickallport.findmevinyl.ui.settings.SettingsRoute
import com.nickallport.findmevinyl.shops.ShopSearchUiState
import com.nickallport.findmevinyl.shops.FavouriteShopsViewModel
import com.nickallport.findmevinyl.shops.MapsLinkResolver
import com.nickallport.findmevinyl.shops.ShopPositionFinder
import com.nickallport.findmevinyl.ui.shops.AddShopRoute
import com.nickallport.findmevinyl.ui.shops.FavouriteShopsRoute
import com.nickallport.findmevinyl.ui.shops.ShopDetailRoute
import androidx.compose.runtime.collectAsState
import com.nickallport.findmevinyl.artist.ArtistImageFinder
import com.nickallport.findmevinyl.artist.ArtistPhotoStore
import com.nickallport.findmevinyl.artist.ArtistPhotoViewModel
import com.nickallport.findmevinyl.artist.ArtistPictureSource
import com.nickallport.findmevinyl.artist.LimitedReader
import com.nickallport.findmevinyl.artist.ArtistImageViewModel
import com.nickallport.findmevinyl.artist.ArtistSearchViewModel
import com.nickallport.findmevinyl.image.ImageLoader
import com.nickallport.findmevinyl.wikidata.WikidataApiFactory
import com.nickallport.findmevinyl.coverart.CoverArtLoader
import com.nickallport.findmevinyl.discography.AlbumDetailRoute
import com.nickallport.findmevinyl.discography.GenreControls
import com.nickallport.findmevinyl.genre.ArtistGenreViewModel
import androidx.room.withTransaction
import com.nickallport.findmevinyl.ui.artist.JoinArtistsRoute
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.favourites.ArtistGroupViewModel
import com.nickallport.findmevinyl.discography.AlbumDetailsViewModel
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import com.nickallport.findmevinyl.artist.ArtistSuggester
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApiFactory
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.MIGRATION_1_2
import com.nickallport.findmevinyl.storage.MIGRATION_2_3
import com.nickallport.findmevinyl.storage.MIGRATION_3_4
import com.nickallport.findmevinyl.storage.MIGRATION_4_5
import com.nickallport.findmevinyl.storage.MIGRATION_5_6
import com.nickallport.findmevinyl.storage.MIGRATION_6_7
import com.nickallport.findmevinyl.storage.MIGRATION_7_8
import com.nickallport.findmevinyl.storage.MIGRATION_8_9
import com.nickallport.findmevinyl.storage.MIGRATION_10_11
import com.nickallport.findmevinyl.storage.MIGRATION_11_12
import com.nickallport.findmevinyl.storage.MIGRATION_12_13
import com.nickallport.findmevinyl.storage.MIGRATION_13_14
import com.nickallport.findmevinyl.storage.MIGRATION_14_15
import com.nickallport.findmevinyl.storage.MIGRATION_15_16
import com.nickallport.findmevinyl.storage.MIGRATION_16_17
import com.nickallport.findmevinyl.storage.MIGRATION_17_18
import com.nickallport.findmevinyl.storage.MIGRATION_18_19
import com.nickallport.findmevinyl.storage.MIGRATION_19_20
import com.nickallport.findmevinyl.storage.MIGRATION_20_21
import com.nickallport.findmevinyl.storage.MIGRATION_21_22
import com.nickallport.findmevinyl.storage.MIGRATION_22_23
import com.nickallport.findmevinyl.storage.MIGRATION_9_10
import com.nickallport.findmevinyl.ui.artist.ArtistResultsRoute
import com.nickallport.findmevinyl.ui.artist.ArtistsScreen
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import android.content.Intent
import com.nickallport.findmevinyl.ui.wantlist.WantListRoute
import com.nickallport.findmevinyl.wantlist.WantListViewModel
import com.nickallport.findmevinyl.photon.PlaceSuggester
import com.nickallport.findmevinyl.shops.ShopApiFactory
import com.nickallport.findmevinyl.shops.ShopSearchViewModel
import com.nickallport.findmevinyl.ui.shops.ShopSearchRoute
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import com.nickallport.findmevinyl.location.DeviceLocationProvider
import com.nickallport.findmevinyl.shops.NearbyShopSearcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.net.Uri
import android.widget.Toast
import com.nickallport.findmevinyl.shops.MapLinkBuilder
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.holidays.NagerApiFactory
import com.nickallport.findmevinyl.holidays.PublicHolidayCalendar
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private var permissionResult: CompletableDeferred<Boolean>? = null
    private var photoResult: CompletableDeferred<Uri?>? = null

    // The system photo picker. It needs no permission, because the user picks the one photo to share.
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoResult?.complete(uri)
    }

    // The system file pickers for a backup. They need no permission, because the user picks the one file.
    private var backupFileResult: CompletableDeferred<Uri?>? = null
    private val backupFilePicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        backupFileResult?.complete(uri)
    }
    private var restoreFileResult: CompletableDeferred<Uri?>? = null
    private val restoreFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreFileResult?.complete(uri)
    }

    /** Asks where to save the backup, writes it, and says what happened. Null when the user backed out. */
    private suspend fun backUpData(backup: BackupViewModel): String? {
        val result = CompletableDeferred<Uri?>()
        backupFileResult = result
        backupFilePicker.launch("FindMeVinyl-backup-${LocalDate.now()}.zip")
        val uri = result.await() ?: return null
        return try {
            val summary = contentResolver.openOutputStream(uri, "wt")?.use { backup.createBackup(it) }
            summary?.let(BackupMessages::backedUp) ?: "The backup could not be saved."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "The backup could not be saved."
        }
    }

    /** Asks for a backup file, adds what is in it, and says what happened. Null when the user backed out. */
    private suspend fun restoreData(backup: BackupViewModel): String? {
        val result = CompletableDeferred<Uri?>()
        restoreFileResult = result
        // Any file type is offered, because file apps do not agree on what a zip is called.
        restoreFilePicker.launch(arrayOf("*/*"))
        val uri = result.await() ?: return null
        return try {
            val input = contentResolver.openInputStream(uri) ?: return "The file could not be opened."
            BackupMessages.restored(input.use { backup.restore(it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "The file could not be opened."
        }
    }

    private sealed class PickedPhoto {
        object NoneChosen : PickedPhoto()
        object Unreadable : PickedPhoto()
        class Bytes(val bytes: ByteArray) : PickedPhoto()
    }

    private suspend fun pickPhoto(): PickedPhoto {
        val result = CompletableDeferred<Uri?>()
        photoResult = result
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        val uri = result.await() ?: return PickedPhoto.NoneChosen
        val bytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { LimitedReader.read(it, MAX_PHOTO_BYTES) }
        }
        return bytes?.let { PickedPhoto.Bytes(it) } ?: PickedPhoto.Unreadable
    }

    private companion object {
        // A phone photo is a few megabytes. Anything much bigger is not a photo to put in memory.
        const val MAX_PHOTO_BYTES = 30 * 1024 * 1024
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionResult?.complete(results.values.any { it })
    }

    private fun hasLocationPermission(): Boolean {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun requestLocationPermission(): Boolean {
        if (hasLocationPermission()) return true

        val result = CompletableDeferred<Boolean>()
        permissionResult = result
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        return result.await()
    }

    // Text shared into the app, until the page for adding a shop has been opened for it.
    private var incomingShare by mutableStateOf<String?>(null)

    private fun readShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            incomingShare = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readShare(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A rotation or a return from the background has the same intent again, and it was dealt with the first time.
        if (savedInstanceState == null) readShare(intent)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "find-me-vinyl-db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23).build()

        val favouritesLayoutStore = FavouritesLayoutStore(getSharedPreferences("favourites_layout", MODE_PRIVATE))
        val favouriteShopsViewModel = FavouriteShopsViewModel(database.favouriteShopDao())
        val mapsLinkResolver = MapsLinkResolver()
        val shopPositionFinder = ShopPositionFinder(ShopApiFactory.createNominatim())
        // The file keeps its old name, "owned_first", so artists that had Owned first on keep it.
        val groupingStore = GroupingStore(getSharedPreferences("owned_first", MODE_PRIVATE))
        val appSettings = AppSettings(SettingsStore(getSharedPreferences("settings", MODE_PRIVATE)))

        val backupViewModel = BackupViewModel(
            database,
            ArtistPhotoStore(File(filesDir, "artist_photos")),
            currentSettings = { appSettings.settings.value },
            applySettings = { appSettings.set(it) }
        )

        val api = MusicBrainzApiFactory.create()
        val searchViewModel = ArtistSearchViewModel(api)
        val artistSuggester = ArtistSuggester(api)
        val artistImageViewModel = ArtistImageViewModel(
            ArtistImageFinder(api, WikidataApiFactory.create()),
            database.artistImageDao()
        )
        val artistPhotoViewModel = ArtistPhotoViewModel(
            database.artistUserPhotoDao(),
            ArtistPhotoStore(File(filesDir, "artist_photos"))
        )
        // A photo the user chose wins over the picture from the web, and then the web is not asked.
        val artistPictures = ArtistPictureSource(
            userPhoto = { id -> artistPhotoViewModel.loadPhoto(id) },
            webPicture = { id -> artistImageViewModel.loadImageUrl(id)?.let { ImageLoader.shared.load(it) } },
            // The header of an artist's page shows it bigger, so it asks Commons for more pixels.
            largeWebPicture = { id ->
                artistImageViewModel.loadImageUrl(id)?.let { ImageLoader.shared.load(ArtistImageFinder.atWidth(it, 400)) }
            },
            // A group shows the photo chosen for it, and otherwise its first member's picture.
            firstMemberOf = { groupId -> database.artistGroupDao().getMembers(groupId).firstOrNull()?.artistId }
        )
        val discographyViewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao(), groupDao = database.artistGroupDao())
        val albumDetailsViewModel = AlbumDetailsViewModel(api, database.albumDetailsDao())
        val publicHolidayCalendar = PublicHolidayCalendar(NagerApiFactory.create(), database.publicHolidaysDao())
        val favouritesViewModel = FavouritesViewModel(database.artistDao(), database.artistGroupDao())
        val artistGenreViewModel = ArtistGenreViewModel(api, database.artistGenresDao(), database.artistDao(), database.artistGroupDao())
        val artistGroupViewModel = ArtistGroupViewModel(
            database.artistDao(), database.artistGroupDao(),
            inTransaction = { block -> database.withTransaction { block() } }
        )
        val wantListViewModel = WantListViewModel(database.albumDao())
        val placeSuggester = PlaceSuggester(ShopApiFactory.createPhoton())
        val shopSearchViewModel = ShopSearchViewModel(
            ShopApiFactory.createNominatim(),
            ShopApiFactory.createOverpass(),
            ShopApiFactory.createOverpassFallbacks(),
            customShops = { favouriteShopsViewModel.loadAll().map { it.shop }.filter { it.isCustom } }
        )
        val locationProvider = DeviceLocationProvider(applicationContext)
        val nearbyShopSearcher = NearbyShopSearcher(
            requestPermission = { requestLocationPermission() },
            currentLocation = { locationProvider.currentLocation() },
            searchAround = { latitude, longitude ->
                shopSearchViewModel.searchAround(latitude, longitude)
            }
        )

        setContent {
            FindMeVinylTheme {
                // The window colour behind the status and navigation bars follows the theme, so the
                // clock and icons stay readable in both light and dark.
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val settings by appSettings.settings.collectAsState()
                    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
                        mutableStateOf<Screen>(Screen.Home)
                    }

                    // Text shared into the app from Google Maps opens the page for adding a shop.
                    LaunchedEffect(incomingShare) {
                        incomingShare?.let { text ->
                            currentScreen = Screen.AddShop(text)
                            incomingShare = null
                        }
                    }

                    BackHandler(enabled = currentScreen.backTarget() != null) {
                        currentScreen.backTarget()?.let { currentScreen = it }
                    }

                    Column {
                    // Every page except the front screen has the logo bar at the top.
                    if (currentScreen != Screen.Home) {
                        Masthead(onHome = { currentScreen = Screen.Home })
                    }

                    Box(modifier = Modifier.weight(1f)) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                onArtists = { currentScreen = Screen.Search },
                                onWants = { currentScreen = Screen.WantList },
                                onShops = { currentScreen = Screen.Shops },
                                onSettings = { currentScreen = Screen.Settings }
                            )
                        }
                        is Screen.Search -> {
                            ArtistsScreen(
                                onSearch = { query -> currentScreen = Screen.ArtistResults(query) },
                                suggestArtists = { text -> artistSuggester.suggest(text) },
                                loadFavourites = { favouritesViewModel.loadFavourites() },
                                onMoveFavouriteUp = { id -> favouritesViewModel.moveUp(id) },
                                onMoveFavouriteDown = { id -> favouritesViewModel.moveDown(id) },
                                onReorderFavourites = { ids -> favouritesViewModel.reorder(ids) },
                                startInGrid = favouritesLayoutStore.isGrid(),
                                onGridChange = { favouritesLayoutStore.setGrid(it) },
                                startGroupedByGenre = favouritesLayoutStore.isGroupedByGenre(),
                                onGroupedByGenreChange = { favouritesLayoutStore.setGroupedByGenre(it) },
                                startSortedAz = favouritesLayoutStore.isSortedAz(),
                                onSortedAzChange = { favouritesLayoutStore.setSortedAz(it) },
                                fillGenreDefaults = { onFound -> artistGenreViewModel.fillDefaults(onFound) },
                                loadArtistImage = { id -> artistPictures.load(id) },
                                onArtistSelected = { artist ->
                                    // A group opens on the discography of all its artists, under the group's name.
                                    currentScreen = Screen.Discography(artist.id, artist.name, Screen.Search)
                                }
                            )
                        }
                        is Screen.ArtistResults -> {
                            ArtistResultsRoute(
                                query = screen.query,
                                search = { query -> searchViewModel.search(query) },
                                loadArtistImage = { id -> artistPictures.load(id) },
                                onArtistSelected = { artist ->
                                    currentScreen = Screen.Discography(artist.id, artist.name, screen)
                                },
                                onSearch = { query -> currentScreen = Screen.ArtistResults(query) },
                                cache = SearchResultCache.shared
                            )
                        }
                        is Screen.Discography -> {
                            var inGroup by remember(screen.artistId) { mutableStateOf(false) }
                            LaunchedEffect(screen.artistId) { inGroup = artistGroupViewModel.groupOf(screen.artistId) != null }
                            DiscographyRoute(
                                artistId = screen.artistId,
                                artistName = screen.artistName,
                                load = { id -> discographyViewModel.discography(id) },
                                loadSaved = { id -> discographyViewModel.loadSaved(id) },
                                onToggleOwned = { albumId, currentlyOwned ->
                                    discographyViewModel.toggleOwned(
                                        albumId, currentlyOwned, screen.artistId, screen.artistName
                                    )
                                },
                                onToggleWanted = { albumId, currentlyWanted ->
                                    discographyViewModel.toggleWanted(
                                        albumId, currentlyWanted, screen.artistId, screen.artistName
                                    )
                                },
                                loadIsFavourite = { discographyViewModel.isFavourite(screen.artistId) },
                                onSaveFavourite = { id, name -> discographyViewModel.saveFavourite(id, name) },
                                onRemoveFavourite = { id -> discographyViewModel.removeFavourite(id) },
                                onOpenArtists = { currentScreen = Screen.Search },
                                onJoinArtists = { currentScreen = Screen.JoinArtists(screen.artistId, screen.artistName, screen) },
                                inGroup = inGroup,
                                isGroup = ArtistGroupViewModel.isGroupId(screen.artistId),
                                genreControls = remember(screen.artistId) {
                                    GenreControls(
                                        load = { artistGenreViewModel.genreOf(screen.artistId) },
                                        suggested = { artistGenreViewModel.autoGenreOf(screen.artistId) },
                                        options = { artistGenreViewModel.options(screen.artistId) },
                                        set = { genre -> artistGenreViewModel.setGenre(screen.artistId, genre) }
                                    )
                                },
                                startWithGrouping = groupingStore.get(screen.artistId),
                                onGroupingChange = { groupingStore.set(screen.artistId, it) },
                                settings = settings,
                                onOpenSettings = { currentScreen = Screen.Settings },
                                loadArtistImage = { id -> artistPictures.load(id, large = true) },
                                onChoosePhoto = { id ->
                                    when (val picked = pickPhoto()) {
                                        PickedPhoto.NoneChosen -> false
                                        PickedPhoto.Unreadable -> {
                                            Toast.makeText(this@MainActivity, "Couldn't use that photo", Toast.LENGTH_SHORT).show()
                                            false
                                        }
                                        is PickedPhoto.Bytes -> artistPhotoViewModel.savePhoto(id, picked.bytes).also { saved ->
                                            if (!saved) {
                                                Toast.makeText(this@MainActivity, "Couldn't use that photo", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onAddAlbum = { currentScreen = Screen.AddAlbum(screen.artistId, screen.artistName, null, screen) },
                                onOpenAlbum = { albumId, title ->
                                    currentScreen = Screen.AlbumDetail(
                                        albumId, title, screen.artistId, screen.artistName, screen
                                    )
                                },
                                loadCover = { id -> CoverArtLoader.shared.load(id) },
                                loadFormats = { id -> discographyViewModel.loadFormats(id) },
                                checkAllFormats = { id, onProgress -> discographyViewModel.checkAllFormats(id, onProgress) }
                            )
                        }
                        is Screen.JoinArtists -> {
                            JoinArtistsRoute(
                                startArtist = GroupMember(screen.artistId, screen.artistName),
                                loadGroup = { id -> artistGroupViewModel.groupOf(id) },
                                loadFavourites = { favouritesViewModel.loadFavourites() },
                                suggestArtists = { text -> artistSuggester.suggest(text) },
                                save = { groupId, name, members ->
                                    if (groupId == null) artistGroupViewModel.create(name, members)
                                    else artistGroupViewModel.update(groupId, name, members)
                                },
                                ungroup = { id -> artistGroupViewModel.ungroup(id) },
                                // Saved or split up: the favourites show the result.
                                onDone = { currentScreen = Screen.Search },
                                onCancel = { currentScreen = screen.backTo }
                            )
                        }
                        is Screen.AddAlbum -> {
                            val editing = screen.editingAlbumId
                            AddAlbumRoute(
                                artistName = screen.artistName,
                                editingAlbumId = editing,
                                loadDraft = { id -> discographyViewModel.loadDraft(id) },
                                save = { draft ->
                                    if (editing != null) {
                                        discographyViewModel.updateAlbum(editing, draft)
                                    } else {
                                        discographyViewModel.addAlbum(screen.artistId, screen.artistName, draft) != null
                                    }
                                },
                                delete = { if (editing != null) discographyViewModel.deleteAlbum(editing) },
                                showHiddenNote = settings.hideBootlegs,
                                onOpenSettings = { currentScreen = Screen.Settings },
                                // Changed: back to the album's page with its new title. Added: back to the discography.
                                onSaved = { draft ->
                                    val from = screen.backTo
                                    currentScreen = if (from is Screen.AlbumDetail) from.copy(title = draft.title.trim()) else from
                                },
                                // Deleted: the album's page is gone, so back to the discography behind it.
                                onDeleted = { currentScreen = (screen.backTo as? Screen.AlbumDetail)?.backTo ?: screen.backTo },
                                onCancel = { currentScreen = screen.backTo }
                            )
                        }
                        is Screen.AlbumDetail -> {
                            AlbumDetailRoute(
                                albumId = screen.albumId,
                                title = screen.title,
                                artistName = screen.artistName,
                                loadDetails = { id -> albumDetailsViewModel.load(id) },
                                loadAlbum = { id -> discographyViewModel.loadAlbum(id) },
                                onToggleOwned = { albumId, currentlyOwned ->
                                    discographyViewModel.toggleOwned(
                                        albumId, currentlyOwned, screen.artistId, screen.artistName
                                    )
                                },
                                onToggleWanted = { albumId, currentlyWanted ->
                                    discographyViewModel.toggleWanted(
                                        albumId, currentlyWanted, screen.artistId, screen.artistName
                                    )
                                },
                                loadCover = { id -> CoverArtLoader.shared.loadLarge(id) },
                                loadPressing = { albumId, releaseId ->
                                    albumDetailsViewModel.selectPressing(albumId, releaseId)
                                },
                                saveCopies = { albumId, copies -> discographyViewModel.saveCopies(albumId, copies) },
                                saveWantFormat = { albumId, format -> discographyViewModel.saveWantFormat(albumId, format) },
                                saveMyPressing = { albumId, pressing -> discographyViewModel.saveMyPressing(albumId, pressing) },
                                onEditAlbum = if (CustomAlbums.isCustom(screen.albumId)) {
                                    { currentScreen = Screen.AddAlbum(screen.artistId, screen.artistName, screen.albumId, screen) }
                                } else {
                                    null
                                },
                                // The page the album was opened from is the artist's, or the group's.
                                onOpenArtist = {
                                    currentScreen = screen.backTo as? Screen.Discography
                                        ?: Screen.Discography(screen.artistId, screen.artistName, Screen.Search)
                                }
                            )
                        }
                        is Screen.WantList -> {
                            WantListRoute(
                                load = { wantListViewModel.loadWantList() },
                                exportText = { options -> wantListViewModel.exportText(options) },
                                onMoveUp = { id -> wantListViewModel.moveUp(id) },
                                onMoveDown = { id -> wantListViewModel.moveDown(id) },
                                onRemove = { id -> wantListViewModel.remove(id) },
                                onShareText = { text -> shareText(text) }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsRoute(
                                settings = settings,
                                onChange = { appSettings.set(it) },
                                backUp = { backUpData(backupViewModel) },
                                restore = { restoreData(backupViewModel) }
                            )
                        }
                        is Screen.Shops -> {
                            // Read again each time the page opens, so a shop kept or let go on its page shows here.
                            var favouriteShopIds by remember { mutableStateOf(emptySet<String>()) }
                            LaunchedEffect(Unit) { favouriteShopIds = favouriteShopsViewModel.favouriteIds() }
                            ShopSearchRoute(
                                favouriteShopIds = favouriteShopIds,
                                onOpenFavourites = { currentScreen = Screen.FavouriteShops },
                                hideCharityShops = settings.hideCharityShops,
                                hideSecondHandShops = settings.hideSecondHandShops,
                                onOpenSettings = { currentScreen = Screen.Settings },
                                search = { town -> shopSearchViewModel.searchTown(town) },
                                searchNearby = { nearbyShopSearcher.findNearby() },
                                suggestPlaces = { text -> placeSuggester.suggest(text) },
                                searchSuggestion = { suggestion -> shopSearchViewModel.searchSuggestion(suggestion) },
                                onShopSelected = { shop ->
                                    // The region of the search, for the public holidays and the layout of the address.
                                    val region = (SearchResultCache.shared.shop as? ShopSearchUiState.Success)?.region
                                    currentScreen = Screen.ShopDetail(shop, region)
                                },
                                searchPlace = { place, others ->
                                    shopSearchViewModel.searchPlace(place, others)
                                },
                                loadPublicHolidays = { region -> publicHolidayCalendar.datesNear(region, LocalDate.now()) },
                                cache = SearchResultCache.shared
                            )
                        }
                        is Screen.FavouriteShops -> {
                            FavouriteShopsRoute(
                                load = { favouriteShopsViewModel.loadAll() },
                                onShopSelected = { favourite ->
                                    currentScreen = Screen.ShopDetail(favourite.shop, favourite.region, backTo = Screen.FavouriteShops)
                                },
                                onAddShop = { currentScreen = Screen.AddShop(null) }
                            )
                        }
                        is Screen.AddShop -> {
                            AddShopRoute(
                                sharedText = screen.sharedText,
                                editing = screen.editing,
                                resolveLink = { link -> mapsLinkResolver.resolve(link) },
                                findPosition = { address -> shopPositionFinder.find(address) },
                                currentPosition = {
                                    if (requestLocationPermission()) {
                                        try {
                                            locationProvider.currentLocation()
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                },
                                onSave = { shop ->
                                    favouriteShopsViewModel.add(shop, null)
                                    currentScreen = Screen.ShopDetail(shop, null, backTo = Screen.FavouriteShops)
                                }
                            )
                        }
                        is Screen.ShopDetail -> {
                            ShopDetailRoute(
                                shop = screen.shop,
                                region = screen.region,
                                loadIsFavourite = { favouriteShopsViewModel.isFavourite(screen.shop.id) },
                                onOpenMapsLink = { url -> openLink(Intent.ACTION_VIEW, url, "No web browser or maps app found on this device") },
                                onEdit = if (screen.shop.isCustom) {
                                    { currentScreen = Screen.AddShop(null, screen.shop) }
                                } else {
                                    null
                                },
                                onSetFavourite = { keep ->
                                    if (keep) favouriteShopsViewModel.add(screen.shop, screen.region)
                                    else favouriteShopsViewModel.remove(screen.shop.id)
                                },
                                loadPublicHolidays = { region -> publicHolidayCalendar.datesNear(region, LocalDate.now()) },
                                onOpenMap = { openInMaps(screen.shop) },
                                onCall = { uri -> openLink(Intent.ACTION_DIAL, uri, "No phone app found on this device") },
                                onOpenWebsite = { url -> openLink(Intent.ACTION_VIEW, url, "No web browser found on this device") },
                                onSendEmail = { uri -> openLink(Intent.ACTION_SENDTO, uri, "No email app found on this device") }
                            )
                        }
                    }
                    }
                    }
                }
                }
            }
        }
    }

    private fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share your want list").apply {
            // The app takes shared text now, to add a shop, but a want list is for other people.
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this@MainActivity, MainActivity::class.java)))
        }
        startActivity(chooser)
    }

    /** Hands a link that has already been checked ([com.nickallport.findmevinyl.shops.WebLink] and the like) to another app. */
    private fun openLink(action: String, uri: String, noAppMessage: String) {
        try {
            startActivity(Intent(action, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, noAppMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInMaps(shop: Shop) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MapLinkBuilder.geoUri(shop)))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No map app found on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
