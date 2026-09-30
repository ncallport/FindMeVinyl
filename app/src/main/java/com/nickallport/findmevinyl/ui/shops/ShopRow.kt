package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory

@Composable
fun ShopRow(shop: Shop, modifier: Modifier = Modifier) {
    val categoryLabel = when (shop.category) {
        ShopCategory.RECORD_SHOP -> "Record Shop"
        ShopCategory.CHARITY_SHOP -> "Charity Shop"
        ShopCategory.SECOND_HAND_SHOP -> "Second-Hand Shop"
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = shop.name, style = MaterialTheme.typography.titleMedium)
        Text(text = categoryLabel, style = MaterialTheme.typography.bodySmall)
        Text(text = shop.openingHours ?: "Hours unknown", style = MaterialTheme.typography.bodyMedium)
    }
}