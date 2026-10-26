package com.berkbb.channellogos.ui

import com.berkbb.channellogos.logos.LogoCandidate
import com.berkbb.channellogos.tv.Channel

/**
 * The channel list as CSV for a spreadsheet: a header, then one row of quoted cells per channel
 * with its suggested logo. Channel names come from broadcasters, so a cell a spreadsheet would run
 * as a formula ("=HYPERLINK(…)", "+1", "-2", "@SUM(…)") is prefixed with ' and stays plain text.
 */
object ChannelCsv {

    const val HEADER = "Number,Name,Type,HasLogo,SuggestedLogo,SuggestedSource,Strength"

    fun build(channels: List<Channel>, suggestions: Map<Long, LogoCandidate>): String = buildString {
        append(HEADER).append('\n')
        for (channel in channels) {
            val suggestion = suggestions[channel.id]
            listOf(
                channel.number,
                channel.name,
                if (channel.isRadio) "Radio" else "TV",
                if (channel.hasLogo) "Yes" else "No",
                suggestion?.name.orEmpty(),
                suggestion?.source?.name.orEmpty(),
                suggestion?.strength?.name.orEmpty()
            ).joinTo(this, ",", postfix = "\n", transform = ::cell)
        }
    }

    /** [value] as a quoted cell; a leading formula character is neutralised with '. */
    fun cell(value: String): String {
        val text = if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value
        return "\"" + text.replace("\"", "\"\"") + "\""
    }

    // Tab and carriage return also start formulas in some spreadsheets (OWASP CSV injection).
    private const val FORMULA_STARTS = "=+-@\t\r"
}
