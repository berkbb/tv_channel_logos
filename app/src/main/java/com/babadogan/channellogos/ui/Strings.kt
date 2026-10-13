package com.babadogan.channellogos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.babadogan.channellogos.settings.Settings
import java.util.Locale

/**
 * UI texts in Turkish, English and Romanian. The language follows the TV's unless chosen in the
 * settings (English for any other TV language); it is Compose state, so a change redraws the
 * screen at once.
 */
object Strings {
    /** Languages with texts; [Settings.language] is one of these or "system". */
    val LANGUAGES = listOf("tr", "en", "ro")

    private var current: String by mutableStateOf(systemLanguage())

    /** "system", "tr", "en" or "ro" ([Settings.language]). */
    fun setLanguage(language: String) {
        current = if (language == Settings.LANGUAGE_SYSTEM) systemLanguage() else language.takeIf { it in LANGUAGES } ?: "en"
    }

    private fun systemLanguage() = Locale.getDefault().language.takeIf { it in LANGUAGES } ?: "en"

    private fun t(turkish: String, english: String, romanian: String) = when (current) {
        "tr" -> turkish
        "ro" -> romanian
        else -> english
    }

    val appTitle get() = t("Kanal Logoları", "Channel Logos", "Logo-uri canale")
    val appSubtitle get() = t(
        "TV'nin kanal listesindeki eksik logoları bulur ve ekler.",
        "Finds and adds the logos missing from the TV's channel list.",
        "Găsește și adaugă logo-urile care lipsesc din lista de canale a televizorului."
    )

    val connecting get() = t("TV'ye bağlanılıyor…", "Connecting to the TV…", "Conectare la televizor…")
    val approveHint get() = t(
        "TV'de \"USB hata ayıklamaya izin verilsin mi?\" sorusu çıkarsa \"Bu bilgisayardan her zaman izin ver\" işaretleyip Tamam'a basın.",
        "If the TV asks \"Allow USB debugging?\", tick \"Always allow from this computer\" and press OK.",
        "Dacă televizorul întreabă „Permiteți remedierea erorilor prin USB?”, bifați „Permiteți întotdeauna de pe acest computer” și apăsați OK."
    )
    val connected get() = t("Bağlı", "Connected", "Conectat")
    val connectFailed get() = t("TV'ye bağlanılamadı", "Could not connect to the TV", "Nu s-a putut conecta la televizor")
    val networkDebuggingHint get() = t(
        "Ayarlar → Geliştirici seçenekleri → \"Ağ üzerinden hata ayıklama\" açık olmalı.",
        "Settings → Developer options → \"Network debugging\" must be on.",
        "Setări → Opțiuni pentru dezvoltatori → „Remedierea erorilor prin rețea” trebuie să fie activată."
    )
    val retry get() = t("Tekrar Dene", "Try Again", "Încearcă din nou")

    fun channelStats(total: Int, withLogo: Int, radios: Int) = t(
        "$total kanal ($radios radyo) · $withLogo logolu · ${total - withLogo} logosuz",
        "$total channels ($radios radio) · $withLogo with logo · ${total - withLogo} without",
        "$total canale ($radios radio) · $withLogo cu logo · ${total - withLogo} fără"
    )

    fun sourceStats(tv: Int, gitHub: Int, picons: Int, iptv: Int) = t(
        "Kaynaklar: TV'nin tablosu $tv · GitHub $gitHub · picons $picons · iptv-org $iptv",
        "Sources: TV table $tv · GitHub $gitHub · picons $picons · iptv-org $iptv",
        "Surse: tabelul televizorului $tv · GitHub $gitHub · picons $picons · iptv-org $iptv"
    )

    val tvTableMissing get() = t("Bu TV'de yerleşik logo tablosu yok", "This TV has no built-in logo table", "Acest televizor nu are un tabel de logo-uri încorporat")
    val gitHubUnavailable get() = t("GitHub'a ulaşılamadı", "GitHub could not be reached", "GitHub nu a putut fi accesat")

