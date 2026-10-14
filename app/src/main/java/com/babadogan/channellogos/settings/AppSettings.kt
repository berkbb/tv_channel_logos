package com.babadogan.channellogos.settings

import android.content.Context
import androidx.core.content.edit

/** The user's choices, kept across launches. */
data class Settings(
    /** "system", "tr", "en" or "ro". */
    val language: String = LANGUAGE_SYSTEM,
    /** "system", "dark" or "light". */
    val theme: String = THEME_SYSTEM,
    /** Preselect safe matches after a scan (otherwise nothing is preselected). */
    val preselectSafe: Boolean = true,
    /** Offer a logo drawn from the name for channels no catalogue knows. */
    val textLogos: Boolean = true,
    val gitHub: Boolean = true,
    val picons: Boolean = true,
    val iptv: Boolean = true,
    val usb: Boolean = true
) {
    /** The settings as "key" → "value", for a backup file; [fromMap] reads them back. */
    fun toMap(): Map<String, String> = mapOf(
        "language" to language,
        "theme" to theme,
        "preselect_safe" to preselectSafe.toString(),
        "text_logos" to textLogos.toString(),
        "source_github" to gitHub.toString(),
        "source_picons" to picons.toString(),
        "source_iptv" to iptv.toString(),
        "source_usb" to usb.toString()
    )

    companion object {
        /** Settings from [toMap]'s keys; missing or unknown values keep the defaults. */
        fun fromMap(map: Map<String, String>): Settings {
            val defaults = Settings()
            fun flag(key: String, default: Boolean) = map[key]?.toBooleanStrictOrNull() ?: default
            return Settings(
                language = map["language"]?.takeIf { it == LANGUAGE_SYSTEM || it in listOf("tr", "en", "ro") } ?: defaults.language,
                theme = map["theme"]?.takeIf { it in listOf(THEME_SYSTEM, THEME_DARK, THEME_LIGHT) } ?: defaults.theme,
                preselectSafe = flag("preselect_safe", defaults.preselectSafe),
                textLogos = flag("text_logos", defaults.textLogos),
                gitHub = flag("source_github", defaults.gitHub),
                picons = flag("source_picons", defaults.picons),
                iptv = flag("source_iptv", defaults.iptv),
                usb = flag("source_usb", defaults.usb)
            )
        }

        const val LANGUAGE_SYSTEM = "system"
        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = Settings(
        language = prefs.getString("language", Settings.LANGUAGE_SYSTEM) ?: Settings.LANGUAGE_SYSTEM,
        theme = prefs.getString("theme", Settings.THEME_SYSTEM) ?: Settings.THEME_SYSTEM,
        preselectSafe = prefs.getBoolean("preselect_safe", true),
        textLogos = prefs.getBoolean("text_logos", true),
        gitHub = prefs.getBoolean("source_github", true),
        picons = prefs.getBoolean("source_picons", true),
        iptv = prefs.getBoolean("source_iptv", true),
        usb = prefs.getBoolean("source_usb", true)
    )

    fun save(settings: Settings) = prefs.edit {
        putString("language", settings.language)
        putString("theme", settings.theme)
        putBoolean("preselect_safe", settings.preselectSafe)
        putBoolean("text_logos", settings.textLogos)
        putBoolean("source_github", settings.gitHub)
        putBoolean("source_picons", settings.picons)
        putBoolean("source_iptv", settings.iptv)
        putBoolean("source_usb", settings.usb)
    }
}
