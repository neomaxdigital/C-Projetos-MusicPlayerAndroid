package com.musicplayer.offline.lyrics

data class LyricLine(val timeMs: Long?, val text: String)
data class LyricsDocument(val lines: List<LyricLine>) {
    val synchronized: Boolean get() = lines.any { it.timeMs != null }
}

object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val metadata = Regex("^\\[(ar|ti|al|by|offset|re|ve):.*]$", RegexOption.IGNORE_CASE)

    fun parse(raw: String): LyricsDocument {
        val parsed = buildList {
            raw.lineSequence().forEach { source ->
                val line = source.trimEnd()
                val matches = timestamp.findAll(line).toList()
                if (matches.isNotEmpty()) {
                    val text = timestamp.replace(line, "").trim()
                    matches.forEach { match ->
                        val minutes = match.groupValues[1].toLong()
                        val seconds = match.groupValues[2].toLong()
                        val fraction = match.groupValues[3]
                        val millis = when (fraction.length) { 1 -> fraction.toLong() * 100; 2 -> fraction.toLong() * 10; 3 -> fraction.toLong(); else -> 0 }
                        add(LyricLine(minutes * 60_000 + seconds * 1_000 + millis, text))
                    }
                } else if (line.isNotBlank() && !metadata.matches(line)) add(LyricLine(null, line.trim()))
            }
        }
        val synchronized = parsed.filter { it.timeMs != null }.sortedBy { it.timeMs }
        return LyricsDocument(if (synchronized.isNotEmpty()) synchronized else parsed)
    }
}
