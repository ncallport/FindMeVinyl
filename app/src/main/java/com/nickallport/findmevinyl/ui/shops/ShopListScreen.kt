package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopSorter

@Composable
fun ShopListScreen(
    shops: List<Shop>,
    fromLat: Double,
    fromLon: Double,
    modifier: Modifier = Modifier
) {
    val sortedShops = ShopSorter.sortByDistance(shops, fromLat, fromLon)

    LazyColumn(modifier = modifier) {
        items(sortedShops) { shop ->
            ShopRow(shop = shop, modifier = Modifier.testTag("shop_row"))
        }
    }
}