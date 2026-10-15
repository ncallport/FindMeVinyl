package com.nickallport.findmevinyl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.nickallport.findmevinyl.discography.OwnedFirstStore
import com.nickallport.findmevinyl.settings.AppSettings
import com.nickallport.findmevinyl.settings.SettingsStore
import com.nickallport.findmevinyl.ui.settings.SettingsScreen
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
import com.nickallport.findmevinyl.discography.AlbumDetailsViewModel
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApiFactory
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.MIGRATION_1_2
import com.nickallport.findmevinyl.storage.MIGRATION_2_3
import com.nickallport.findmevinyl.storage.MIGRATION_3_4
import com.nickallport.findmevinyl.storage.MIGRATION_4_5
import com.nickallport.findmevinyl.storage.MIGRATION_5_6
import com.nickallport.findmevinyl.storage.MIGRATION_6_7
import com.nickallport.findmevinyl.ui.artist.ArtistResultsRoute
import com.nickallport.findmevinyl.ui.artist.ArtistsScreen
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import android.content.Intent
import com.nickallport.findmevinyl.ui.wantlist.WantListRoute
import com.nickallport.findmevinyl.wantlist.WantListViewModel
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
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.ActivityNotFoundException
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "find-me-vinyl-db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()

        val ownedFirstStore = OwnedFirstStore(getSharedPreferences("owned_first", MODE_PRIVATE))
        val appSettings = AppSettings(SettingsStore(getSharedPreferences("settings", MODE_PRIVATE)))

        val api = MusicBrainzApiFactory.create()
        val searchViewModel = ArtistSearchViewModel(api)
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
            webPicture = { id -> artistImageViewModel.loadImageUrl(id)?.let { ImageLoader.shared.load(it) } }
        )
        val discographyViewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao())
        val albumDetailsViewModel = AlbumDetailsViewModel(api, database.albumDetailsDao())
        val publicHolidayCalendar = PublicHolidayCalendar(NagerApiFactory.create(), database.publicHolidaysDao())
        val favouritesViewModel = FavouritesViewModel(database.artistDao())
        val wantListViewModel = WantListViewModel(database.albumDao())
        val shopSearchViewModel = ShopSearchViewModel(
            ShopApiFactory.createNominatim(),
            ShopApiFactory.createOverpass()
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
                                loadFavourites = { favouritesViewModel.loadFavourites() },
                                onMoveFavouriteUp = { id -> favouritesViewModel.moveUp(id) },
                                onMoveFavouriteDown = { id -> favouritesViewModel.moveDown(id) },
                                loadArtistImage = { id -> artistPictures.load(id) },
                                onArtistSelected = { artist ->
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
                            DiscographyRoute(
                                artistId = screen.artistId,
                                artistName = screen.artistName,
                                load = { id -> discographyViewModel.discography(id) },
                                loadSaved = { id -> discographyViewModel.loadSaved(id) },
                                onToggleOwned = { albumId, currentlyOwned ->
                                    discographyViewModel.toggleOwned(albumId, currentlyOwned)
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
                                startWithOwnedFirst = ownedFirstStore.isOn(screen.artistId),
                                onOwnedFirstChange = { ownedFirstStore.set(screen.artistId, it) },
                                settings = settings,
                                onOpenSettings = { currentScreen = Screen.Settings },
                                loadArtistImage = { id -> artistPictures.load(id) },
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
                                hasUserPhoto = { id -> artistPhotoViewModel.hasPhoto(id) },
                                onUseOriginalPhoto = { id -> artistPhotoViewModel.removePhoto(id) },
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
                        is Screen.AlbumDetail -> {
                            AlbumDetailRoute(
                                albumId = screen.albumId,
                                title = screen.title,
                                artistName = screen.artistName,
                                loadDetails = { id -> albumDetailsViewModel.load(id) },
                                loadAlbum = { id -> discographyViewModel.loadAlbum(id) },
                                onToggleOwned = { albumId, currentlyOwned ->
                                    discographyViewModel.toggleOwned(albumId, currentlyOwned)
                                },
                                onToggleWanted = { albumId, currentlyWanted ->
                                    discographyViewModel.toggleWanted(
                                        albumId, currentlyWanted, screen.artistId, screen.artistName
                                    )
                                },
                                loadCover = { id -> CoverArtLoader.shared.loadLarge(id) },
                                loadPressing = { albumId, releaseId ->
                                    albumDetailsViewModel.selectPressing(albumId, releaseId)
                                }
                            )
                        }
                        is Screen.WantList -> {
                            WantListRoute(
                                load = { wantListViewModel.loadWantList() },
                                exportText = { wantListViewModel.exportText() },
                                onMoveUp = { id -> wantListViewModel.moveUp(id) },
                                onMoveDown = { id -> wantListViewModel.moveDown(id) },
                                onRemove = { id -> wantListViewModel.remove(id) },
                                onShareText = { text -> shareText(text) }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(settings = settings, onChange = { appSettings.set(it) })
                        }
                        is Screen.Shops -> {
                            ShopSearchRoute(
                                hideCharityShops = settings.hideCharityShops,
                                onOpenSettings = { currentScreen = Screen.Settings },
                                search = { town -> shopSearchViewModel.searchTown(town) },
                                searchNearby = { nearbyShopSearcher.findNearby() },
                                onShopSelected = { shop -> openInMaps(shop) },
                                searchPlace = { place, others ->
                                    shopSearchViewModel.searchPlace(place, others)
                                },
                                loadPublicHolidays = { region -> publicHolidayCalendar.datesNear(region, LocalDate.now()) },
                                cache = SearchResultCache.shared
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
        startActivity(Intent.createChooser(sendIntent, "Share your want list"))
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
