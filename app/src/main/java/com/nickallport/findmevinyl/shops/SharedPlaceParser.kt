package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import java.net.URI
import java.net.URLDecoder

/** What could be read from a place shared from Google Maps, or from a position pasted. Any part can be missing. */
data class SharedPlace(
    val name: String? = null,
    val address: String? = null,
    val link: String? = null,
    val position: Coordinates? = null
)

/**
 * Reads what Google Maps gives when a place is shared: its name on one line, its address on the next, and a short
 * link. A link has no position in it (it names the place and says where, in words), so a position comes only if
 * coordinates were pasted, or if the link is of a kind that has them. Nothing here asks the network for anything.
 */
object SharedPlaceParser {
    private val URL = Regex("""https?://\S+""")
    private val COORDINATES = Regex("""(?<![\d.])(-?\d{1,2}\.\d+)\s*[,;]\s*(-?\d{1,3}\.\d+)(?!\d)""")
    private val PLACE_POSITION = Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)""")
    private val VIEW_POSITION = Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")

    private val GOOGLE_MAPS_HOSTS = setOf(
        "www.google.com", "google.com", "maps.google.com", "www.google.co.uk", "www.google.pt", "www.google.es",
        "www.google.fr", "www.google.de", "www.google.it", "www.google.ie"
    )

    /** Splits text into a name, an address, a link and a position, each if it is there. */
    fun parse(text: String): SharedPlace {
        val link = URL.find(text)?.value?.trimEnd('.', ',', ')')
        val withoutLink = URL.replace(text, "\n")
        val position = coordinatesIn(withoutLink)

        val lines = withoutLink.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .filterNot { position != null && COORDINATES.containsMatchIn(it) }
        val fromText = SharedPlace(
            name = lines.firstOrNull(),
            address = lines.drop(1).joinToString(", ").ifEmpty { null },
            link = link,
            position = position
        )

        // A link to a place that is already a full address says the same things, so it fills what the text left out.
        val fromLink = link?.takeIf { isMapsLink(it) }?.let { fromPlaceUrl(it) }
        return SharedPlace(
            name = fromText.name ?: fromLink?.name,
            address = fromText.address ?: fromLink?.address,
            link = link,
            position = fromText.position ?: fromLink?.position
        )
    }

    /**
     * Reads a link that has been followed to the end: the name and address in its path (`/maps/place/Name,+Address/`),
     * and a position if it has one. A consent page is read for the link it is about. Null for anything else.
     */
    fun fromPlaceUrl(url: String): SharedPlace? {
        val parsed = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (parsed.host == "consent.google.com") {
            val next = queryParameter(parsed, "continue") ?: return null
            return fromPlaceUrl(next)
        }
        if (!isMapsLink(url)) return null

        val path = parsed.rawPath.orEmpty()
        var name: String? = null
        var address: String? = null
        val marker = "/maps/place/"
        if (path.contains(marker)) {
            val segment = path.substringAfter(marker).substringBefore('/')
            val decoded = runCatching { URLDecoder.decode(segment, "UTF-8") }.getOrNull().orEmpty().trim()
            if (decoded.isNotEmpty() && !decoded.startsWith("@")) {
                name = decoded.substringBefore(", ").trim().ifEmpty { null }
                address = decoded.substringAfter(", ", "").trim().ifEmpty { null }
            }
        }

        val position = positionIn(url, parsed)
        if (name == null && position == null) return null
        return SharedPlace(name = name, address = address, link = url, position = position)
    }

    /** Whether a link is one to follow: a Google Maps link on https, and nothing else. */
    fun isMapsLink(url: String): Boolean {
        val parsed = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (parsed.scheme != "https") return false
        val host = parsed.host?.lowercase() ?: return false
        val path = parsed.rawPath.orEmpty()
        return when {
            host == "maps.app.goo.gl" -> true
            host == "goo.gl" -> path.startsWith("/maps")
            host == "consent.google.com" -> true
            host in GOOGLE_MAPS_HOSTS -> host == "maps.google.com" || path.startsWith("/maps")
            else -> false
        }
    }

    private fun positionIn(url: String, parsed: URI): Coordinates? {
        PLACE_POSITION.find(url)?.let { return coordinates(it.groupValues[1], it.groupValues[2]) }
        VIEW_POSITION.find(url)?.let { return coordinates(it.groupValues[1], it.groupValues[2]) }
        for (key in listOf("q", "query", "ll", "destination")) {
            queryParameter(parsed, key)?.let { value -> coordinatesIn(value)?.let { return it } }
        }
        return null
    }

    private fun coordinatesIn(text: String): Coordinates? =
        COORDINATES.find(text)?.let { coordinates(it.groupValues[1], it.groupValues[2]) }

    private fun coordinates(latitude: String, longitude: String): Coordinates? {
        val lat = latitude.toDoubleOrNull() ?: return null
        val lon = longitude.toDoubleOrNull() ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) Coordinates(lat, lon) else null
    }

    private fun queryParameter(uri: URI, key: String): String? =
        uri.rawQuery?.split('&')?.mapNotNull { pair ->
            val name = pair.substringBefore('=')
            if (name == key) runCatching { URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") }.getOrNull() else null
        }?.firstOrNull()
}
