package com.berkbb.channellogos

import com.berkbb.channellogos.logos.LogoQuality
import com.berkbb.channellogos.logos.LogoQuality.Issue
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoQualityTest {

    private fun image(width: Int, height: Int, pixel: (x: Int, y: Int) -> Int) =
        IntArray(width * height) { pixel(it % width, it / width) }

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val clear = 0x00000000

    @Test
    fun aLargeOpaqueLogoIsFine() {
        assertEquals(emptySet<Issue>(), LogoQuality.assess(200, 150, image(200, 150) { _, _ -> white }))
    }

    @Test
    fun thumbnailsAreSmall() {
        assertEquals(setOf(Issue.SMALL), LogoQuality.assess(52, 39, image(52, 39) { _, _ -> white }))
    }

    @Test
    fun darkTextOnTransparencyIsFlagged() {
        // Black strokes in the middle third, the rest see-through.
        val pixels = image(300, 100) { _, y -> if (y in 33..66) black else clear }
        assertEquals(setOf(Issue.DARK_ON_TRANSPARENT), LogoQuality.assess(300, 100, pixels))
    }

    @Test
    fun lightArtOnTransparencyAndDarkArtOnATileAreFine() {
        val light = image(300, 100) { _, y -> if (y in 33..66) white else clear }
        assertEquals(emptySet<Issue>(), LogoQuality.assess(300, 100, light))
        // Dark text on its own opaque white tile shows on any background.
        val tiled = image(300, 100) { x, _ -> if (x in 100..200) black else white }
        assertEquals(emptySet<Issue>(), LogoQuality.assess(300, 100, tiled))
    }
}
