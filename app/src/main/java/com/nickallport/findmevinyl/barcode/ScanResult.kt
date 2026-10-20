package com.nickallport.findmevinyl.barcode

/** What came of asking the user to photograph a barcode. */
sealed class ScanResult {
    /** A barcode number was read from the photo. */
    data class Number(val text: String) : ScanResult()
    /** A photo was taken but no barcode could be read from it. */
    object Unreadable : ScanResult()
    /** The user backed out of the camera. */
    object Cancelled : ScanResult()
    /** There is no camera app to take the photo. */
    object NoCamera : ScanResult()
}
