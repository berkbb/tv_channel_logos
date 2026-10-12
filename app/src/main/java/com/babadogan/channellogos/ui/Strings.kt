package com.babadogan.channellogos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.babadogan.channellogos.settings.Settings
import java.util.Locale

/**
 * UI texts in Turkish and English. The language follows the TV's unless chosen in the settings;
 * it is Compose state, so a change redraws the screen at once.
 */
object Strings {
    private var tr: Boolean by mutableStateOf(Locale.getDefault().language == "tr")

    /** "system", "tr" or "en" ([Settings.language]). */
    fun setLanguage(language: String) {
        tr = if (language == Settings.LANGUAGE_SYSTEM) Locale.getDefault().language == "tr" else language == "tr"
    }

    private fun t(turkish: String, english: String) = if (tr) turkish else english

    val appTitle get() = t("Kanal Logoları", "Channel Logos")
    val appSubtitle get() = t(
        "TV'nin kanal listesindeki eksik logoları bulur ve ekler.",
        "Finds and adds the logos missing from the TV's channel list."
    )

    val connecting get() = t("TV'ye bağlanılıyor…", "Connecting to the TV…")
    val approveHint get() = t(
        "TV'de \"USB hata ayıklamaya izin verilsin mi?\" sorusu çıkarsa \"Bu bilgisayardan her zaman izin ver\" işaretleyip Tamam'a basın.",
        "If the TV asks \"Allow USB debugging?\", tick \"Always allow from this computer\" and press OK."
    )
    val connected get() = t("Bağlı", "Connected")
    val connectFailed get() = t("TV'ye bağlanılamadı", "Could not connect to the TV")
    val networkDebuggingHint get() = t(
        "Ayarlar → Geliştirici seçenekleri → \"Ağ üzerinden hata ayıklama\" açık olmalı.",
        "Settings → Developer options → \"Network debugging\" must be on."
    )
    val retry get() = t("Tekrar Dene", "Try Again")

    fun channelStats(total: Int, withLogo: Int, radios: Int) = t(
        "$total kanal ($radios radyo) · $withLogo logolu · ${total - withLogo} logosuz",
        "$total channels ($radios radio) · $withLogo with logo · ${total - withLogo} without"
    )

    fun sourceStats(tv: Int, gitHub: Int, iptv: Int) = t(
        "Kaynaklar: TV'nin tablosu $tv · GitHub $gitHub · iptv-org $iptv",
        "Sources: TV table $tv · GitHub $gitHub · iptv-org $iptv"
    )

    val tvTableMissing get() = t("Bu TV'de yerleşik logo tablosu yok", "This TV has no built-in logo table")
    val gitHubUnavailable get() = t("GitHub'a ulaşılamadı", "GitHub could not be reached")

    val rescan get() = t("Kanalları Yeniden Tara", "Rescan Channels")
    val refreshSources get() = t("Logoları İnternetten Güncelle", "Update Logos from the Internet")
    val updatingSources get() = t("Logo kaynakları internetten güncelleniyor…", "Updating logo sources from the internet…")
    val sourcesUpdated get() = t("Logo kaynakları güncellendi.", "Logo sources updated.")
    fun sourcesUpdatedAt(date: String) = t("İnternet kaynakları: $date", "Online sources: $date")
    fun apply(count: Int) = t("Seçilenleri Yaz ($count)", "Write Selected ($count)")
    val selectAll get() = t("Tümünü Seç", "Select All")
    val selectSafe get() = t("Güvenlileri Seç", "Select Safe Ones")
    val selectNone get() = t("Seçimi Kaldır", "Clear Selection")
    val showMissing get() = t("Logosuz TV kanalları", "TV channels without logo")
    val showAll get() = t("Tümü (radyolar dahil)", "All (incl. radio)")

    fun scanFailed(detail: String) = t("Kanallar okunamadı: $detail", "Could not read the channels: $detail")
    fun writeFailed(detail: String) = t("Logolar yazılamadı: $detail", "Could not write the logos: $detail")
    val loadingChannels get() = t("Kanallar okunuyor…", "Reading channels…")
    val loadingSources get() = t("Logo kaynakları yükleniyor…", "Loading logo sources…")
    fun writing(done: Int, total: Int) = t("Yazılıyor: $done / $total", "Writing: $done / $total")
    fun written(count: Int, failed: Int) =
        if (failed == 0) t("$count logo yazıldı.", "$count logos written.")
        else t("$count logo yazıldı, $failed başarısız.", "$count logos written, $failed failed.")

