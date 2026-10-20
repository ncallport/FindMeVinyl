package com.nickallport.findmevinyl.barcode

/**
 * Reads a barcode number typed in or read from a photo: UPC-A (12 digits), EAN-13 or EAN-8, with a correct check digit,
 * so a mistyped number is caught before MusicBrainz is asked.
 */
object BarcodeNumber {
    private val LENGTHS = setOf(8, 12, 13)

    /** The digits of [text] when they are a valid barcode, with spaces and dashes taken out, otherwise null. */
    fun parse(text: String): String? {
        val digits = text.filterNot { it.isWhitespace() || it == '-' }
        if (digits.length !in LENGTHS || !digits.all { it in '0'..'9' }) return null
        return digits.takeIf { hasValidCheckDigit(it) }
    }

    /** The check digit of EAN-8, UPC-A and EAN-13 weights the digits 3, 1, 3, 1 ... counting from the one before the check digit. */
    private fun hasValidCheckDigit(digits: String): Boolean {
        val body = digits.dropLast(1)
        val sum = body.reversed().mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 3 else 1 }.sum()
        return (10 - sum % 10) % 10 == digits.last() - '0'
    }
}
