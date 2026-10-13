package com.babadogan.channellogos.logos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.JsonReader
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipFile

enum class LogoSource {
    /** An image on a USB stick, put there by the user: preferred over every catalogue. */
    FILE,
    /** Built into the TV's firmware. */
    TV,
    /** github.com/tv-logo/tv-logos */
    GITHUB,
    /** github.com/picons/picons: logos linked to DVB services and service names */
    PICONS,
    /** iptv-org's channel database (logo links to many hosts) */
    IPTV,
    /** Drawn by this app from the channel name, when no logo exists anywhere */
    TEXT
}

/** A logo that could be assigned to a channel. */
class LogoCandidate(
    val source: LogoSource,
    /** The name the source knows the logo by. */
    val name: String,
    val strength: NameMatcher.Strength,
    /** Stable id for caching ("tv:5209", "github:turkey/atv-tr.png"). */
    val id: String,
    /** Where the logo is filed; see [LogoEntry.tier]. */
    val tier: Int = LogoEntry.COUNTRY
)

/**
 * A logo filed under a name key. [tier] says how close its catalogue is to the TV's country:
 * the same name means different channels in different countries (the international "ShowHD"
 * is Discovery Showcase, Turkey's "Show TV" is another channel), so nearer catalogues win.
 */
data class LogoEntry<T>(val value: T, val tier: Int) {
    companion object {
        const val COUNTRY = 0
        const val INTERNATIONAL = 1
        const val OTHER = 2
    }
}

/** Country the TV's channels belong to, in the forms both sources use. */
class Country(val isoCode: String) {
    /** English name, as used by the TV's logo table ("Turkey") and by tv-logos ("turkey"). */
    val englishName: String = Locale("", isoCode).getDisplayCountry(Locale.ENGLISH)
        .let { if (it == "Türkiye") "Turkey" else it }
    val gitHubFolder: String = englishName.lowercase(Locale.ROOT).replace(' ', '-')
    val gitHubSuffix: String = isoCode.lowercase(Locale.ROOT)

    companion object {
        fun ofDevice(): Country = Country(Locale.getDefault().country.ifEmpty { "TR" })
    }
}

/**
 * The logos built into Philips (TP Vision) TV firmware: PNGs and a name → logo table inside the
 * system app that fills in channel logos. Read-only; the app reads it straight from the APK.
 */
class BuiltInLogoSource(private val context: Context) {

    private var apk: File? = null
    private val byKey = HashMap<String, LogoEntry<Int>>()

    val available: Boolean get() = apk != null
    val size: Int get() = byKey.size

    /**
     * Loads the table: [country] first, then "international", then every other country as a
     * fallback (foreign channels on the same satellite, e.g. Russian ones on Türksat).
     */
    fun load(country: Country) {
        val file = findApk() ?: return
        apk = file
        // Building the keys takes seconds on a TV; the result is kept until the firmware changes.
        val index = File(context.filesDir, "tv-logo-index-${country.isoCode}-${file.length()}-${file.lastModified()}.tsv")
        if (index.exists() && readIndex(index)) return
        byKey.clear()
        build(file, country)
        context.filesDir.listFiles { f -> f.name.startsWith("tv-logo-index-") }?.forEach { it.delete() }
        index.bufferedWriter().use { out ->
            for ((key, entry) in byKey) out.append(key).append('\t').append(entry.value.toString()).append('\t').append(entry.tier.toString()).append('\n')
        }
    }

    private fun readIndex(index: File): Boolean = runCatching {
        index.forEachLine { line ->
            val fields = line.split('\t')
            byKey[fields[0]] = LogoEntry(fields[1].toInt(), fields[2].toInt())
        }
        byKey.isNotEmpty()
    }.getOrDefault(false)

