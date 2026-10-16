package com.nickallport.findmevinyl.ui.shops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nickallport.findmevinyl.shops.FavouriteShop
import com.nickallport.findmevinyl.ui.brand.RecordSpinner

/** The favourite shops, read from the phone when the page opens. */
@Composable
fun FavouriteShopsRoute(
    load: suspend () -> List<FavouriteShop>,
    onShopSelected: (FavouriteShop) -> Unit,
    modifier: Modifier = Modifier,
    onAddShop: (() -> Unit)? = null
) {
    var shops by remember { mutableStateOf<List<FavouriteShop>?>(null) }

    LaunchedEffect(Unit) {
        shops = load()
    }

    val loaded = shops
    if (loaded == null) {
        RecordSpinner(modifier = modifier.testTag("loading_indicator"))
    } else {
        FavouriteShopsScreen(shops = loaded, onShopSelected = onShopSelected, modifier = modifier, onAddShop = onAddShop)
    }
}
