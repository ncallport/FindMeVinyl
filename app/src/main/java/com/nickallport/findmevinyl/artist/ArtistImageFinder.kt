package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.wikidata.WikidataApi
import java.net.URLDecoder
import java.net.URLEncoder

/** The two links of an artist that can lead to a picture. Either can be missing. */
data class ArtistLinks(val wikidataId: String?, val commonsFile: String?)

/**
 * Finds the address of a picture of an artist, in two steps so a caller can treat them differently.
 * [fetchLinks] is the MusicBrainz request, which has to wait its turn in the MusicBrainz queue.
 * [pictureUrl] is the Wikidata request, which has no such limit.
 *
 * MusicBrainz links an artist to its Wikidata item, and Wikidata gives the item's image as a file on
 * Wikimedia Commons. That is an exact match, where searching by name could pick the wrong artist.
 * Some artists only have an "image" link on MusicBrainz, which is used when Wikidata has none.
 * The picture itself is not downloaded here. The address asks Commons for a thumbnail.
 */
class ArtistImageFinder(
    private val musicBrainz: MusicBrainzApi,
    private val wikidata: WikidataApi,
    private val commonsBaseUrl: String = "https://commons.wikimedia.org",
    private val width: Int = 120
) {
    suspend fun fetchLinks(artistId: String): ArtistLinks {
        val relations = musicBrainz.getArtistLinks(artistId = artistId).relations.orEmpty()
        val wikidataId = relations
            .firstNotNullOfOrNull { if (it.type == "wikidata") it.url?.resource?.let(::wikidataItem) else null }
        val commonsFile = relations
            .firstNotNullOfOrNull { if (it.type == "image") it.url?.resource?.let(::commonsFileName) else null }
        return ArtistLinks(wikidataId, commonsFile)
    }

    /** The address of the picture, or null when there is none. */
    suspend fun pictureUrl(links: ArtistLinks): String? {
        val fromWikidata = links.wikidataId?.let { item ->
            wikidata.getImageClaims(entityId = item).claims?.get("P18").orEmpty()
                .firstOrNull { it.rank != "deprecated" && it.fileName() != null }
                ?.fileName()
        }
        val file = fromWikidata ?: links.commonsFile ?: return null
        val encoded = URLEncoder.encode(file, "UTF-8").replace("+", "%20")
        return "$commonsBaseUrl/wiki/Special:FilePath/$encoded?width=$width"
    }

    // Only an item page ("/wiki/Q151012") counts. Other Wikidata pages, such as properties, do not.
    private fun wikidataItem(resource: String): String? =
        wikidataItemPage.find(resource)?.groupValues?.get(1)

    // Only a Commons file page counts. "Image" links to other sites are ignored.
    private fun commonsFileName(resource: String): String? =
        commonsFilePage.find(resource)?.groupValues?.get(1)?.let {
            URLDecoder.decode(it.replace("+", "%2B"), "UTF-8")
        }

    companion object {
        /** [url], an address [pictureUrl] made, asked for at [width] pixels instead. An address with no width is kept. */
        fun atWidth(url: String, width: Int): String = url.replace(Regex("""([?&])width=\d+"""), "$1width=$width")

        private val wikidataItemPage = Regex("""^https?://(?:www\.)?wikidata\.org/wiki/(Q\d+)/?$""")
        private val commonsFilePage = Regex("""^https?://commons\.wikimedia\.org/wiki/File:(.+)$""")
    }
}