    private fun build(file: File, country: Country) {
        val db = File(context.cacheDir, "ChannelLogoDatabase.db")
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(DB_ENTRY) ?: return
            zip.getInputStream(entry).use { input -> db.outputStream().use { input.copyTo(it) } }
        }
        // One pass over the table; rows are then filed country first, international next, the rest last.
        val rows = ArrayList<Triple<Int, String, Int>>()
        SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY).use { sqlite ->
            sqlite.rawQuery("SELECT Country, SecondaryLogoName, LogoId FROM ChannelLogoTable", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    val tier = when (cursor.getString(0)) {
                        country.englishName -> LogoEntry.COUNTRY
                        "international" -> LogoEntry.INTERNATIONAL
                        else -> LogoEntry.OTHER
                    }
                    rows += Triple(tier, name, cursor.getInt(2))
                }
            }
        }
        db.delete()
        rows.sortBy { it.first } // stable: table order kept within a tier
        for ((tier, name, logoId) in rows) {
            for (key in NameMatcher.logoKeys(name)) byKey.putIfAbsent(key, LogoEntry(logoId, tier))
        }
    }

    fun entries(): Map<String, LogoEntry<Int>> = byKey

    fun read(logoId: Int): ByteArray {
        val file = apk ?: error("Built-in logos not available")
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("assets/Logos/sys/$logoId.png") ?: error("Logo $logoId not found")
            return zip.getInputStream(entry).use { it.readBytes() }
        }
    }

    private fun findApk(): File? {
        val known = runCatching {
            File(context.packageManager.getApplicationInfo(TP_VISION_PROVIDER, 0).sourceDir)
        }.getOrNull()
        if (known != null && hasTable(known)) return known
        // Other firmware versions may name the package differently; look for the table itself.
        return File("/system/app").listFiles()?.asSequence()
            ?.flatMap { dir -> dir.listFiles()?.asSequence().orEmpty() }
            ?.filter { it.name.endsWith(".apk") && it.name.contains("content", ignoreCase = true) }
            ?.firstOrNull { hasTable(it) }
    }

    private fun hasTable(apk: File): Boolean =
        apk.canRead() && runCatching { ZipFile(apk).use { it.getEntry(DB_ENTRY) != null } }.getOrDefault(false)

    private companion object {
        const val TP_VISION_PROVIDER = "com.example.tvcontentprovider"
        const val DB_ENTRY = "assets/Logos/ChannelLogoDatabase.db"
    }
}

/**
 * Community logos from github.com/tv-logo/tv-logos (countries/<country>/<name>-<cc>.png).
 * The repository has no license, so nothing is bundled: logos are downloaded on the TV for the
 * owner's personal use.
 */
class GitHubLogoSource(private val context: Context) {

    /** Key → "folder/file.png". */
    private val byKey = HashMap<String, LogoEntry<String>>()

    val size: Int get() = byKey.size

    /**
     * Loads the file lists of [country]'s folder, then the international, UK and US folders
     * (pay-TV channels carried in many countries). Cached lists are used when GitHub cannot be
     * reached; fails only if no list at all could be loaded.
     */
    fun load(country: Country, refresh: Boolean = false) {
        byKey.clear()
        if (refresh) File(context.cacheDir, "github").deleteRecursively()
        val folders = linkedMapOf(country.gitHubFolder to country.gitHubSuffix)
        for ((folder, suffix) in EXTRA_FOLDERS) folders.putIfAbsent(folder, suffix)
        var loaded = 0
        for ((folder, suffix) in folders) {
            val tier = if (folder == country.gitHubFolder) LogoEntry.COUNTRY else LogoEntry.INTERNATIONAL
            val json = runCatching { listing(folder, refresh) }.getOrNull() ?: continue
            loaded++
            val files = JSONArray(json)
            for (i in 0 until files.length()) {
                val name = files.getJSONObject(i).optString("name")
                val key = NameMatcher.gitHubKey(name, suffix) ?: continue
                for (variant in listOfNotNull(key, NameMatcher.withoutQuality(key))) {
                    byKey.putIfAbsent(variant, LogoEntry("$folder/$name", tier))
                }
            }
        }
        check(loaded > 0) { "No GitHub logo list could be loaded" }
    }

    fun entries(): Map<String, LogoEntry<String>> = byKey

    /** [path] is "folder/file.png" as stored in [entries]. */
    fun read(path: String): ByteArray {
        val cached = File(File(context.cacheDir, "github"), path)
        if (cached.exists()) return cached.readBytes()
        val bytes = httpGet("https://raw.githubusercontent.com/$REPO/main/countries/$path")
        cached.parentFile?.mkdirs()
        cached.writeBytes(bytes)
        return bytes
    }

    /** When the lists were last downloaded (0 if never). */
    fun updatedAt(country: Country): Long = File(context.filesDir, "github-${country.gitHubFolder}.json").lastModified()

    private fun listing(folder: String, refresh: Boolean): String {
        val cache = File(context.filesDir, "github-$folder.json")
        // The catalogue changes rarely; a day-old list saves four requests on every start.
        if (!refresh && cache.exists() && System.currentTimeMillis() - cache.lastModified() < LISTING_MAX_AGE_MS) return cache.readText()
        return runCatching {
            httpGet("https://api.github.com/repos/$REPO/contents/countries/$folder")
                .toString(Charsets.UTF_8).also { cache.writeText(it) }
        }.getOrElse { if (cache.exists()) cache.readText() else throw it }
    }

