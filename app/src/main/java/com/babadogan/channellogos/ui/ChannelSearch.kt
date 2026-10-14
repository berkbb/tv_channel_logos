package com.babadogan.channellogos.ui

import com.babadogan.channellogos.logos.NameMatcher
import com.babadogan.channellogos.tv.Channel

/**
 * Type-to-filter search over the channel list. Names are compared as [NameMatcher.key]s, so case,
 * accents, Turkish i's and punctuation do not matter ("trt 1" finds "TRT 1 HD", "cnn turk" finds
 * "CNN TÜRK"). Every word of the query must appear in the name; a query of digits also finds the
 * channels whose number starts with it.
 */
object ChannelSearch {

    fun matches(channel: Channel, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.all { it.isDigit() } && channel.number.startsWith(trimmed)) return true
        val words = trimmed.split(WHITESPACE).map(NameMatcher::key).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val name = NameMatcher.key(channel.name)
        return words.all { it in name }
    }

    fun filter(channels: List<Channel>, query: String): List<Channel> =
        if (query.isBlank()) channels else channels.filter { matches(it, query) }

    private val WHITESPACE = Regex("\\s+")
}
