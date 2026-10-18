package com.berkbb.channellogos

import com.berkbb.channellogos.logos.FileLogoSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileLogoSourceTest {

    @Test
    fun imagesAreOffered() {
        assertTrue(FileLogoSource.isLogoFile("CARTON NETWORK.png"))
        assertTrue(FileLogoSource.isLogoFile("show-tv.JPG"))
        assertTrue(FileLogoSource.isLogoFile("atv.jpeg"))
        assertTrue(FileLogoSource.isLogoFile("trt1.webp"))
    }

    @Test
    fun macMetadataAndHiddenFilesAreSkipped() {
        // macOS copies leave "._name" AppleDouble files whose key equals the real logo's.
        assertFalse(FileLogoSource.isLogoFile("._CARTON NETWORK.png"))
        assertFalse(FileLogoSource.isLogoFile(".hidden.png"))
    }

    @Test
    fun otherFilesAreSkipped() {
        assertFalse(FileLogoSource.isLogoFile("logo.svg"))
        assertFalse(FileLogoSource.isLogoFile("notes.pdf"))
        assertFalse(FileLogoSource.isLogoFile("png"))
    }

    @Test
    fun appDataBackupsAndHiddenFoldersAreNotSearched() {
        assertTrue(FileLogoSource.isBrowsedFolder("Download"))
        assertTrue(FileLogoSource.isBrowsedFolder("Logolar"))
        // "Android" holds apps' data, including the PNGs this app stages for writing.
        assertFalse(FileLogoSource.isBrowsedFolder("Android"))
        assertFalse(FileLogoSource.isBrowsedFolder("ChannelLogosBackup"))
        assertFalse(FileLogoSource.isBrowsedFolder("LOST.DIR"))
        assertFalse(FileLogoSource.isBrowsedFolder(".Spotlight-V100"))
    }

    @Test
    fun internalStorageIsToldApartFromSticks() {
        assertTrue(FileLogoSource.isInternal("/storage/emulated/0/Download/atv.png"))
        assertTrue(FileLogoSource.isInternal("/sdcard/atv.png"))
        assertFalse(FileLogoSource.isInternal("/storage/2AD6-1B0E/CARTON NETWORK.png"))
    }
}
