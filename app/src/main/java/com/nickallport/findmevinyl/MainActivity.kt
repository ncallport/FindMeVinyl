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
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.favourites.FavouritesViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApiFactory
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.ui.artist.ArtistSearchRoute
import com.nickallport.findmevinyl.ui.favourites.FavouritesRoute
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import android.content.Intent
import com.nickallport.findmevinyl.ui.wantlist.WantListRoute
import com.nickallport.findmevinyl.wantlist.WantListViewModel

private sealed class Screen {
    object Search : Screen()
    object Favourites : Screen()
    object WantList : Screen()
    data class Discography(
        val artistId: String,
        val artistName: String,
        val backTo: Screen
    ) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "find-me-vinyl-db"
        ).build()

        val api = MusicBrainzApiFactory.create()
        val searchViewModel = ArtistSearchViewModel(api)
        val discographyViewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao())
        val favouritesViewModel = FavouritesViewModel(database.artistDao())
        val wantListViewModel = WantListViewModel(database.albumDao())

        setContent {
            FindMeVinylTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Search) }

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
                                onOpenWantList = { currentScreen = Screen.WantList }
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
                                onRemoveFavourite = { id -> discographyViewModel.removeFavourite(id) }
                            )
                        }
                        is Screen.WantList -> {
                            WantListRoute(
                                load = { wantListViewModel.loadWantList() },
                                exportText = { wantListViewModel.exportText() },
                                onShareText = { text -> shareText(text) }
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
}