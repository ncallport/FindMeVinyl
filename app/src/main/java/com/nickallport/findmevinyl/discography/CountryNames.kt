package com.nickallport.findmevinyl.discography

import java.util.Locale

object CountryNames {
    // MusicBrainz's codes for a release that was not made for one country.
    private val special = mapOf("XW" to "Worldwide", "XE" to "Europe")
    private val isoCodes = Locale.getISOCountries().toSet()

    fun of(code: String): String {
        val upper = code.uppercase()
        special[upper]?.let { return it }
        // Java names an unknown code "Unknown Region", so only real country codes are translated.
        if (upper !in isoCodes) return code
        return Locale.Builder().setRegion(upper).build().getDisplayCountry(Locale.ENGLISH).ifBlank { code }
    }
}
