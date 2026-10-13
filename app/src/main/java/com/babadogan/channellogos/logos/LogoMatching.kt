package com.babadogan.channellogos.logos

/** Finds logos for a channel name in the sources' key → logo tables. */
object LogoMatching {

    /** The sources' key → logo tables. */
    class Catalogs(
        /** Images on a USB stick, by file name. */
        val files: Map<String, LogoEntry<String>> = emptyMap(),
        val tv: Map<String, LogoEntry<Int>> = emptyMap(),
        val gitHub: Map<String, LogoEntry<String>> = emptyMap(),
        /** picons by service name (a fallback: names from every satellite). */
        val picons: Map<String, LogoEntry<String>> = emptyMap(),
        /** picons by DVB service, "SID_TSID_ONID" → logo name; see Channel.serviceKey. */
        val piconServices: Map<String, String> = emptyMap(),
        val iptv: Map<String, LogoEntry<String>> = emptyMap()
    )

    /**
     * The best logo for [channelName] (broadcast as [serviceKey], if known). Ranked by catalogue
     * first (the TV's own country, then international, then others), then by how closely the name
     * matches, then by source: the user's own file, the TV's own logo (made for this screen),
     * GitHub, picons, iptv-org. A picon linked to the channel's own DVB service counts as the TV's
     * country.
     */
    fun best(channelName: String, catalogs: Catalogs, serviceKey: String? = null): LogoCandidate? =
        matches(channelName, catalogs, serviceKey).minWithOrNull(RANKING)

    /**
     * Whether [candidate] can be selected without a look: the user's own file, a logo from the
     * TV's own country in the TV's table or the curated GitHub collection whose name matches up
     * to "HD"/"SD", or a picon linked to the channel's DVB service whose name agrees. Anything else (foreign catalogues, iptv-org's community links, similar names,
     * drawn logos) waits for the user, since the same name can mean another channel elsewhere.
     */
    fun isSafe(candidate: LogoCandidate): Boolean =
        candidate.tier == LogoEntry.COUNTRY &&
            candidate.strength != NameMatcher.Strength.LOOSE &&
            candidate.source in SAFE_SOURCES

    /** A logo drawn from the channel's name, for channels no catalogue knows. */
    fun textLogo(channelName: String) =
        LogoCandidate(LogoSource.TEXT, channelName, NameMatcher.Strength.LOOSE, "text:$channelName", LogoEntry.OTHER)

    /**
     * Logos to offer when picking by hand: the matches above, then logos whose name starts with
     * the channel's base name or vice versa ("trt" ↔ "trt1", "trtspor"), closest first, and
     * finally a drawn logo.
     */
    fun candidates(channelName: String, catalogs: Catalogs, serviceKey: String? = null, limit: Int = 16): List<LogoCandidate> {
        val keys = NameMatcher.channelKeys(channelName)
        if (keys.isEmpty()) return emptyList()
        val result = LinkedHashMap<String, LogoCandidate>()
        matches(channelName, catalogs, serviceKey).sortedWith(RANKING).forEach { result.putIfAbsent(it.id, it) }
        val base = keys.first().first.let { NameMatcher.withoutQuality(it) ?: it }
        if (base.length >= 3) {
            val related = ArrayList<Pair<Int, LogoCandidate>>()
            for ((key, entry) in catalogs.files) {
                if (related(key, base)) related += distance(key, base) to linkCandidate(LogoSource.FILE, entry, NameMatcher.Strength.LOOSE)
            }
            for ((key, entry) in catalogs.tv) {
                if (related(key, base)) related += distance(key, base) to tvCandidate(key, entry, NameMatcher.Strength.LOOSE)
            }
            for ((key, entry) in catalogs.gitHub) {
                if (related(key, base)) related += distance(key, base) to linkCandidate(LogoSource.GITHUB, entry, NameMatcher.Strength.LOOSE)
            }
            for ((key, entry) in catalogs.picons) {
                if (related(key, base)) related += distance(key, base) to linkCandidate(LogoSource.PICONS, entry, NameMatcher.Strength.LOOSE)
            }
            for ((key, entry) in catalogs.iptv) {
                if (related(key, base)) related += distance(key, base) to linkCandidate(LogoSource.IPTV, entry, NameMatcher.Strength.LOOSE)
            }
            related.sortedWith(compareBy({ it.second.tier }, { it.first }, { it.second.source.ordinal }))
                .forEach { (_, candidate) -> result.putIfAbsent(candidate.id, candidate) }
        }
        return result.values.take(limit - 1) + textLogo(channelName)
    }

    // picons' name entries are never of the TV's country, so only service matches can be safe.
    private val SAFE_SOURCES = setOf(LogoSource.FILE, LogoSource.TV, LogoSource.GITHUB, LogoSource.PICONS)

    private val RANKING = compareBy<LogoCandidate>({ it.tier }, { it.strength.ordinal }, { it.source.ordinal })

    private fun matches(channelName: String, catalogs: Catalogs, serviceKey: String?): List<LogoCandidate> {
        val found = ArrayList<LogoCandidate>()
        val keys = NameMatcher.channelKeys(channelName)
        serviceKey?.let(catalogs.piconServices::get)?.let { logo ->
            // The broadcast IDs are exact, but services get reused by other channels: a picon
            // whose name has nothing in common with the channel's is offered, not trusted, and
            // ranks like a foreign catalogue's logo.
            val base = keys.firstOrNull()?.first?.let { NameMatcher.withoutQuality(it) ?: it }.orEmpty()
            val logoKey = NameMatcher.key(logo)
            val agrees = base.length >= 3 && (related(logoKey, base) || keys.any { it.first == logoKey })
            found += if (agrees) linkCandidate(LogoSource.PICONS, LogoEntry(logo, LogoEntry.COUNTRY), NameMatcher.Strength.EXACT)
            else linkCandidate(LogoSource.PICONS, LogoEntry(logo, LogoEntry.INTERNATIONAL), NameMatcher.Strength.LOOSE)
        }
        for ((key, strength) in keys) {
            catalogs.files[key]?.let { found += linkCandidate(LogoSource.FILE, it, strength) }
            catalogs.tv[key]?.let { found += tvCandidate(key, it, strength) }
            catalogs.gitHub[key]?.let { found += linkCandidate(LogoSource.GITHUB, it, strength) }
            catalogs.picons[key]?.let { found += linkCandidate(LogoSource.PICONS, it, strength) }
            catalogs.iptv[key]?.let { found += linkCandidate(LogoSource.IPTV, it, strength) }
        }
        return found
    }

    private fun tvCandidate(key: String, entry: LogoEntry<Int>, strength: NameMatcher.Strength) =
        LogoCandidate(LogoSource.TV, key, strength, "tv:${entry.value}", entry.tier)

    private fun linkCandidate(source: LogoSource, entry: LogoEntry<String>, strength: NameMatcher.Strength) =
        LogoCandidate(source, entry.value, strength, "${source.name.lowercase()}:${entry.value}", entry.tier)

    private fun distance(key: String, base: String) = kotlin.math.abs(key.length - base.length)

    private fun related(key: String, base: String) = key.startsWith(base) || (key.length >= 3 && base.startsWith(key))
}
