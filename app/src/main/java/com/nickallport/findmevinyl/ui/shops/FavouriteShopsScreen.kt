package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.shops.FavouriteShop
import com.nickallport.findmevinyl.ui.brand.Glyphs

private const val NO_TOWN = "Other places"

/**
 * The shops the user keeps, under the town each is in, in the order given (town, then name). Each is a row like
 * those in a search, with whether it is open now, and opens the shop's page.
 */
@Composable
fun FavouriteShopsScreen(
    shops: List<FavouriteShop>,
    onShopSelected: (FavouriteShop) -> Unit,
    modifier: Modifier = Modifier,
    /** Shows an Add a shop button, for a shop that OpenStreetMap does not have. */
    onAddShop: (() -> Unit)? = null
) {
    if (shops.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp).testTag("favourite_shops_empty"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                Glyphs.Heart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(56.dp).alpha(0.8f)
            )
            Text(text = "No favourite shops yet", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Open a shop and tap Add to favourites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onAddShop != null) AddShopButton(onAddShop)
        }
        return
    }

    val byTown = shops.groupBy { it.shop.address?.city?.takeIf { city -> city.isNotBlank() } ?: NO_TOWN }.toList()

    LazyColumn(modifier = modifier) {
        if (onAddShop != null) {
            item(key = "add_shop") {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { AddShopButton(onAddShop) }
            }
        }
        byTown.forEach { (town, inTown) ->
            item(key = "town_$town") {
                Text(
                    text = town,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        .testTag("favourite_shops_town_$town")
                )
            }
            items(inTown, key = { it.shop.id }) { favourite ->
                ShopRow(
                    shop = favourite.shop,
                    modifier = Modifier.testTag("favourite_shop_${favourite.shop.id}"),
                    onClick = { onShopSelected(favourite) }
                )
            }
        }
    }
}

@Composable
private fun AddShopButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.testTag("favourite_shops_add")) {
        Text("Add a shop")
    }
}