    val rescan get() = t("Kanalları Yeniden Tara", "Rescan Channels", "Rescanează canalele")
    val refreshSources get() = t("Logoları İnternetten Güncelle", "Update Logos from the Internet", "Actualizează logo-urile de pe internet")
    val updatingSources get() = t("Logo kaynakları internetten güncelleniyor…", "Updating logo sources from the internet…", "Se actualizează sursele de logo-uri de pe internet…")
    val sourcesUpdated get() = t("Logo kaynakları güncellendi.", "Logo sources updated.", "Sursele de logo-uri au fost actualizate.")
    fun sourcesUpdatedAt(date: String) = t("İnternet kaynakları: $date", "Online sources: $date", "Surse online: $date")
    fun apply(count: Int) = t("Seçilenleri Yaz ($count)", "Write Selected ($count)", "Scrie selecția ($count)")
    val selectAll get() = t("Tümünü Seç", "Select All", "Selectează tot")
    val selectSafe get() = t("Güvenlileri Seç", "Select Safe Ones", "Selectează sigurele")
    val selectNone get() = t("Seçimi Kaldır", "Clear Selection", "Anulează selecția")
    val showMissing get() = t("Logosuz TV kanalları", "TV channels without logo", "Canale TV fără logo")
    val showAll get() = t("Tümü (radyolar dahil)", "All (incl. radio)", "Toate (inclusiv radio)")

    fun scanFailed(detail: String) = t("Kanallar okunamadı: $detail", "Could not read the channels: $detail", "Canalele nu au putut fi citite: $detail")
    fun writeFailed(detail: String) = t("Logolar yazılamadı: $detail", "Could not write the logos: $detail", "Logo-urile nu au putut fi scrise: $detail")
    val loadingChannels get() = t("Kanallar okunuyor…", "Reading channels…", "Se citesc canalele…")
    val loadingSources get() = t("Logo kaynakları yükleniyor…", "Loading logo sources…", "Se încarcă sursele de logo-uri…")
    fun writing(done: Int, total: Int) = t("Yazılıyor: $done / $total", "Writing: $done / $total", "Se scrie: $done / $total")
    fun written(count: Int, failed: Int) =
        if (failed == 0) t("$count logo yazıldı.", "$count logos written.", "$count logo-uri scrise.")
        else t("$count logo yazıldı, $failed başarısız.", "$count logos written, $failed failed.", "$count logo-uri scrise, $failed eșuate.")

    val hasLogo get() = t("Logosu var", "Has a logo", "Are logo")
    val noLogo get() = t("Logo yok", "No logo", "Fără logo")
    val noSuggestion get() = t("Öneri yok", "No suggestion", "Nicio sugestie")
    val sourceTv get() = "TV"
    val sourceGitHub get() = "GitHub"
    val sourcePicons get() = "picons"
    val sourceIptv get() = "iptv-org"
    val sourceText get() = t("Yazılı logo", "Drawn logo", "Logo desenat")
    val sourceFile get() = "USB"
    val pickFromUsb get() = t("USB'den Seç", "Pick from USB", "Alege de pe USB")
    val backToSuggestions get() = t("Önerilere Dön", "Back to Suggestions", "Înapoi la sugestii")
    fun usbTitle(channel: String) = t("USB'den logo: $channel", "Logo from USB: $channel", "Logo de pe USB: $channel")
    val usbEmpty get() = t(
        "USB bellekte resim bulunamadı. PNG veya JPG dosyalarını belleğe kopyalayıp takın; dosya adı kanal adıyla aynıysa (ör. \"ATV HD.png\") logo kendiliğinden eşleşir.",
        "No images found on a USB stick. Copy PNG or JPG files to a stick and plug it in; a file named like the channel (e.g. \"ATV HD.png\") is matched automatically.",
        "Nu s-au găsit imagini pe stick-ul USB. Copiați fișiere PNG sau JPG pe un stick și conectați-l; un fișier numit ca și canalul (de ex. „PRO TV HD.png”) este asociat automat."
    )
    fun usbStats(count: Int) = t("USB: $count resim", "USB: $count images", "USB: $count imagini")
    val check get() = t("kontrol et", "check", "verifică")
    val exact get() = t("tam eşleşme", "exact match", "potrivire exactă")
    val close get() = t("yakın eşleşme", "close match", "potrivire apropiată")
    val loose get() = t("benzer isim", "similar name", "nume asemănător")
    val change get() = t("Değiştir", "Change", "Schimbă")

