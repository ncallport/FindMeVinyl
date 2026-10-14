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
import com.nickallport.findmevinyl.artist.ArtistSearchViewModel
import com.nickallport.findmevinyl.coverart.CoverArtLoader
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApiFactory
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.MIGRATION_1_2
import com.nickallport.findmevinyl.ui.artist.ArtistSearchRoute
import com.nickallport.findmevinyl.ui.favourites.FavouritesRoute
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
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : ComponentActivity() {

    private var permissionResult: CompletableDeferred<Boolean>? = null

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
        ).addMigrations(MIGRATION_1_2).build()

        val api = MusicBrainzApiFactory.create()
        val searchViewModel = ArtistSearchViewModel(api)
        val discographyViewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao())
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
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
                        mutableStateOf<Screen>(Screen.Search)
                    }
                    var searchQuery by rememberSaveable { mutableStateOf("") }

                    BackHandler(enabled = currentScreen != Screen.Search) {
                        currentScreen = when (val screen = currentScreen) {
                            is Screen.Discography -> screen.backTo
                            else -> Screen.Search
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Search -> {
                            ArtistSearchRoute(
                                search = { query -> searchViewModel.search(query) },
                                onArtistSelected = { artist ->
                                    currentScreen = Screen.Discography(artist.id, artist.name, Screen.Search)
                                },
                                onOpenFavourites = { currentScreen = Screen.Favourites },
                                onOpenWantList = { currentScreen = Screen.WantList },
                                onOpenShops = { currentScreen = Screen.Shops },
                                initialQuery = searchQuery,
                                onQueryChange = { searchQuery = it },
                                cache = SearchResultCache.shared
                            )
                        }
                        is Screen.Favourites -> {
                            FavouritesRoute(
                                load = { favouritesViewModel.loadFavourites() },
                                onArtistSelected = { artist ->
                                    currentScreen = Screen.Discography(artist.id, artist.name, Screen.Favourites)
                                }
                            )
                        }
                        is Screen.Discography -> {
                            DiscographyRoute(
                                artistId = screen.artistId,
                                artistName = screen.artistName,
                                load = { id -> discographyViewModel.loadDiscography(id) },
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
                                loadCover = { id -> CoverArtLoader.shared.load(id) },
                                loadFormats = { id -> discographyViewModel.loadFormats(id) }
                            )
                        }
                        is Screen.WantList -> {
                            WantListRoute(
                                load = { wantListViewModel.loadWantList() },
                                exportText = { wantListViewModel.exportText() },
                                onShareText = { text -> shareText(text) }
                            )
                        }
                        is Screen.Shops -> {
                            ShopSearchRoute(
                                search = { town -> shopSearchViewModel.searchTown(town) },
                                searchNearby = { nearbyShopSearcher.findNearby() },
                                onShopSelected = { shop -> openInMaps(shop) },
                                searchPlace = { place, others ->
                                    shopSearchViewModel.searchPlace(place, others)
                                },
                                cache = SearchResultCache.shared
                            )
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