package com.nickallport.findmevinyl.artist

import java.io.File

/** The folder where the photos the user chose for artists are kept, one file for each. */
class ArtistPhotoStore(private val directory: File) {

    /** Writes the file, making the folder first. Throws when it cannot. */
    fun write(fileName: String, bytes: ByteArray) {
        directory.mkdirs()
        File(directory, fileName).writeBytes(bytes)
    }

    /** The file's bytes, or null when there is no such file or it cannot be read. */
    fun read(fileName: String): ByteArray? = try {
        File(directory, fileName).takeIf { it.isFile }?.readBytes()
    } catch (e: Exception) {
        null
    }

    fun delete(fileName: String) {
        File(directory, fileName).delete()
    }
}