    val pickTitle get() = t("Logo seç", "Pick a logo", "Alege un logo")
    val noLogoOption get() = t("Logo atama", "Don't assign", "Nu atribui")
    val noCandidates get() = t("Bu isme benzeyen logo bulunamadı.", "No logo with a similar name was found.", "Nu s-a găsit niciun logo cu un nume asemănător.")
    val cancel get() = t("Kapat", "Close", "Închide")

    // ─── Settings ───
    val settings get() = t("Ayarlar", "Settings", "Setări")
    val about get() = t("Hakkında", "About", "Despre")
    val language get() = t("Dil", "Language", "Limbă")
    val languageSystem get() = t("Sistem", "System", "Sistem")
    val theme get() = t("Tema", "Theme", "Temă")
    val themeDark get() = t("Koyu", "Dark", "Întunecată")
    val themeLight get() = t("Açık", "Light", "Luminoasă")
    val preselection get() = t("Ön seçim", "Preselection", "Preselecție")
    val preselectSafe get() = t("Güvenli eşleşmeler", "Safe matches", "Potriviri sigure")
    val preselectNone get() = t("Hiçbiri", "None", "Niciuna")
    val preselectionDesc get() = t(
        "Güvenli: TV'nin ülkesine ait, adı birebir (veya sadece HD/SD farkıyla) eşleşen TV ve GitHub logoları; yayın kimliği (SID/TSID/ONID) ve adı tutan picons logoları.",
        "Safe: TV and GitHub logos of the TV's country whose name matches exactly (or up to HD/SD); picons logos whose broadcast IDs (SID/TSID/ONID) and name agree.",
        "Sigure: logo-urile din tabelul televizorului și de pe GitHub din țara televizorului, al căror nume se potrivește exact (sau doar cu diferența HD/SD); logo-urile picons ale căror ID-uri de difuzare (SID/TSID/ONID) și nume se potrivesc."
    )
    val textLogosSetting get() = t("Yazılı logo önerileri", "Drawn logo suggestions", "Sugestii de logo desenat")
    val textLogosDesc get() = t(
        "Hiçbir kaynakta logosu olmayan kanallara adından çizilen bir logo önerilir.",
        "Channels with no logo anywhere are offered a logo drawn from their name.",
        "Canalelor fără logo în nicio sursă li se propune un logo desenat din numele lor."
    )
    val sources get() = t("Kaynaklar", "Sources", "Surse")
    val on get() = t("Açık", "On", "Activat")
    val off get() = t("Kapalı", "Off", "Dezactivat")

