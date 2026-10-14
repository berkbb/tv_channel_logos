package com.babadogan.channellogos.logos

/**
 * Flags logos that tend to look poor on the TV, judged from the downloaded image before it is
 * fitted into the TV's 136×104 box. Pure pixel arithmetic (ARGB ints), so it is unit-tested on the
 * JVM; [LogoImage.quality] feeds it from a decoded bitmap.
 */
object LogoQuality {

    enum class Issue {
        /** Much smaller than the TV's box (e.g. 52×39 thumbnails): enlarged, it turns blurry. */
        SMALL,
        /** Dark artwork on a transparent background: nearly invisible on the TV's dark channel bar. */
        DARK_ON_TRANSPARENT
    }

    /** Longest side below this is too small; the TV's box is 136 px wide. */
    const val MIN_SIDE = 100

    fun assess(width: Int, height: Int, argb: IntArray): Set<Issue> {
        val issues = HashSet<Issue>()
        if (maxOf(width, height) < MIN_SIDE) issues += Issue.SMALL
        var transparent = 0
        var opaque = 0
        var luminance = 0.0
        for (pixel in argb) {
            val alpha = pixel ushr 24
            if (alpha < 64) {
                transparent++
                continue
            }
            opaque++
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            luminance += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
        }
        val total = transparent + opaque
        if (total > 0 && opaque > 0 &&
            transparent.toDouble() / total >= TRANSPARENT_SHARE &&
            luminance / opaque < DARK_LUMINANCE
        ) {
            issues += Issue.DARK_ON_TRANSPARENT
        }
        return issues
    }

    /** At least this share of the image is see-through... */
    private const val TRANSPARENT_SHARE = 0.25
    /** ...and its visible pixels are on average this dark (0 black, 1 white). */
    private const val DARK_LUMINANCE = 0.3
}
