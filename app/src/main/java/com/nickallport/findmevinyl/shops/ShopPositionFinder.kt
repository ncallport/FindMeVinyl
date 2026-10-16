package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import com.nickallport.findmevinyl.nominatim.NominatimApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Where an address is. [approximate] is true when only the town was found. [place] is what was matched. */
data class FoundPosition(val position: Coordinates, val approximate: Boolean, val place: String)

/**
 * Finds the position of an address with Nominatim (OpenStreetMap), for a shop that is not on the map. It asks for
 * the whole address, and if that is not known it asks for the town, and says the position is approximate. That is
 * at most two requests, a second apart, as Nominatim asks.
 */
class ShopPositionFinder(
    private val nominatim: NominatimApi,
    private val pauseMillis: Long = 1_100
) {
    suspend fun find(address: String): FoundPosition? {
        val parts = address.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        val whole = parts.joinToString(", ")
        search(whole)?.let { return FoundPosition(it, approximate = false, place = whole) }

        if (parts.size < 2) return null
        val town = parts.drop(1).joinToString(", ") { withoutPostcode(it) }.trim(' ', ',')
        if (town.isEmpty() || town == whole) return null
        delay(pauseMillis)
        return search(town)?.let { FoundPosition(it, approximate = true, place = town) }
    }

    private suspend fun search(query: String): Coordinates? = try {
        nominatim.search(query = query, limit = 1, addressDetails = 0).firstOrNull()?.let {
            val lat = it.lat.toDoubleOrNull()
            val lon = it.lon.toDoubleOrNull()
            if (lat != null && lon != null) Coordinates(lat, lon) else null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    // 2500, 1100-225 and the like are left out of the name of a town.
    private fun withoutPostcode(part: String): String =
        part.replace(Regex("""\b\d{4}(-\d{3})?\b"""), "").replace(Regex("\\s+"), " ").trim()
}
