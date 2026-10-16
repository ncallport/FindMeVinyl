package com.nickallport.findmevinyl.shops

import java.net.URLEncoder
import java.util.Locale

object MapLinkBuilder {
    fun geoUri(shop: Shop): String {
        val position = String.format(Locale.US, "%.6f,%.6f", shop.latitude, shop.longitude)

        // Looking for the name and address near the position finds the shop even when the position is only the town's.
        shop.mapQuery?.takeIf { it.isNotBlank() }?.let { query ->
            return "geo:$position?q=" + URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        }

        val label = URLEncoder.encode(
            shop.name.replace("(", "").replace(")", "").trim(),
            "UTF-8"
        ).replace("+", "%20")

        return "geo:$position?q=$position($label)"
    }
}