    val hasLogo get() = t("Logosu var", "Has a logo")
    val noLogo get() = t("Logo yok", "No logo")
    val noSuggestion get() = t("Öneri yok", "No suggestion")
    val sourceTv get() = t("TV", "TV")
    val sourceGitHub get() = "GitHub"
    val sourceIptv get() = "iptv-org"
    val sourceText get() = t("Yazılı logo", "Drawn logo")
    val sourceFile get() = "USB"
    val pickFromUsb get() = t("USB'den Seç", "Pick from USB")
    val backToSuggestions get() = t("Önerilere Dön", "Back to Suggestions")
    fun usbTitle(channel: String) = t("USB'den logo: $channel", "Logo from USB: $channel")
    val usbEmpty get() = t(
        "USB bellekte resim bulunamadı. PNG veya JPG dosyalarını belleğe kopyalayıp takın; dosya adı kanal adıyla aynıysa (ör. \"ATV HD.png\") logo kendiliğinden eşleşir.",
        "No images found on a USB stick. Copy PNG or JPG files to a stick and plug it in; a file named like the channel (e.g. \"ATV HD.png\") is matched automatically."
    )
    fun usbStats(count: Int) = t("USB: $count resim", "USB: $count images")
    val check get() = t("kontrol et", "check")
    val exact get() = t("tam eşleşme", "exact match")
    val close get() = t("yakın eşleşme", "close match")
    val loose get() = t("benzer isim", "similar name")
    val change get() = t("Değiştir", "Change")

    val pickTitle get() = t("Logo seç", "Pick a logo")
    val noLogoOption get() = t("Logo atama", "Don't assign")
    val noCandidates get() = t("Bu isme benzeyen logo bulunamadı.", "No logo with a similar name was found.")
    val cancel get() = t("Kapat", "Close")

    // ─── Settings ───
    val settings get() = t("Ayarlar", "Settings")
    val about get() = t("Hakkında", "About")
    val language get() = t("Dil", "Language")
    val languageSystem get() = t("Sistem", "System")
    val theme get() = t("Tema", "Theme")
    val themeDark get() = t("Koyu", "Dark")
    val themeLight get() = t("Açık", "Light")
    val preselection get() = t("Ön seçim", "Preselection")
    val preselectSafe get() = t("Güvenli eşleşmeler", "Safe matches")
    val preselectNone get() = t("Hiçbiri", "None")
    val preselectionDesc get() = t(
        "Güvenli: TV'nin ülkesine ait, adı birebir (veya sadece HD/SD farkıyla) eşleşen TV ve GitHub logoları.",
        "Safe: TV and GitHub logos of the TV's country whose name matches exactly (or up to HD/SD)."
    )
    val textLogosSetting get() = t("Yazılı logo önerileri", "Drawn logo suggestions")
    val textLogosDesc get() = t(
        "Hiçbir kaynakta logosu olmayan kanallara adından çizilen bir logo önerilir.",
        "Channels with no logo anywhere are offered a logo drawn from their name."
    )
    val sources get() = t("Kaynaklar", "Sources")
    val on get() = t("Açık", "On")
    val off get() = t("Kapalı", "Off")

    // ─── About ───
    fun version(name: String) = t("Sürüm $name", "Version $name")
    val aboutWhat get() = t(
        "Philips (TP Vision) gibi Android TV'lerin kanal listesinde logosu olmayan kanalları bulur, uygun logoyu önerir ve TV'nin kanal veritabanına yazar.",
        "Finds the channels without a logo in the channel list of Android TVs like Philips (TP Vision), suggests a fitting logo and writes it into the TV's channel database."
    )
    val aboutHowTitle get() = t("Nasıl çalışır?", "How does it work?")
    val aboutHow get() = t(
        "Yayın kanallarının logolarını yalnızca sistem ve ADB kabuğu değiştirebilir. Uygulama TV'nin kendi ADB'sine (ağ üzerinden hata ayıklama, localhost:5555) bağlanır ve küçük bir yardımcıyı kabuk yetkisiyle çalıştırır. Bu yüzden ilk açılışta TV bir kez izin ister.",
        "Only the system and the ADB shell may change broadcast channels' logos. The app connects to the TV's own ADB (network debugging, localhost:5555) and runs a small helper with shell rights, which is why the TV asks once for permission."
    )
    val aboutSourcesTitle get() = t("Logo kaynakları", "Logo sources")
    val aboutSources get() = t(
        "• TV'nin tablosu: TV yazılımındaki yerleşik logolar.\n• GitHub tv-logo/tv-logos: topluluk logoları; depo lisanssız olduğundan uygulamaya gömülmez, kişisel kullanım için TV'de indirilir.\n• iptv-org: kamu malı (Unlicense) kanal veritabanı; logolar başka sitelerde barındırılır.\n• Yazılı logo: uygulamanın kanal adından çizdiği logo.\nLogolar sahiplerinin ticari markalarıdır.",
        "• TV table: logos built into the TV's software.\n• GitHub tv-logo/tv-logos: community logos; the repository has no license, so nothing is bundled and logos are downloaded on the TV for personal use.\n• iptv-org: public-domain (Unlicense) channel database; logos are hosted elsewhere.\n• Drawn logo: drawn by the app from the channel name.\nLogos are trademarks of their owners."
    )
    val aboutNoteTitle get() = t("Bilmeniz gerekenler", "Good to know")
    val aboutNote get() = t(
        "TV'de kanal güncellemesi veya yeniden kurulum yazılan logoları silebilir; o zaman uygulamayı tekrar çalıştırın. Yanlış bir logoyu \"Tümü\" görünümünde \"Değiştir\" ile düzeltebilirsiniz.",
        "A channel update or reinstallation on the TV may remove the written logos; run the app again then. A wrong logo can be fixed with \"Change\" in the \"All\" view."
    )

