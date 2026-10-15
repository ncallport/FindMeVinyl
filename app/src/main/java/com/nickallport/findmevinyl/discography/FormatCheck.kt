package com.nickallport.findmevinyl.discography

/** How checking the formats of all of an artist's albums at once ended. */
sealed class FormatCheckResult {
    /** Every page was read. [updated] is how many saved albums got their formats. */
    data class Done(val updated: Int) : FormatCheckResult()

    /** The artist has too many releases to read page by page, so the albums are looked up one at a time as before. */
    object TooBig : FormatCheckResult()

    /** A page could not be had. Nothing was saved, and trying again carries on from that page. */
    object Failed : FormatCheckResult()
}