    private companion object {
        const val REPO = "tv-logo/tv-logos"
        val EXTRA_FOLDERS = listOf("international" to "int", "united-kingdom" to "uk", "united-states" to "us")
        const val LISTING_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}

/**
 * Logos from github.com/picons/picons (GPL-3.0, so downloaded on the TV, never bundled). Its
 * `srp.index` links DVB services ("SID_TSID_ONID_NAMESPACE" → logo name) and `snp.index` service
 * names to logos; the logos themselves are SVG or PNG files in `build-source/logos`.
 */
class PiconsSource(private val context: Context) {

    /** Name key → logo name. */
    private val byKey = HashMap<String, LogoEntry<String>>()
    /** "SID_TSID_ONID" → logo name. */
    private val byService = HashMap<String, String>()

    val size: Int get() = byService.size + byKey.size

    fun load(country: Country, refresh: Boolean = false) {
        byKey.clear()
        byService.clear()
        if (refresh) File(context.cacheDir, "picons").deleteRecursively()
        val services = parseIndex(index("srp.index", refresh))
        byService += servicesFor(services, PREFERRED_NAMESPACES[country.isoCode])
        // Service names come from every satellite and country, so they are only a fallback.
        for ((name, logo) in parseIndex(index("snp.index", refresh))) {
            for (key in NameMatcher.logoKeys(name)) byKey.putIfAbsent(key, LogoEntry(logo, LogoEntry.INTERNATIONAL))
        }
    }

    fun entries(): Map<String, LogoEntry<String>> = byKey

    fun services(): Map<String, String> = byService

    /** When the indexes were last downloaded (0 if never). */
    fun updatedAt(): Long = File(context.filesDir, "picons-srp.index").lastModified()

    /** The logo [name] as PNG: the PNG file if there is one, else the SVG drawn into a bitmap. */
    fun read(name: String): ByteArray {
        val cached = File(File(context.cacheDir, "picons"), "$name.png")
        if (cached.exists()) return cached.readBytes()
        val png = runCatching { httpGet("$RAW/build-source/logos/$name.default.png") }.getOrNull()
            ?: LogoImage.fromSvg(httpGet("$RAW/build-source/logos/$name.default.svg"))
        cached.parentFile?.mkdirs()
        cached.writeBytes(png)
        return png
    }

    private fun index(name: String, refresh: Boolean): List<String> {
        val cache = File(context.filesDir, "picons-$name")
        if (!refresh && cache.exists() && System.currentTimeMillis() - cache.lastModified() < INDEX_MAX_AGE_MS) return cache.readLines()
        return runCatching {
            httpGet("$RAW/build-source/$name").also { cache.writeBytes(it) }.toString(Charsets.UTF_8).lines()
        }.getOrElse { if (cache.exists()) cache.readLines() else throw it }
    }

    companion object {
        private const val RAW = "https://raw.githubusercontent.com/picons/picons/master"
        private const val INDEX_MAX_AGE_MS = 24 * 60 * 60 * 1000L

        /**
         * Satellite namespaces (orbital position) whose services win when the same IDs are used
         * on several satellites: the TV provider does not say which satellite a channel is on.
         */
        private val PREFERRED_NAMESPACES = mapOf("TR" to "1A40000") // Türksat 42°E

        /** "key=value" lines; comments and malformed lines are skipped. */
        internal fun parseIndex(lines: List<String>): List<Pair<String, String>> = lines.mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0 || line.startsWith("#")) null
            else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }

        /**
         * "SID_TSID_ONID" → logo name from service references "SID_TSID_ONID_NAMESPACE". When
         * satellites disagree about a service, [preferredNamespace]'s logo is used; if none of
         * them is preferred, the service is left out rather than guessed.
         */
        internal fun servicesFor(references: List<Pair<String, String>>, preferredNamespace: String?): Map<String, String> {
            val byIds = LinkedHashMap<String, MutableList<Pair<String, String>>>()
            for ((reference, logo) in references) {
                val parts = reference.uppercase().split('_')
                if (parts.size != 4) continue
                byIds.getOrPut("${parts[0]}_${parts[1]}_${parts[2]}") { ArrayList() } += parts[3] to logo
            }
            val result = HashMap<String, String>()
            for ((ids, logos) in byIds) {
                val preferred = logos.filter { it.first == preferredNamespace }.map { it.second }.distinct()
                val all = logos.map { it.second }.distinct()
                when {
                    preferred.size == 1 -> result[ids] = preferred[0]
                    preferred.isEmpty() && all.size == 1 -> result[ids] = all[0]
                }
            }
            return result
        }
    }
}

