package com.babadogan.channellogos.logos

/**
 * Whether the TV carries channels of another country (Romanian channels on a Turkish TV), so that
 * country's catalogues are worth adding. Only names that country's catalogue knows exactly and the
 * TV's own country does not count: "Kanal D" exists in both and proves nothing.
 */
object ForeignChannels {

    /** At least this many distinct channels must match before a country is added. */
    const val MIN_CHANNELS = 3

    fun carries(channelNames: List<String>, foreignKeys: Set<String>, ownKeys: Set<String>): Boolean =
        channelNames.asSequence()
            .mapNotNull { name -> NameMatcher.channelKeys(name).firstOrNull()?.first }
            .map { key -> NameMatcher.withoutQuality(key) ?: key }
            .filter { it in foreignKeys && it !in ownKeys }
            .distinct()
            .take(MIN_CHANNELS)
            .count() >= MIN_CHANNELS
}
