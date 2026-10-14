package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton

@Composable
fun ArtistSearchScreen(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenFavourites: () -> Unit = {},
    onOpenWantList: () -> Unit = {},
    onOpenShops: () -> Unit = {},
    initialQuery: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }

    Column(modifier = modifier.padding(16.dp)) {
        TextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChange(it)
            },
            modifier = Modifier.testTag("search_field")
        )
        Button(
            onClick = { onSearch(query) },
            modifier = Modifier.testTag("search_button")
        ) {
            Text("Search")
        }
        TextButton(
            onClick = onOpenFavourites,
            modifier = Modifier.testTag("favourites_link")
        ) {
            Text("Favourite Artists")
        }
        TextButton(
            onClick = onOpenWantList,
            modifier = Modifier.testTag("want_list_link")
        ) {
            Text("Want List")
        }
        TextButton(
            onClick = onOpenShops,
            modifier = Modifier.testTag("find_shops_link")
        ) {
            Text("Find Shops")
        }
    }
}