    // ─── About ───
    fun version(name: String) = t("Sürüm $name", "Version $name", "Versiunea $name")
    val aboutWhat get() = t(
        "Philips (TP Vision) gibi Android TV'lerin kanal listesinde logosu olmayan kanalları bulur, uygun logoyu önerir ve TV'nin kanal veritabanına yazar.",
        "Finds the channels without a logo in the channel list of Android TVs like Philips (TP Vision), suggests a fitting logo and writes it into the TV's channel database.",
        "Găsește canalele fără logo din lista de canale a televizoarelor Android TV precum Philips (TP Vision), propune un logo potrivit și îl scrie în baza de date de canale a televizorului."
    )
    val aboutHowTitle get() = t("Nasıl çalışır?", "How does it work?", "Cum funcționează?")
    val aboutHow get() = t(
        "Yayın kanallarının logolarını yalnızca sistem ve ADB kabuğu değiştirebilir. Uygulama TV'nin kendi ADB'sine (ağ üzerinden hata ayıklama, localhost:5555) bağlanır ve küçük bir yardımcıyı kabuk yetkisiyle çalıştırır. Bu yüzden ilk açılışta TV bir kez izin ister.",
        "Only the system and the ADB shell may change broadcast channels' logos. The app connects to the TV's own ADB (network debugging, localhost:5555) and runs a small helper with shell rights, which is why the TV asks once for permission.",
        "Doar sistemul și shell-ul ADB pot schimba logo-urile canalelor de difuzare. Aplicația se conectează la ADB-ul televizorului (remedierea erorilor prin rețea, localhost:5555) și rulează un mic program ajutător cu drepturi de shell; de aceea televizorul cere o dată permisiunea."
    )
    val aboutSourcesTitle get() = t("Logo kaynakları", "Logo sources", "Surse de logo-uri")
    val aboutSources get() = t(
        "• TV'nin tablosu: TV yazılımındaki yerleşik logolar.\n• GitHub tv-logo/tv-logos: topluluk logoları; depo lisanssız olduğundan uygulamaya gömülmez, kişisel kullanım için TV'de indirilir.\n• picons: kanalların yayın kimliklerine bağlı logolar (GPL-3.0); uygulamaya gömülmez, TV'de indirilir.\n• iptv-org: kamu malı (Unlicense) kanal veritabanı; logolar başka sitelerde barındırılır.\n• Yazılı logo: uygulamanın kanal adından çizdiği logo.\nLogolar sahiplerinin ticari markalarıdır.",
        "• TV table: logos built into the TV's software.\n• GitHub tv-logo/tv-logos: community logos; the repository has no license, so nothing is bundled and logos are downloaded on the TV for personal use.\n• picons: logos linked to the channels' broadcast IDs (GPL-3.0); not bundled, downloaded on the TV.\n• iptv-org: public-domain (Unlicense) channel database; logos are hosted elsewhere.\n• Drawn logo: drawn by the app from the channel name.\nLogos are trademarks of their owners.",
        "• Tabelul televizorului: logo-uri încorporate în software-ul televizorului.\n• GitHub tv-logo/tv-logos: logo-uri ale comunității; depozitul nu are licență, așa că nimic nu este inclus în aplicație, iar logo-urile se descarcă pe televizor pentru uz personal.\n• picons: logo-uri legate de ID-urile de difuzare ale canalelor (GPL-3.0); nu sunt incluse, se descarcă pe televizor.\n• iptv-org: bază de date de canale în domeniul public (Unlicense); logo-urile sunt găzduite pe alte site-uri.\n• Logo desenat: desenat de aplicație din numele canalului.\nLogo-urile sunt mărci comerciale ale proprietarilor lor."
    )
    val aboutNoteTitle get() = t("Bilmeniz gerekenler", "Good to know", "De reținut")
    val aboutNote get() = t(
        "TV'de kanal güncellemesi veya yeniden kurulum yazılan logoları silebilir; o zaman uygulamayı tekrar çalıştırın. Yanlış bir logoyu \"Tümü\" görünümünde \"Değiştir\" ile düzeltebilirsiniz.",
        "A channel update or reinstallation on the TV may remove the written logos; run the app again then. A wrong logo can be fixed with \"Change\" in the \"All\" view.",
        "O actualizare a canalelor sau o reinstalare pe televizor poate șterge logo-urile scrise; atunci rulați din nou aplicația. Un logo greșit se poate corecta cu „Schimbă” în vizualizarea „Toate”."
    )

    // ─── About: developer, license, disclaimer (as in AynaCast) ───
    val aboutCompany get() = "© Babadogan Tech & Edu"
    val aboutDeveloperTitle get() = t("Geliştirici ve Telif", "Developer & Copyright", "Dezvoltator și drepturi de autor")
    val aboutDeveloper get() = t(
        "Babadogan Tech & Edu — Ankara Üniversitesi Teknokent\nbabadogan.net",
        "Babadogan Tech & Edu — Ankara University Technopolis\nbabadogan.net",
        "Babadogan Tech & Edu — Parcul Tehnologic al Universității din Ankara\nbabadogan.net"
    )
    val aboutLicenseTitle get() = t("Lisans", "License", "Licență")
    val aboutLicense get() = t(
        "MIT Açık Kaynak Lisansı ile lisanslanmıştır.",
        "Licensed under the MIT Open Source License.",
        "Licențiat sub licența open source MIT."
    )
    val aboutDisclaimerTitle get() = t("Yasal Uyarı ve Marka Bildirimi", "Legal Notice & Trademarks", "Notă legală și mărci comerciale")
    val aboutDisclaimer get() = t(
        "Philips, Koninklijke Philips N.V.'nin; Android TV, Google LLC'nin ticari markasıdır. Kanal adları ve logoları ilgili yayıncıların ticari markalarıdır. Bu bağımsız proje Philips, TP Vision, Google, yayıncılar, tv-logo, picons veya iptv-org ile ilişkili değildir ve onlar tarafından onaylanmamıştır. Logolar yalnızca cihaz sahibinin kişisel kullanımı için TV'nin kendi kanal listesine yazılır; uygulama hiçbir logoyu dağıtmaz.",
        "Philips is a trademark of Koninklijke Philips N.V.; Android TV is a trademark of Google LLC. Channel names and logos are trademarks of their broadcasters. This independent project is not affiliated with or endorsed by Philips, TP Vision, Google, the broadcasters, tv-logo, picons or iptv-org. Logos are written to the TV's own channel list for the owner's personal use only; the app distributes no logos.",
        "Philips este marcă comercială a Koninklijke Philips N.V.; Android TV este marcă comercială a Google LLC. Numele și logo-urile canalelor sunt mărci comerciale ale radiodifuzorilor lor. Acest proiect independent nu este afiliat cu și nici aprobat de Philips, TP Vision, Google, radiodifuzori, tv-logo, picons sau iptv-org. Logo-urile sunt scrise în lista de canale a televizorului doar pentru uzul personal al proprietarului; aplicația nu distribuie niciun logo."
    )