    // ─── About: developer, license, disclaimer (as in AynaCast) ───
    val aboutCompany get() = "© Babadogan Tech & Edu"
    val aboutDeveloperTitle get() = t("Geliştirici ve Telif", "Developer & Copyright")
    val aboutDeveloper get() = t(
        "Babadogan Tech & Edu — Ankara Üniversitesi Teknokent\nbabadogan.net",
        "Babadogan Tech & Edu — Ankara University Technopolis\nbabadogan.net"
    )
    val aboutLicenseTitle get() = t("Lisans", "License")
    val aboutLicense get() = t("MIT Açık Kaynak Lisansı ile lisanslanmıştır.", "Licensed under the MIT Open Source License.")
    val aboutDisclaimerTitle get() = t("Yasal Uyarı ve Marka Bildirimi", "Legal Notice & Trademarks")
    val aboutDisclaimer get() = t(
        "Philips, Koninklijke Philips N.V.'nin; Android TV, Google LLC'nin ticari markasıdır. Kanal adları ve logoları ilgili yayıncıların ticari markalarıdır. Bu bağımsız proje Philips, TP Vision, Google, yayıncılar, tv-logo veya iptv-org ile ilişkili değildir ve onlar tarafından onaylanmamıştır. Logolar yalnızca cihaz sahibinin kişisel kullanımı için TV'nin kendi kanal listesine yazılır; uygulama hiçbir logoyu dağıtmaz.",
        "Philips is a trademark of Koninklijke Philips N.V.; Android TV is a trademark of Google LLC. Channel names and logos are trademarks of their broadcasters. This independent project is not affiliated with or endorsed by Philips, TP Vision, Google, the broadcasters, tv-logo or iptv-org. Logos are written to the TV's own channel list for the owner's personal use only; the app distributes no logos."
    )

    // ─── Backup and reset ───
    val backupSection get() = t("Yedekleme", "Backup")
    val backupLogos get() = t("Logoları Yedekle", "Back Up Logos")
    val restoreBackup get() = t("Yedekten Geri Yükle", "Restore Backup")
    fun resetToDefault(count: Int) = t("Varsayılana Döndür ($count)", "Reset to Default ($count)")
    val resetOne get() = t("Varsayılana Döndür", "Reset to Default")
    fun backupInfo(count: Int, date: String) = t("Yedek: $count logo, $date", "Backup: $count logos, $date")
    val noBackup get() = t("Henüz yedek yok", "No backup yet")
    val changedMark get() = t("değiştirildi", "changed")
    val backingUp get() = t("Logolar yedekleniyor…", "Backing up logos…")
    fun backedUp(count: Int) = t("$count logo yedeklendi.", "$count logos backed up.")
    val restoring get() = t("Yedek geri yükleniyor…", "Restoring the backup…")
    fun restored(count: Int, missed: Int) =
        if (missed == 0) t("$count logo geri yüklendi.", "$count logos restored.")
        else t("$count logo geri yüklendi, $missed kanal bulunamadı.", "$count logos restored, $missed channels not found.")
    val resetting get() = t("Orijinal logolar geri konuyor…", "Putting the original logos back…")
    fun resetDone(count: Int, failed: Int) =
        if (failed == 0) t("$count kanal varsayılana döndü.", "$count channels reset to default.")
        else t("$count kanal varsayılana döndü, $failed başarısız.", "$count channels reset, $failed failed.")
    val confirm get() = t("Onayla", "Confirm")
    val yes get() = t("Evet", "Yes")
    val no get() = t("Vazgeç", "Cancel")
    val confirmBackup get() = t(
        "Tüm kanalların şu anki logoları yedeklenecek. Önceki yedeğin yerine geçer.",
        "The current logos of all channels will be backed up, replacing the previous backup."
    )
    val confirmRestore get() = t(
        "Yedekteki logolar, kanal numarası ve adı eşleşen kanallara yazılacak.",
        "The backed-up logos will be written to the channels with the same number and name."
    )
    fun confirmReset(count: Int) = t(
        "Bu uygulamanın değiştirdiği $count kanal ilk hâline döndürülecek: önceki logosu geri konacak, logosu yoksa silinecek.",
        "The $count channels this app changed will go back to how they were: their earlier logo is put back, or removed if they had none."
    )
}