/**
 * Logos on USB sticks (or other removable storage): PNG/JPEG/WebP files, matched by file name
 * ("ATV HD.png", "show-tv.jpg"). Needs the storage read permission.
 */
class UsbLogoSource {

    private val byKey = HashMap<String, LogoEntry<String>>()
    private val all = ArrayList<File>()

    val size: Int get() = all.size

    /** All images found, for picking by hand. */
    fun files(): List<File> = all

    fun load() {
        byKey.clear()
        all.clear()
        for (volume in volumes()) collect(volume, depth = 0)
        all.sortBy { it.name.lowercase() }
        for (file in all) {
            for (key in NameMatcher.logoKeys(file.nameWithoutExtension)) byKey.putIfAbsent(key, LogoEntry(file.path, LogoEntry.COUNTRY))
        }
    }

    fun entries(): Map<String, LogoEntry<String>> = byKey

    fun read(path: String): ByteArray = File(path).readBytes()

    /** Removable volumes are mounted under /storage next to the built-in "emulated" one. */
    private fun volumes(): List<File> =
        File("/storage").listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }.orEmpty()

    private fun collect(dir: File, depth: Int) {
        val entries = dir.listFiles() ?: return
        for (entry in entries) {
            if (all.size >= MAX_FILES) return
            if (entry.isDirectory) {
                if (depth < MAX_DEPTH && !entry.name.startsWith(".")) collect(entry, depth + 1)
            } else if (entry.extension.lowercase() in EXTENSIONS) {
                all += entry
            }
        }
    }

    private companion object {
        val EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
        const val MAX_DEPTH = 3
        const val MAX_FILES = 3000
    }
}

/**
 * Logos from the iptv-org database (github.com/iptv-org/database, public domain data): channel
 * names and alternative names with links to logos hosted elsewhere. The two JSON files are ~14 MB,
 * so they are read as streams into a compact index that is kept for a week.
 */
class IptvOrgSource(private val context: Context) {

    private val byKey = HashMap<String, LogoEntry<String>>()

    val size: Int get() = byKey.size

    fun load(country: Country, refresh: Boolean = false) {
        byKey.clear()
        if (refresh) File(context.cacheDir, "iptv").deleteRecursively()
        val index = File(context.filesDir, "iptv-org-${country.isoCode}.tsv")
        if (refresh || !index.exists() || System.currentTimeMillis() - index.lastModified() > INDEX_MAX_AGE_MS) {
            runCatching { build(country, index) }.onFailure { if (!index.exists()) throw it }
        }
        index.forEachLine { line ->
            val fields = line.split('\t')
            if (fields.size == 3) byKey.putIfAbsent(fields[0], LogoEntry(fields[1], fields[2].toInt()))
        }
    }

    fun entries(): Map<String, LogoEntry<String>> = byKey

    /** When the index was last built from iptv-org (0 if never). */
    fun updatedAt(country: Country): Long = File(context.filesDir, "iptv-org-${country.isoCode}.tsv").lastModified()

    fun read(url: String): ByteArray {
        val cached = File(File(context.cacheDir, "iptv"), url.hashCode().toUInt().toString(16))
        if (cached.exists()) return cached.readBytes()
        val bytes = httpGet(url)
        cached.parentFile?.mkdirs()
        cached.writeBytes(bytes)
        return bytes
    }

    private fun build(country: Country, index: File) {
        // Channel id → logo URL: the channel's own logo (not a feed's), raster formats only.
        val logos = HashMap<String, String>()
        stream("$API/logos.json") { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                var channel: String? = null
                var url: String? = null
                var feed: String? = null
                var format: String? = null
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "channel" -> channel = reader.nextStringOrNull()
                        "url" -> url = reader.nextStringOrNull()
                        "feed" -> feed = reader.nextStringOrNull()
                        "format" -> format = reader.nextStringOrNull()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (channel != null && url != null && feed == null && format?.uppercase() != "SVG") logos.putIfAbsent(channel, url)
            }
            reader.endArray()
        }
        // Names → logo, the TV's country first.
        val rows = ArrayList<Triple<Int, String, String>>()
        stream("$API/channels.json") { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                var id: String? = null
                var countryCode: String? = null
                val names = ArrayList<String>()
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "id" -> id = reader.nextStringOrNull()
                        "country" -> countryCode = reader.nextStringOrNull()
                        "name" -> reader.nextStringOrNull()?.let { names += it }
                        "alt_names" -> {
                            reader.beginArray()
                            while (reader.hasNext()) reader.nextStringOrNull()?.let { names += it }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                val url = logos[id] ?: continue
                val tier = if (countryCode == country.isoCode) LogoEntry.COUNTRY else LogoEntry.OTHER
                for (name in names) rows += Triple(tier, name, url)
            }
            reader.endArray()
        }
        rows.sortBy { it.first }
        val keys = LinkedHashMap<String, Pair<String, Int>>()
        for ((tier, name, url) in rows) {
            for (key in NameMatcher.logoKeys(name)) keys.putIfAbsent(key, url to tier)
        }
        val partial = File(index.path + ".part")
        partial.bufferedWriter().use { out ->
            for ((key, value) in keys) out.append(key).append('\t').append(value.first).append('\t').append(value.second.toString()).append('\n')
        }
        partial.renameTo(index)
    }

    private fun stream(url: String, read: (JsonReader) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode} for $url" }
            JsonReader(connection.inputStream.bufferedReader()).use(read)
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonReader.nextStringOrNull(): String? =
        if (peek() == android.util.JsonToken.NULL) { nextNull(); null } else nextString()

    private companion object {
        const val API = "https://iptv-org.github.io/api"
        const val INDEX_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    }
}

