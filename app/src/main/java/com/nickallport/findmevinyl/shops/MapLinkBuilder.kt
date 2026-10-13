package com.nickallport.findmevinyl.shops

import java.net.URLEncoder
import java.util.Locale

object MapLinkBuilder {
    fun geoUri(shop: Shop): String {
        val position = String.format(Locale.US, "%.6f,%.6f", shop.latitude, shop.longitude)
        val label = URLEncoder.encode(
            shop.name.replace("(", "").replace(")", "").trim(),
            "UTF-8"
        ).replace("+", "%20")

        return "geo:$position?q=$position($label)"
    }
}