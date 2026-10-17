package com.nickallport.findmevinyl.photon

import kotlinx.coroutines.CancellationException

/**
 * Suggests places for what the user has typed so far. It asks for nothing before three letters, and
 * keeps each answer for the run of the app, so typing a word again (or backspacing) costs no request.
 * A failure gives no suggestions and is not kept: suggestions are a help, and searching still works.
 */
class PlaceSuggester(private val api: PhotonApi) {
    private val answers = mutableMapOf<String, List<PlaceSuggestion>>()

    suspend fun suggest(text: String): List<PlaceSuggestion> {
        val query = text.trim()
        if (query.count { !it.isWhitespace() } < MIN_LETTERS) return emptyList()
        val key = query.lowercase()
        answers[key]?.let { return it }
        return try {
            PhotonMapper.suggestions(api.search(query)).also { answers[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val MIN_LETTERS = 3
    }
}
