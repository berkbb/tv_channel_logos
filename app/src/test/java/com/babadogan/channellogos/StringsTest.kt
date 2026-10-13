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
    fun everyTextExistsInEveryLanguage() {
        val byLanguage = Strings.LANGUAGES.associateWith { texts(it) }
        val english = byLanguage.getValue("en")
        assertTrue("expected many texts, found ${english.size}", english.size > 50)
        for ((language, texts) in byLanguage) {
            assertEquals(english.keys, texts.keys)
            for ((name, text) in texts) assertTrue("$name is blank in $language", text.isNotBlank())
        }
        // Proper names and words the languages share aside, every two languages really differ
        val properNames = setOf("getSourceTv", "getSourceGitHub", "getSourcePicons", "getSourceIptv", "getSourceFile", "getAboutCompany")
        val sharedWords = mapOf(("ro" to "tr") to setOf("getLanguageSystem")) // "Sistem"
        for (a in Strings.LANGUAGES) for (b in Strings.LANGUAGES) {
            if (a >= b) continue
            val same = byLanguage.getValue(a).filter { (name, text) -> byLanguage.getValue(b)[name] == text }.keys -
                properNames - sharedWords[a to b].orEmpty()
            assertEquals("same text in $a and $b", emptySet<String>(), same)
        }
    }

    @Test
    fun anUnknownLanguageFallsBackToEnglish() {
        Strings.setLanguage("de")
        val german = Strings.settings
        Strings.setLanguage("en")
        assertEquals(Strings.settings, german)
    }

    @Test
    fun parameterizedTextsCarryTheirValues() {
        for (language in Strings.LANGUAGES) {
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
