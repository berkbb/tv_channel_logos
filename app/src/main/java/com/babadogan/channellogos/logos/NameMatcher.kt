package com.babadogan.channellogos.logos

import java.text.Normalizer
import java.util.Locale

/**
 * Channel names and logo names are compared as keys: lowercase ASCII letters and digits only.
 * "STAR TV HD", "Star-TV" and "startv" all become "startvhd" / "startv".
 */
object NameMatcher {

    private val QUALITY_SUFFIXES = listOf("uhd", "fhd", "hd", "sd", "4k")
    private val MARKS = Regex("\\p{Mn}+")
    /** Non-ASCII character → its ASCII letters/digits ("ç" → "c", "ß" → "ss", "€" → ""). */
    private val foldCache = java.util.concurrent.ConcurrentHashMap<Char, String>()
    // "TİCARİ" marks the commercial (bar/café) feed of a channel; it carries the channel's logo.
    // Explicit letter/digit look-arounds: "\\b" is ASCII-only on newer JVMs and would not end after "İ".
    private val COMMERCIAL = Regex("(?i)(?<![\\p{L}\\p{N}])t[iİı]car[iİı](?![\\p{L}\\p{N}])")

    /** How close a candidate key is to the channel name; lower is better. */
    enum class Strength { EXACT, WITHOUT_QUALITY, LOOSE }

    /**
     * Lowercase ASCII letters and digits of [name]; accents are dropped and every Turkish i
     * (I, İ, ı, i) becomes "i". A plain character loop: Normalizer and regular expressions cost
     * about a millisecond per name on TV processors, and the TV's logo table has ~10,000 names.
     */
    fun key(name: String): String {
        val out = StringBuilder(name.length)
        for (c in name) {
            when {
                c in 'a'..'z' || c in '0'..'9' -> out.append(c)
                c in 'A'..'Z' -> out.append(c + 32)
                c == '&' -> out.append("and")
                c == '+' -> out.append("plus")
                c == 'İ' || c == 'ı' -> out.append('i')
                c.code < 128 -> Unit // spaces, punctuation
                else -> out.append(foldCache.getOrPut(c) { fold(c) })
            }
        }
        return out.toString()
    }

    private fun fold(c: Char): String {
        val stripped = MARKS.replace(Normalizer.normalize(c.toString().lowercase(Locale.ROOT), Normalizer.Form.NFD), "")
        return buildString { for (ch in stripped) if (ch in 'a'..'z' || ch in '0'..'9') append(ch) }
    }

    /** [key] without a trailing quality marker ("trt1hd" → "trt1"), or null if it has none. */
    fun withoutQuality(key: String): String? {
        for (suffix in QUALITY_SUFFIXES) {
            if (key.length > suffix.length + 1 && key.endsWith(suffix)) return key.removeSuffix(suffix)
        }
        return null
    }

    /**
     * Keys a channel name is looked up by, best first: the exact key, the key without its quality
     * marker, then that key with or without a trailing "tv" ("show" ↔ "showtv").
     */
    fun channelKeys(name: String): List<Pair<String, Strength>> {
        val exact = key(COMMERCIAL.replace(name, " "))
        if (exact.isEmpty()) return emptyList()
        val keys = LinkedHashMap<String, Strength>()
        keys[exact] = Strength.EXACT
        val base = withoutQuality(exact)
        base?.let { keys.putIfAbsent(it, Strength.WITHOUT_QUALITY) }
        val core = base ?: exact
        val loose = if (core.endsWith("tv") && core.length > 4) core.removeSuffix("tv") else core + "tv"
        keys.putIfAbsent(loose, Strength.LOOSE)
        return keys.map { it.key to it.value }
    }

    /**
     * Keys a logo is filed under: its own key and, for a name ending in a quality marker, the key
     * without it, so channel "beIN MOVIES PREMIERE" finds logo "bein-movies-premiere-hd".
     */
    fun logoKeys(logoName: String): List<String> {
        val key = key(logoName)
        if (key.isEmpty()) return emptyList()
        return listOfNotNull(key, withoutQuality(key))
    }

    /**
     * Key of a tv-logos file name: "star-tv-tr.png" → "startv", "trt-1-hd-tr.png" → "trt1hd".
     * Returns null for files that are not plain logos (horizontal "-hz" variants, the mosaic).
     */
    fun gitHubKey(fileName: String, countrySuffix: String): String? {
        if (!fileName.endsWith(".png")) return null
        val stem = fileName.removeSuffix(".png").removeSuffix("-$countrySuffix")
        if (stem.endsWith("-hz")) return null
        return key(stem).ifEmpty { null }
    }
}
