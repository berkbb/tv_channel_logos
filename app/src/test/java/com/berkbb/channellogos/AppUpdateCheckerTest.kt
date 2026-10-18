package com.berkbb.channellogos

import com.berkbb.channellogos.ui.AppUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun isNewerVersionComparesCorrectly() {
        assertTrue(AppUpdateChecker.isNewerVersion("1.3.1", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.4.0", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0.0", "1.3.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.10.0", "1.9.0"))

        assertFalse(AppUpdateChecker.isNewerVersion("1.3.0", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.2.9", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.2.0", "1.3.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("", "1.3.0"))
    }
}