private const val USER_AGENT = "tv-channel-logos"

/** Downloads [url], following redirects (logo hosts often redirect). */
internal fun httpGet(url: String): ByteArray {
    var target = URL(url)
    repeat(5) {
        val connection = target.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            when (val code = connection.responseCode) {
                200 -> return connection.inputStream.use { it.readBytes() }
                301, 302, 303, 307, 308 -> target = URL(target, connection.getHeaderField("Location"))
                else -> error("HTTP $code for $url")
            }
        } finally {
            connection.disconnect()
        }
    }
    error("Too many redirects for $url")
}

/** Philips channel logos are 136×104; other logos are fitted into that box, keeping their shape. */
object LogoImage {
    const val WIDTH = 136
    const val HEIGHT = 104

    fun fit(png: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(png, 0, png.size) ?: error("Not an image")
        if (source.width == WIDTH && source.height == HEIGHT) return png
        val scale = minOf(WIDTH.toFloat() / source.width, HEIGHT.toFloat() / source.height)
        val width = source.width * scale
        val height = source.height * scale
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val left = (WIDTH - width) / 2
        val top = (HEIGHT - height) / 2
        Canvas(out).drawBitmap(source, null, RectF(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
        return png(out)
    }

    /** An SVG logo drawn into a PNG whose longer side is [SVG_SIZE] px; [fit] shrinks it later. */
    fun fromSvg(svgBytes: ByteArray): ByteArray {
        val svg = com.caverock.androidsvg.SVG.getFromInputStream(svgBytes.inputStream())
        val box = svg.documentViewBox
            ?: RectF(0f, 0f, svg.documentWidth.takeIf { it > 0 } ?: SVG_SIZE.toFloat(), svg.documentHeight.takeIf { it > 0 } ?: SVG_SIZE.toFloat())
        check(box.width() > 0 && box.height() > 0) { "SVG without a size" }
        val scale = SVG_SIZE / maxOf(box.width(), box.height())
        val width = (box.width() * scale).toInt().coerceAtLeast(1)
        val height = (box.height() * scale).toInt().coerceAtLeast(1)
        // With a view box set, the document size scales the drawing instead of cropping it.
        svg.setDocumentViewBox(box.left, box.top, box.width(), box.height())
        svg.setDocumentWidth(width.toFloat())
        svg.setDocumentHeight(height.toFloat())
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(out))
        return png(out)
    }

    private const val SVG_SIZE = 400

    /**
     * A logo drawn from the channel's name, for channels no catalogue knows: bold dark text on a
     * light tile like the TV's own logos, shrunk until it fits in two lines.
     */
    fun text(name: String): ByteArray {
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2F2F7.toInt() }
        canvas.drawRoundRect(RectF(2f, 2f, WIDTH - 2f, HEIGHT - 2f), 12f, 12f, tile)
        val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1C1E.toInt()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val text = name.trim()
        val maxWidth = WIDTH - 16
        var size = 34f
        var layout: android.text.StaticLayout
        while (true) {
            paint.textSize = size
            layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            val widest = (0 until layout.lineCount).maxOf { layout.getLineWidth(it) }
            val fits = layout.lineCount <= 2 && layout.height <= HEIGHT - 12 && widest <= maxWidth &&
                (0 until layout.lineCount).none { layout.getEllipsisCount(it) > 0 }
            if (fits || size <= 12f) break
            size -= 2f
        }
        canvas.save()
        canvas.translate(8f, (HEIGHT - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
        return png(out)
    }

    private fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
}
