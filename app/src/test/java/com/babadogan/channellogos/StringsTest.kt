package com.babadogan.channellogos

import com.babadogan.channellogos.settings.Settings
import com.babadogan.channellogos.ui.Strings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class StringsTest {

    @After
    fun reset() = Strings.setLanguage(Settings.LANGUAGE_SYSTEM)

    /** Every text property, read in [language]. */
    private fun texts(language: String): Map<String, String> {
        Strings.setLanguage(language)
        return Strings::class.java.declaredMethods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == String::class.java && Modifier.isPublic(it.modifiers) }
            .associate { it.name to it.invoke(Strings) as String }
    }

    @Test
    fun everyTextExistsInBothLanguages() {
        val turkish = texts("tr")
        val english = texts("en")
        assertTrue("expected many texts, found ${turkish.size}", turkish.size > 50)
        assertEquals(turkish.keys, english.keys)
        for ((name, text) in turkish) {
            assertTrue("$name is blank in Turkish", text.isNotBlank())
            assertTrue("$name is blank in English", english.getValue(name).isNotBlank())
        }
        // Proper names aside, the two languages really differ
        val same = turkish.filter { (name, text) -> english[name] == text }.keys
        assertEquals(setOf("getSourceTv", "getSourceGitHub", "getSourceIptv", "getSourceFile", "getAboutCompany"), same)
    }

    @Test
    fun parameterizedTextsCarryTheirValues() {
        for (language in listOf("tr", "en")) {
            Strings.setLanguage(language)
            assertTrue(Strings.channelStats(589, 85, 194).contains("589"))
            assertTrue(Strings.apply(12).contains("12"))
            assertTrue(Strings.written(3, 1).contains("3"))
            assertTrue(Strings.scanFailed("boom").contains("boom"))
            assertTrue(Strings.version("1.0.0").contains("1.0.0"))
        }
        Strings.setLanguage("tr")
        val turkish = Strings.apply(1)
        Strings.setLanguage("en")
        assertNotEquals(turkish, Strings.apply(1))
    }
}
