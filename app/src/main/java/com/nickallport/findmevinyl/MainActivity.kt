package com.nickallport.findmevinyl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nickallport.findmevinyl.artist.ArtistSearchViewModel
import com.nickallport.findmevinyl.discography.DiscographyViewModel
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApiFactory
import com.nickallport.findmevinyl.discography.DiscographyRoute
import com.nickallport.findmevinyl.ui.artist.ArtistSearchRoute
import com.nickallport.findmevinyl.ui.theme.FindMeVinylTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

private sealed class Screen {
    object Search : Screen()
    data class Discography(val artistId: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val api = MusicBrainzApiFactory.create()
        val searchViewModel = ArtistSearchViewModel(api)
        val discographyViewModel = DiscographyViewModel(api)

        setContent {
            FindMeVinylTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Search) }

                    when (val screen = currentScreen) {
                        is Screen.Search -> {
                            ArtistSearchRoute(
                                search = { query -> searchViewModel.search(query) },
                                onArtistSelected = { artistId ->
                                    currentScreen = Screen.Discography(artistId)
                                }
                            )
                        }
                        is Screen.Discography -> {
                            DiscographyRoute(
                                artistId = screen.artistId,
                                load = { id -> discographyViewModel.loadDiscography(id) },
                                onToggleOwned = { /* Room wiring comes later */ }
                            )
                        }
                    }
                }
            }
        }
    }
}