    // ─── Backup and reset ───
    val backupSection get() = t("Yedekleme", "Backup", "Copie de rezervă")
    val backupLogos get() = t("Logoları Yedekle", "Back Up Logos", "Salvează logo-urile")
    val restoreBackup get() = t("Yedekten Geri Yükle", "Restore Backup", "Restaurează copia")
    fun resetToDefault(count: Int) = t("Varsayılana Döndür ($count)", "Reset to Default ($count)", "Revino la implicit ($count)")
    val resetOne get() = t("Varsayılana Döndür", "Reset to Default", "Revino la implicit")
    fun backupInfo(count: Int, date: String) = t("Yedek: $count logo, $date", "Backup: $count logos, $date", "Copie: $count logo-uri, $date")
    val noBackup get() = t("Henüz yedek yok", "No backup yet", "Încă nu există o copie")
    val changedMark get() = t("değiştirildi", "changed", "modificat")
    val backingUp get() = t("Logolar yedekleniyor…", "Backing up logos…", "Se salvează logo-urile…")
    fun backedUp(count: Int) = t("$count logo yedeklendi.", "$count logos backed up.", "$count logo-uri salvate.")
    val restoring get() = t("Yedek geri yükleniyor…", "Restoring the backup…", "Se restaurează copia…")
    fun restored(count: Int, missed: Int) =
        if (missed == 0) t("$count logo geri yüklendi.", "$count logos restored.", "$count logo-uri restaurate.")
        else t("$count logo geri yüklendi, $missed kanal bulunamadı.", "$count logos restored, $missed channels not found.", "$count logo-uri restaurate, $missed canale negăsite.")
    val resetting get() = t("Orijinal logolar geri konuyor…", "Putting the original logos back…", "Se pun la loc logo-urile originale…")
    fun resetDone(count: Int, failed: Int) =
        if (failed == 0) t("$count kanal varsayılana döndü.", "$count channels reset to default.", "$count canale readuse la implicit.")
        else t("$count kanal varsayılana döndü, $failed başarısız.", "$count channels reset, $failed failed.", "$count canale readuse la implicit, $failed eșuate.")
    val confirm get() = t("Onayla", "Confirm", "Confirmă")
    val yes get() = t("Evet", "Yes", "Da")
    val no get() = t("Vazgeç", "Cancel", "Renunță")
    val confirmBackup get() = t(
        "Tüm kanalların şu anki logoları yedeklenecek. Önceki yedeğin yerine geçer.",
        "The current logos of all channels will be backed up, replacing the previous backup.",
        "Logo-urile actuale ale tuturor canalelor vor fi salvate, înlocuind copia anterioară."
    )
    val confirmRestore get() = t(
        "Yedekteki logolar, kanal numarası ve adı eşleşen kanallara yazılacak.",
        "The backed-up logos will be written to the channels with the same number and name.",
        "Logo-urile din copie vor fi scrise pe canalele cu același număr și nume."
    )
    fun confirmReset(count: Int) = t(
        "Bu uygulamanın değiştirdiği $count kanal ilk hâline döndürülecek: önceki logosu geri konacak, logosu yoksa silinecek.",
        "The $count channels this app changed will go back to how they were: their earlier logo is put back, or removed if they had none.",
        "Cele $count canale modificate de această aplicație vor reveni la starea inițială: logo-ul anterior este pus la loc sau șters dacă nu aveau unul."
    )
}
