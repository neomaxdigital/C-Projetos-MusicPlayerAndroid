package com.musicplayer.offline.music

object AudioFileSupport {
    val recognizedMimeTypes = listOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/wav",
        "audio/x-wav",
        "audio/wave",
        "audio/vnd.wave"
    )

    fun title(metadataTitle: String?, displayName: String): String {
        val fallback = displayName.substringBeforeLast('.', displayName).ifBlank { "Sem título" }
        return normalized(metadataTitle) ?: fallback
    }

    fun artist(metadataArtist: String?): String = normalized(metadataArtist) ?: UNKNOWN_ARTIST

    fun album(metadataAlbum: String?): String = normalized(metadataAlbum) ?: UNKNOWN_ALBUM

    fun mimeType(mediaStoreMimeType: String?, displayName: String): String =
        normalized(mediaStoreMimeType) ?: when (displayName.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> ""
        }

    fun isSupported(mimeType: String?, displayName: String, uri: String = ""): Boolean {
        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val uriExtension = uri.substringBefore('?').substringAfterLast('.', "").lowercase()
        return normalizedMime in recognizedMimeTypes || extension in setOf("mp3", "wav") || uriExtension in setOf("mp3", "wav")
    }

    fun isWav(mimeType: String?, displayName: String, uri: String = ""): Boolean {
        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val uriExtension = uri.substringBefore('?').substringAfterLast('.', "").lowercase()
        return normalizedMime in setOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave") ||
            extension == "wav" || uriExtension == "wav"
    }

    fun isWav(song: Song): Boolean = isWav(song.mimeType, song.displayName, song.uri.toString())

    fun isUnsupportedCodecError(errorCode: Int): Boolean =
        errorCode in 3_001..3_004 || errorCode in 4_001..4_005

    private fun normalized(value: String?): String? = value
        ?.trim()
        ?.takeUnless { it.isBlank() || it.equals("<unknown>", ignoreCase = true) }
}
