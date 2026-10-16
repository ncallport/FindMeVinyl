package com.nickallport.findmevinyl.shops

/**
 * Turns what OpenStreetMap says into something the phone may be asked to open. The text comes from volunteers
 * and is not trusted: only an ordinary web address, a phone number and an email address pass, so a shop's
 * page cannot send the user to a link that does anything else.
 */
object WebLink {
    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    private val WEB_ADDRESS = Regex("^https?://[^\\s/]+\\S*$", RegexOption.IGNORE_CASE)
    private val BARE_HOST = Regex("^[^\\s/:]+\\.[^\\s/:]+(/\\S*)?$")

    /** An http or https address to open, or null. An address with no scheme is taken to be https. */
    fun normalise(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        return when {
            SCHEME.containsMatchIn(text) -> text.takeIf { WEB_ADDRESS.matches(it) }
            BARE_HOST.matches(text) -> "https://$text"
            else -> null
        }
    }
}

object PhoneLink {
    private val ALLOWED = Regex("^[+()\\d\\s.\\-/]+$")

    /** A `tel:` link for the first number in [raw], with only its digits and a leading plus, or null. */
    fun dialUri(raw: String?): String? {
        val first = raw?.split(';', ',')?.firstOrNull()?.trim().orEmpty()
        if (first.isEmpty() || !ALLOWED.matches(first)) return null
        val digits = first.filter { it.isDigit() }
        if (digits.length < MIN_DIGITS) return null
        return "tel:" + (if (first.startsWith("+")) "+" else "") + digits
    }

    private const val MIN_DIGITS = 5
}

object EmailLink {
    private val ADDRESS = Regex("^[^\\s@<>\",;:]+@[^\\s@<>\",;:]+\\.[^\\s@<>\",;:]+$")

    /** A `mailto:` link, or null when [raw] is not a plain email address. */
    fun mailUri(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        return if (ADDRESS.matches(text)) "mailto:$text" else null
    }
}
