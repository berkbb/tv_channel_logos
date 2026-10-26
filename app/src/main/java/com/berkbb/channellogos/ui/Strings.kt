package com.berkbb.channellogos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.berkbb.channellogos.settings.Settings
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

    /** Locale of the shown language: dates and upper case follow it, not the TV's language. */
    val locale: Locale get() = Locale.forLanguageTag(current)

    /** Date and time in the shown language ("14 oct. 2026, 10:30" in Romanian). */
    fun dateTime(epochMillis: Long): String =
        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale)
            .format(java.util.Date(epochMillis))

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

    fun extraCountries(names: String) = t(
        "Listede $names kanalları var; $names logoları da aranıyor.",
        "The list has channels from $names; $names logos are searched too.",
        "Lista are canale din $names; se caută și logo-uri din $names."
    )
    val tvTableMissing get() = t("Bu TV'de yerleşik logo tablosu yok", "This TV has no built-in logo table", "Acest televizor nu are un tabel de logo-uri încorporat")
    val gitHubUnavailable get() = t("GitHub'a ulaşılamadı", "GitHub could not be reached", "GitHub nu a putut fi accesat")

    val rescan get() = t("Kanalları Yeniden Tara", "Rescan Channels", "Rescanează canalele")
    val refreshSources get() = t("Logoları İnternetten Güncelle", "Update Logos from the Internet", "Actualizează logo-urile de pe internet")
    val updatingSources get() = t("Logo kaynakları internetten güncelleniyor…", "Updating logo sources from the internet…", "Se actualizează sursele de logo-uri de pe internet…")
    val sourcesUpdated get() = t("Logo kaynakları güncellendi.", "Logo sources updated.", "Sursele de logo-uri au fost actualizate.")
    // ─── Badges on the main screen ───
    fun channelCount(count: Int) = t("$count kanal", "$count channels", "$count canale")
    fun withLogoCount(count: Int) = t("$count logolu", "$count with logo", "$count cu logo")
    fun withoutLogoCount(count: Int) = t("$count logosuz", "$count without logo", "$count fără logo")
    fun radioCount(count: Int) = t("$count radyo", "$count radio", "$count posturi radio")
    val actionsSection get() = t("İşlemler", "Actions", "Acțiuni")

    fun sourcesUpdatedAt(date: String) = t("İnternet kaynakları: $date", "Online sources: $date", "Surse online: $date")
    fun apply(count: Int) = t("Seçilenleri Yaz ($count)", "Write Selected ($count)", "Scrie selecția ($count)")
    val selectAll get() = t("Tümünü Seç", "Select All", "Selectează tot")
    val selectSafe get() = t("Güvenlileri Seç", "Select Safe Ones", "Selectează sigurele")
    val selectNone get() = t("Seçimi Kaldır", "Clear Selection", "Anulează selecția")
    val showMissing get() = t("Logosuz TV kanalları", "TV channels without logo", "Canale TV fără logo")
    val showAll get() = t("Tümü (radyolar dahil)", "All (incl. radio)", "Toate (inclusiv radio)")
    val listSection get() = t("Liste", "List", "Listă")
    val selectionSection get() = t("Seçim", "Selection", "Selecție")
    val updateSection get() = t("Tara ve güncelle", "Scan & update", "Scanare și actualizare")
    val searchHint get() = t("Kanal ara: yazmaya başlayın (ad veya numara)", "Search channels: start typing (name or number)", "Caută canale: începeți să tastați (nume sau număr)")
    fun hiddenMatches(count: Int) = t(
        "Logosu olan veya radyo $count kanal daha eşleşiyor.",
        "$count more matching channels have a logo or are radio.",
        "Încă $count canale potrivite au logo sau sunt radio."
    )
    val searchAll get() = t("Tümünde Ara", "Search All", "Caută în toate")
    fun listCounter(total: Int, selected: Int, suggested: Int) = t(
        "$total kanal · $selected seçili · $suggested önerili",
        "$total channels · $selected selected · $suggested with a suggestion",
        "$total canale · $selected selectate · $suggested cu sugestie"
    )
    val longPressHint get() = t("OK'a basılı tutun: hızlı menü", "Hold OK: quick menu", "Țineți apăsat OK: meniu rapid")
    val writeNow get() = t("Bu Logoyu Şimdi Yaz", "Write This Logo Now", "Scrie acest logo acum")
    val select get() = t("Seç", "Select", "Selectează")
    val unselect get() = t("Seçimden Çıkar", "Unselect", "Deselectează")
    val currentLogo get() = t("Şu anki logo", "Current logo", "Logo-ul actual")
    val currentLogoNone get() = t("Bu kanalın şu an logosu yok.", "This channel has no logo now.", "Acest canal nu are logo acum.")
    val lowQualityMark get() = t("düşük kalite", "low quality", "calitate slabă")
    val lowQualitySmall get() = t("çok küçük, bulanık görünebilir", "very small, may look blurry", "foarte mic, poate părea neclar")
    val lowQualityDark get() = t("koyu ve şeffaf, koyu arka planda zor görünür", "dark and see-through, hard to see on dark backgrounds", "întunecat și transparent, greu de văzut pe fundal întunecat")
    fun preparing(done: Int, total: Int) = t("Logolar hazırlanıyor: $done / $total", "Preparing logos: $done / $total", "Se pregătesc logo-urile: $done / $total")
    val confirmWriteTitle get() = t("Yazmadan önce kontrol", "Check before writing", "Verificare înainte de scriere")
    fun confirmWriteSummary(count: Int) = t("$count logo TV'ye yazılacak.", "$count logos will be written to the TV.", "$count logo-uri vor fi scrise pe televizor.")
    fun replacingWarning(count: Int) = t(
        "Bunların $count tanesi kanalın mevcut logosunun yerine geçecek. Eskileri saklanır; \"Orijinaline Döndür\" ile geri alınabilir.",
        "$count of them replace a logo the channel already has. The old ones are kept and can be put back with \"Restore Original\".",
        "$count dintre ele înlocuiesc un logo pe care canalul îl are deja. Cele vechi se păstrează și pot fi puse la loc cu „Readu la original”."
    )
    fun lowQualityWarning(count: Int) = t(
        "$count logo düşük kaliteli: çok küçük ya da koyu renkli ve şeffaf. TV'de bulanık veya zor görünebilir.",
        "$count logos are low quality: very small, or dark and see-through. They may look blurry or be hard to see on the TV.",
        "$count logo-uri au calitate slabă: foarte mici sau întunecate și transparente. Pot părea neclare sau greu de văzut pe televizor."
    )
    fun prepareFailed(count: Int) = t(
        "$count logo indirilemedi; bunlar atlanacak.",
        "$count logos could not be downloaded and are left out.",
        "$count logo-uri nu au putut fi descărcate și vor fi omise."
    )
    val autoBackupNote get() = t(
        "Henüz yedek olmadığından, yazmadan önce TV'deki tüm logoların yedeği alınacak.",
        "There is no backup yet, so all logos on the TV are backed up before writing.",
        "Încă nu există o copie, așa că toate logo-urile de pe televizor vor fi salvate înainte de scriere."
    )
    val autoBackedUp get() = t(
        "Yazmadan önce tüm logoların yedeği alındı.",
        "All logos were backed up before writing.",
        "Toate logo-urile au fost salvate înainte de scriere."
    )
    fun writeAll(count: Int) = t("Yaz ($count)", "Write ($count)", "Scrie ($count)")
    fun skipLowQuality(count: Int) = t("Düşük Kalitelileri Atla ($count)", "Skip Low Quality ($count)", "Omite calitatea slabă ($count)")
    val noChannelsTitle get() = t("Kanal bulunamadı", "No channels found", "Nu s-au găsit canale")
    val noChannelsExplained get() = t(
        "TV'nin Android kanal listesinde yayın kanalı yok. Kanallar henüz kurulmadıysa önce kanal araması yapın ve \"Kanalları Yeniden Tara\"ya basın. Kanallar kuruluysa bu TV onları üreticinin kendi veritabanında tutuyor olabilir; o zaman yazılan logolar TV'de görünmez.",
        "The TV's Android channel list has no broadcast channels. If no channels are installed yet, run a channel search and press \"Rescan Channels\". If they are installed, this TV probably keeps them in the manufacturer's own database, where written logos would not show.",
        "Lista de canale Android a televizorului nu are canale de difuzare. Dacă nu sunt instalate canale, faceți o căutare de canale și apăsați „Rescanează canalele”. Dacă sunt instalate, probabil televizorul le ține în baza de date proprie a producătorului, unde logo-urile scrise nu s-ar vedea."
    )
    val noChannelAccess get() = t(
        "Bu TV, ADB kabuğunun kanal listesini okumasına izin vermiyor (ACCESS_ALL_EPG_DATA yok). Bu marka/modelde uygulama logoları değiştiremez.",
        "This TV does not let the ADB shell read the channel list (no ACCESS_ALL_EPG_DATA). The app cannot change logos on this brand/model.",
        "Acest televizor nu permite shell-ului ADB să citească lista de canale (fără ACCESS_ALL_EPG_DATA). Aplicația nu poate schimba logo-urile pe această marcă/model."
    )
    val unsupportedTvTitle get() = t("Bu TV Desteklenmiyor", "This TV is Not Supported", "Acest televizor nu este acceptat")
    val unsupportedTvMessage get() = t(
        "Bu TV'nin üretici yazılımı veya sistemi, kanal veritabanına logo eklenmesini desteklemiyor veya izin vermiyor. Uygulama bu cihazda çalışamaz.",
        "This TV's firmware or system does not support or allow writing logos to the broadcast channel database. The app cannot operate on this device.",
        "Sistemul sau firmware-ul acestui televizor nu acceptă sau nu permite scrierea de logo-uri în baza de date a canalelor. Aplicația nu poate funcționa pe acest dispozitiv."
    )
    val unsupportedNoTuner get() = t(
        "Cihazda yayın kanalı tuner girişi bulunamadı.",
        "No broadcast tuner input was found on this device.",
        "Nu s-a găsit nicio intrare de tuner pentru canale de difuzare pe acest dispozitiv."
    )
    val exitApp get() = t("Uygulamadan Çık", "Exit App", "Ieși din aplicație")
    val technicalDetails get() = t("Teknik Detay", "Technical Details", "Detalii tehnice")
    val noMatches get() = t("Aramaya uyan kanal yok.", "No channel matches the search.", "Niciun canal nu corespunde căutării.")

    fun scanFailed(detail: String) = t("Kanallar okunamadı: $detail", "Could not read the channels: $detail", "Canalele nu au putut fi citite: $detail")
    fun writeFailed(detail: String) = t("Logolar yazılamadı: $detail", "Could not write the logos: $detail", "Logo-urile nu au putut fi scrise: $detail")
    val doneTitle get() = t("Tamamlandı", "Done", "Gata")
    val updatedTitle get() = t("Güncellendi", "Updated", "Actualizat")
    val errorTitle get() = t("Bir sorun oluştu", "Something went wrong", "A apărut o problemă")
    val errorHint get() = t(
        "TV'nin ağ üzerinden hata ayıklamasının açık olduğunu ve internet bağlantısını kontrol edip tekrar deneyin. Sorun sürerse TV'yi yeniden başlatın.",
        "Check that the TV's network debugging is on and that it is online, then try again. If the problem persists, restart the TV.",
        "Verificați că remedierea erorilor prin rețea este activată și că televizorul are internet, apoi încercați din nou. Dacă problema persistă, reporniți televizorul."
    )
    val ok get() = t("Tamam", "OK", "Am înțeles")
    fun channelsScanned(count: Int) = t("$count kanal okundu.", "$count channels read.", "$count canale citite.")
    val pleaseWait get() = t("Lütfen bekleyin, TV kapatılmamalı.", "Please wait and keep the TV on.", "Vă rugăm așteptați și nu opriți televizorul.")
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
    val sourceInternal get() = t("TV hafızası", "TV storage", "Memoria TV")
    val filesSource get() = t("Dosyalar", "Files", "Fișiere")
    val pickFromFiles get() = t("Dosyalardan Seç", "Pick from Files", "Alege din fișiere")
    val browseFolders get() = t("Klasörlerde Gez", "Browse Folders", "Răsfoiește dosarele")
    val backToSuggestions get() = t("Önerilere Dön", "Back to Suggestions", "Înapoi la sugestii")
    fun filesTitle(channel: String) = t("Dosyadan logo: $channel", "Logo from a file: $channel", "Logo dintr-un fișier: $channel")
    fun browseTitle(channel: String) = t("Klasörler: $channel", "Folders: $channel", "Dosare: $channel")
    val filesEmpty get() = t(
        "USB bellekte veya TV hafızasında resim bulunamadı. PNG, JPG veya WebP dosyalarını belleğe kopyalayıp takın; dosya adı kanal adıyla aynıysa (ör. \"ATV HD.png\") logo kendiliğinden eşleşir.",
        "No images found on a USB stick or in the TV's storage. Copy PNG, JPG or WebP files to a stick and plug it in; a file named like the channel (e.g. \"ATV HD.png\") is matched automatically.",
        "Nu s-au găsit imagini pe stick-ul USB sau în memoria televizorului. Copiați fișiere PNG, JPG sau WebP pe un stick și conectați-l; un fișier numit ca și canalul (de ex. „PRO TV HD.png”) este asociat automat."
    )
    val noPlaces get() = t(
        "Takılı USB bellek yok ve TV hafızası okunamıyor. Uygulamaya depolama izni verildiğinden emin olun.",
        "No USB stick is plugged in and the TV's storage cannot be read. Make sure the app has the storage permission.",
        "Niciun stick USB conectat, iar memoria televizorului nu poate fi citită. Verificați că aplicația are permisiunea de stocare."
    )
    val folderEmpty get() = t("Bu klasörde resim ya da klasör yok.", "This folder has no images or folders.", "Acest dosar nu are imagini sau dosare.")
    val upFolder get() = t("Üst klasör", "Up one folder", "Dosarul părinte")
    fun fileStats(count: Int) = t("Dosyalar: $count resim", "Files: $count images", "Fișiere: $count imagini")
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
    val appearanceSection get() = t("Görünüm", "Appearance", "Aspect")
    val suggestionsSection get() = t("Öneriler", "Suggestions", "Sugestii")
    val languageDesc get() = t(
        "Seçilene kadar TV'nin dili kullanılır.",
        "Uses the TV's language until you choose one.",
        "Folosește limba televizorului până alegeți una."
    )
    val themeDesc get() = t(
        "\"Sistem\" TV'nin koyu/açık ayarını izler.",
        "\"System\" follows the TV's dark/light setting.",
        "„Sistem” urmează setarea întunecată/luminoasă a televizorului."
    )
    val sourcesDesc get() = t(
        "Açık kaynaklar logo aramasında kullanılır; seçmek açar veya kapatır.",
        "Sources that are on are searched for logos; select one to turn it on or off.",
        "Sursele activate sunt folosite la căutarea logo-urilor; selectați una pentru a o activa sau dezactiva."
    )
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
    val aboutDeveloperTitle get() = t("Geliştiren", "Developer", "Dezvoltator")
    val aboutDeveloper get() = "berkbb"
    val aboutWebsiteTitle get() = t("Web sitesi", "Website", "Site web")
    val aboutWebsite get() = "https://berkbb.github.io"
    val aboutLicenseTitle get() = t("Lisans", "License", "Licență")
    val aboutLicense get() = t(
        "GNU Genel Kamu Lisansı v3.0 (GPL-3.0) ile lisanslanmıştır.",
        "Licensed under the GNU General Public License v3.0 (GPL-3.0).",
        "Licențiat sub GNU General Public License v3.0 (GPL-3.0)."
    )
    val viewLicense get() = t("Lisansı Görüntüle", "View License", "Vezi licența")
    val dialogLicenseTitle get() = t("GNU Genel Kamu Lisansı v3.0", "GNU General Public License v3.0", "Licența Publică Generală GNU v3.0")
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
    fun resetToDefault(count: Int) = t("Tüm Kanalları Orijinaline Döndür ($count)", "Restore All Channels to Original ($count)", "Readu toate canalele la original ($count)")
    val resetOne get() = t("Orijinaline Döndür", "Restore Original", "Readu la original")
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
    val exportBackup get() = t("Yedeği Dışa Aktar", "Export Backup", "Exportă copia")
    val importBackup get() = t("Yedeği İçe Aktar", "Import Backup", "Importă copia")
    val exportTitle get() = t("Yedek nereye kaydedilsin?", "Where should the backup go?", "Unde se salvează copia?")
    val exportDesc get() = t(
        "Logolar ve ayarlar, her dosyanın SHA-256 özetiyle birlikte kaydedilir. USB bellekteki yedek başka bir TV'ye taşınabilir; TV dahili hafızasındaki yedek uygulama silinse de kalır (fabrika ayarlarına dönüşte silinir).",
        "Logos and settings are saved with a SHA-256 checksum of every file. A backup on a USB stick can move to another TV; one in the TV's internal storage survives uninstalling the app (not a factory reset).",
        "Logo-urile și setările se salvează cu suma de control SHA-256 a fiecărui fișier. O copie pe stick-ul USB poate fi mutată pe alt televizor; una în memoria internă a televizorului rămâne și dacă aplicația este dezinstalată (nu și la resetarea din fabrică)."
    )
    val placeUsb get() = t("USB Bellek", "USB Stick", "Stick USB")
    val placeInternal get() = t("TV Dahili Hafıza", "TV Internal Storage", "Memoria internă a TV-ului")
    val exporting get() = t("Yedek kaydediliyor…", "Saving the backup…", "Se salvează copia…")
    val importing get() = t("Yedek alınıyor ve doğrulanıyor…", "Reading and checking the backup…", "Se citește și se verifică copia…")
    val searchingBackups get() = t("Yedekler aranıyor ve doğrulanıyor…", "Looking for backups and checking them…", "Se caută și se verifică copiile…")
    fun exported(count: Int, path: String) = t(
        "$count logoluk yedek ayarlarla birlikte kaydedildi:\n$path\n\nGeri yüklemek için bu uygulamada \"Yedeği İçe Aktar\", ardından \"Yedekten Geri Yükle\"ye basın.",
        "The backup of $count logos was saved with the settings:\n$path\n\nTo bring it back, press \"Import Backup\" in this app, then \"Restore Backup\".",
        "Copia cu $count logo-uri a fost salvată împreună cu setările:\n$path\n\nPentru a o readuce, apăsați „Importă copia” în această aplicație, apoi „Restaurează copia”."
    )
    val internalCopyFailed get() = t(
        "Yedek TV'nin dahili hafızasına kopyalanamadı.",
        "The backup could not be copied to the TV's internal storage.",
        "Copia nu a putut fi copiată în memoria internă a televizorului."
    )
    fun imported(count: Int) = t(
        "$count logoluk yedek alındı. TV'ye yazmak için \"Yedekten Geri Yükle\"ye basın.",
        "The backup of $count logos was taken over. Press \"Restore Backup\" to write it to the TV.",
        "Copia cu $count logo-uri a fost preluată. Apăsați „Restaurează copia” pentru a o scrie pe televizor."
    )
    val settingsRestored get() = t("Yedekteki ayarlar da yüklendi.", "The backup's settings were applied too.", "Au fost aplicate și setările din copie.")
    fun damagedSkipped(count: Int) = t(
        "SHA-256 doğrulaması tutmayan $count logo dosyası bozuk; bunlar atlandı.",
        "$count logo files failed their SHA-256 check and were skipped as damaged.",
        "$count fișiere de logo nu au trecut verificarea SHA-256 și au fost omise ca deteriorate."
    )
    val noUsbStick get() = t(
        "Yazılabilir USB bellek bulunamadı. Belleği takıp TV'nin tanımasını bekleyin.",
        "No writable USB stick was found. Plug one in and wait until the TV has recognised it.",
        "Nu s-a găsit niciun stick USB inscriptibil. Conectați unul și așteptați ca televizorul să-l recunoască."
    )
    val importTitle get() = t("Yedek seç", "Choose a backup", "Alegeți o copie")
    val noBackupsFound get() = t(
        "Yedek bulunamadı. USB bellekte veya TV'nin dahili hafızasında \"ChannelLogosBackup\" klasörü aranır.",
        "No backup was found. A \"ChannelLogosBackup\" folder is looked for on USB sticks and in the TV's internal storage.",
        "Nu s-a găsit nicio copie. Se caută un dosar „ChannelLogosBackup” pe stick-uri USB și în memoria internă a televizorului."
    )
    fun backupFoundLine(logos: Int, matched: Int) = t(
        "$logos logo · $matched kanal bu TV'de bulundu",
        "$logos logos · $matched channels found on this TV",
        "$logos logo-uri · $matched canale găsite pe acest televizor"
    )
    val settingsIncluded get() = t("ayarlar dahil", "settings included", "inclusiv setările")
    val verifiedOk get() = t("SHA-256 doğrulandı: tüm dosyalar sağlam", "SHA-256 verified: all files intact", "SHA-256 verificat: toate fișierele sunt intacte")
    val notVerified get() = t("Doğrulama özeti yok (eski sürüm yedeği)", "No checksums (backup from an older version)", "Fără sume de control (copie dintr-o versiune mai veche)")
    val backupUnusable get() = t(
        "Yedeğin kanal listesi bozuk; bu yedek kullanılamaz.",
        "The backup's channel list is damaged; this backup cannot be used.",
        "Lista de canale a copiei este deteriorată; această copie nu poate fi folosită."
    )
    val channelsChangedWarning get() = t(
        "Dikkat: kanallar çok değişmiş. Yedekteki logoların çoğu bu TV'de eşleşen kanal bulamıyor.",
        "Warning: the channels changed a lot. Most logos in the backup find no matching channel on this TV.",
        "Atenție: canalele s-au schimbat mult. Majoritatea logo-urilor din copie nu găsesc un canal potrivit pe acest televizor."
    )
    fun confirmImportFound(channelsChanged: Boolean) = t(
        "Seçilen yedek bu TV'deki mevcut yedeğin yerine geçecek ve içindeki ayarlar yüklenecek. TV'deki logolar bu adımda değişmez.",
        "The chosen backup will replace this TV's current backup and its settings will be applied. The logos on the TV do not change at this step.",
        "Copia aleasă va înlocui copia actuală a acestui televizor, iar setările din ea vor fi aplicate. Logo-urile de pe televizor nu se schimbă la acest pas."
    ) + if (channelsChanged) "\n\n" + channelsChangedWarning else ""
    fun confirmRestoreMatched(matched: Int, total: Int, channelsChanged: Boolean) = t(
        "Yedekteki $total logodan $matched tanesi, numarası ve adı eşleşen kanallara yazılacak.",
        "$matched of the $total backed-up logos will be written to the channels with the same number and name.",
        "$matched din cele $total logo-uri din copie vor fi scrise pe canalele cu același număr și nume."
    ) + if (channelsChanged) "\n\n" + channelsChangedWarning else ""
    val confirm get() = t("Onayla", "Confirm", "Confirmă")
    val yes get() = t("Evet", "Yes", "Da")
    val no get() = t("Vazgeç", "Cancel", "Renunță")
    val confirmBackup get() = t(
        "Tüm kanalların şu anki logoları yedeklenecek. Önceki yedeğin yerine geçer.",
        "The current logos of all channels will be backed up, replacing the previous backup.",
        "Logo-urile actuale ale tuturor canalelor vor fi salvate, înlocuind copia anterioară."
    )
    fun confirmReset(count: Int) = t(
        "Bu uygulamanın değiştirdiği $count kanal ilk hâline döndürülecek: önceki logosu geri konacak, logosu yoksa silinecek.",
        "The $count channels this app changed will go back to how they were: their earlier logo is put back, or removed if they had none.",
        "Cele $count canale modificate de această aplicație vor reveni la starea inițială: logo-ul anterior este pus la loc sau șters dacă nu aveau unul."
    )

    val shortcutClear get() = t("Kaldır", "Clear", "Șterge")
    val shortcutSelectSafe get() = t("Güvenlileri Seç", "Select Safe", "Selectează sigure")
    val shortcutToggleView get() = t("Görünüm", "Toggle View", "Comutare")
    val shortcutWrite get() = t("Yaz", "Write", "Scrie")
    val shortcutsSection get() = t("Kumanda Kısayolları", "Remote Shortcuts", "Scurtături telecomandă")


    val checkUpdates get() = t("Güncellemeleri Denetle", "Check for Updates", "Verifică actualizările")
    val checkingUpdates get() = t("Güncellemeler denetleniyor…", "Checking for updates…", "Se verifică actualizările…")
    val upToDateTitle get() = t("Güncelsiniz", "Up to date", "Actualizat")
    fun upToDate(current: String) = t(
        "Yeni sürüm yok; en güncel sürümü (v$current) kullanıyorsunuz.",
        "No new version; you are using the latest version (v$current).",
        "Nicio versiune nouă; utilizați cea mai recentă versiune (v$current)."
    )
    val updateAvailableTitle get() = t("Yeni sürüm mevcut", "New version available", "Versiune nouă disponibilă")
    fun updateAvailable(version: String, current: String) = t(
        "v$version yayınlandı (yüklü: v$current). Yeni APK'yı GitHub'daki sürüm sayfasından indirip kurabilirsiniz.",
        "v$version is out (installed: v$current). Download and install the new APK from the release page on GitHub.",
        "v$version a fost lansată (instalată: v$current). Descărcați și instalați noul APK de pe pagina versiunii de pe GitHub."
    )
    val updateFailed get() = t("Güncelleme denetlenemedi", "Could not check for updates", "Nu s-au putut verifica actualizările")
    val releasesUnavailable get() = t(
        "GitHub'da yayınlanmış bir sürüm bulunamadı ya da depo herkese açık değil.",
        "No published release was found on GitHub, or the repository is not public.",
        "Nu a fost găsită nicio versiune publicată pe GitHub sau depozitul nu este public."
    )
    val updateCheckOffline get() = t(
        "GitHub'a ulaşılamadı. TV'nin internet bağlantısını denetleyip tekrar deneyin.",
        "GitHub could not be reached. Check the TV's internet connection and try again.",
        "GitHub nu a putut fi accesat. Verificați conexiunea la internet a televizorului și încercați din nou."
    )
    fun updateAvailableInstall(version: String, current: String) = t(
        "v$version yayınlandı (yüklü: v$current). Uygulama şimdi kendini indirip kurabilir.",
        "v$version is out (installed: v$current). The app can download and install itself now.",
        "v$version a fost lansată (instalată: v$current). Aplicația se poate descărca și instala acum."
    )
    val updateNow get() = t("Şimdi Güncelle", "Update Now", "Actualizează acum")
    val later get() = t("Sonra", "Later", "Mai târziu")
    fun downloadingUpdate(version: String, percent: Int) = t(
        "v$version indiriliyor: %$percent",
        "Downloading v$version: $percent%",
        "Se descarcă v$version: $percent%"
    )
    val updateDownloadFailed get() = t(
        "Yeni sürüm indirilemedi ya da doğrulanamadı. İnternet bağlantısını denetleyip tekrar deneyin.",
        "The new version could not be downloaded or verified. Check the internet connection and try again.",
        "Noua versiune nu a putut fi descărcată sau verificată. Verificați conexiunea la internet și încercați din nou."
    )
    val updateInstallFailedTitle get() = t("Güncelleme kurulamadı", "Update not installed", "Actualizarea nu a fost instalată")
    fun updateInstallFailed(reason: String) = t(
        "Sistem yükleyicisi güncellemeyi kurmadı. $reason",
        "The system installer did not install the update. $reason",
        "Programul de instalare al sistemului nu a instalat actualizarea. $reason"
    ).trim()
    val installPermissionTitle get() = t("İzin gerekli", "Permission required", "Permisiune necesară")
    val installPermissionNeeded get() = t(
        "Açılan ayarda Kanal Logoları için \"Bilinmeyen uygulamaları yükle\" iznini açın, sonra Şimdi Güncelle'ye tekrar basın.",
        "In the setting that opens, allow \"Install unknown apps\" for Channel Logos, then press Update Now again.",
        "În setarea care se deschide, permiteți „Instalare aplicații necunoscute” pentru Logo-uri canale, apoi apăsați din nou Actualizează acum."
    )
    val confirmInstallTitle get() = t("Onay bekleniyor", "Waiting for confirmation", "Se așteaptă confirmarea")
    val confirmInstall get() = t(
        "TV'de açılan kurulum onayında YÜKLE'ye basın. Güncellemeden sonra uygulama yeniden açılır.",
        "Press INSTALL in the installation prompt on the TV. The app reopens after the update.",
        "Apăsați INSTALARE în solicitarea de instalare de pe televizor. Aplicația se redeschide după actualizare."
    )
    fun updateNote(version: String) = t(
        "Kanal Logoları v$version yayınlandı: Hakkında > Güncellemeleri Denetle ile kurabilirsiniz.",
        "Channel Logos v$version is out: install it with About > Check for Updates.",
        "Logo-uri canale v$version a fost lansată: instalați-o din Despre > Verifică actualizările."
    )
    val signatureConflict get() = t(
        "Yüklü Kanal Logoları farklı bir anahtarla imzalanmış; yeni sürüm üzerine kurulamaz. Uygulamayı kaldırıp yeni sürümü yeniden kurun (ayarlar ve logo yedekleri uygulama verisiyle birlikte silinir; TV'ye yazılan logolar kalır).",
        "The installed Channel Logos is signed with a different key, so the new version cannot install over it. Uninstall the app, then install the new version (settings and logo backups are removed with the app data; logos written to the TV stay).",
        "Logo-uri canale instalat este semnat cu o altă cheie, deci noua versiune nu se poate instala peste el. Dezinstalați aplicația, apoi instalați noua versiune (setările și copiile de siguranță se șterg cu datele aplicației; logo-urile scrise pe televizor rămân)."
    )

    val previewBeforeWrite get() = t("Değiştirilecek Logolar", "Logos to be Written", "Logo-uri de scris")
    fun andMoreChannels(count: Int) = t("+ $count kanal daha", "+ $count more channels", "+ încă $count canale")

    val radioMark get() = t("Radyo", "Radio", "Post radio")
    val logoInfoTitle get() = t("Logo Bilgisi", "Logo Details", "Detalii logo")
    val logoSource get() = t("Kaynak", "Source", "Sursă")
    val logoReference get() = t("Dosya / Referans", "File / Reference", "Fișier / Referință")
    val logoTier get() = t("Eşleşme Seviyesi", "Match Tier", "Nivel potrivire")
    val logoStrength get() = t("Eşleşme Gücü", "Match Strength", "Putere potrivire")
    val logoQualityStatus get() = t("Görsel Kalitesi", "Visual Quality", "Calitate vizuală")
    val logoQualityOk get() = t("Sorunsuz (TV için uygun)", "Good (Fits TV well)", "Bun (Potrivit pentru TV)")
    val tierCountry get() = t("Ülke Kataloğu", "Country Catalogue", "Catalog național")
    val tierInternational get() = t("Uluslararası Katalog", "International Catalogue", "Catalog internațional")
    val tierOther get() = t("Diğer Ülkeler", "Other Countries", "Alte țări")

    val storageSection get() = t("Depolama ve Önbellek", "Storage and Cache", "Stocare și cache")
    val cacheSizeTitle get() = t("Önbellek Boyutu", "Cache Size", "Dimensiune cache")
    fun cacheSizeDesc(size: String) = t("İndirilen logo önbelleği: $size", "Downloaded logo cache: $size", "Cache de sigle descărcate: $size")
    val cacheClear get() = t("Önbelleği Boşalt", "Clear Cache", "Golește memoria cache")
    val cacheCleared get() = t("Logo önbelleği boşaltıldı.", "Logo cache cleared.", "Memoria cache a siglelor a fost golită.")

    val exportChannelsCsv get() = t("Kanal Listesini Dışa Aktar (CSV)", "Export Channel List (CSV)", "Exportă lista de canale (CSV)")
    val exportingChannels get() = t("Kanal listesi dışa aktarılıyor…", "Exporting channel list…", "Se exportă lista de canale…")
    fun exportedChannels(count: Int, path: String) = t(
        "$count kanalın listesi dışa aktarıldı:\n$path",
        "Exported list of $count channels to:\n$path",
        "S-a exportat lista cu $count canale la:\n$path"
    )
    val channelsCopyFailed get() = t(
        "Kanal listesi TV'nin dahili hafızasına kopyalanamadı.",
        "The channel list could not be copied to the TV's internal storage.",
        "Lista de canale nu a putut fi copiată în memoria internă a televizorului."
    )

    val sortSection get() = t("Kanal Sıralama", "Channel Sorting", "Sortare canale")
    val sortNumber get() = t("Numara", "Number", "Număr")
    val sortName get() = t("İsim (A-Z)", "Name (A-Z)", "Nume (A-Z)")
    val sortMissingFirst get() = t("Eksikler Önde", "Missing First", "Lipsă în față")

    val filterSection get() = t("Medya Filtresi", "Media Filter", "Filtrare media")
    val filterAllMedia get() = t("Tüm Medya", "All Media", "Toate mediile")
    val filterTvOnly get() = t("Yalnızca TV", "TV Only", "Doar TV")
    val filterRadioOnly get() = t("Yalnızca Radyo", "Radio Only", "Doar Radio")

    val zoomPreview get() = t("Logoyu Büyüt", "Enlarge Logo", "Mărește sigla")

    val dvbParams get() = t("DVB Kimliği (SID/TSID/ONID)", "DVB Identifiers (SID/TSID/ONID)", "Identificatori DVB (SID/TSID/ONID)")

    val piconsStyleSetting get() = t("Picons Logo Stili", "Picons Logo Style", "Stil logo Picons")
    val piconsStyleDesc get() = t(
        "Koyu renkli TV bilgi çubuğu için açık veya koyu logo varyantı.",
        "Light or dark logo variant for the TV's dark channel bar.",
        "Variantă deschisă sau întunecată pentru bara TV."
    )
    val piconsStyleDefault get() = t("Varsayılan", "Default", "Implicit")
    val piconsStyleLight get() = t("Açık (Light)", "Light", "Luminoasă")
    val piconsStyleDark get() = t("Koyu (Dark)", "Dark", "Întunecată")

    val selectDrawnLogos get() = t("Logosuzlara Yazılı Ata", "Assign Drawn to Missing", "Atribuie desenate celor lipsă")

    val restartTvSetting get() = t("Canlı TV Arayüzünü Yenile", "Refresh Live TV Screen", "Reîmprospătează ecranul TV")
    val restartTvDesc get() = t(
        "Yeni logoların hemen görünmesi için TV kanal arayüzü arka planda yeniden başlatılır.",
        "Restarts the TV's channel screen in the background so new logos appear at once.",
        "Repornește ecranul de canale în fundal pentru a afișa imediat siglele noi."
    )

    val previewBeforeRestore get() = t("Geri Yüklenecek Logolar", "Logos to be Restored", "Logo-uri de restaurat")
    fun restorePreviewDesc(matched: Int, total: Int) = t(
        "Yedekteki $total logodan $matched tanesi eşleşti ve geri yüklenecek.",
        "$matched of $total backed-up logos matched and will be restored.",
        "$matched din $total sigle salvate s-au potrivit și vor fi restaurate."
    )

    val searchCatalogs get() = t("Kataloglarda Ara", "Search Catalogs", "Căutare în cataloage")
    val searchCatalogHint get() = t("Kanal adı veya anahtar kelime yazın...", "Type channel name or keyword...", "Scrieți numele canalului sau cuvânt-cheie...")
    val resolution get() = t("Çözünürlük", "Resolution", "Rezoluție")
    val healthReportTitle get() = t("Doluluk ve Sağlık Raporu", "Coverage & Health Report", "Raport de acoperire și stare")
    val coverageRate get() = t("Logo Doluluk Oranı", "Logo Coverage Rate", "Rată de acoperire logo")
    val safeMatchesCount get() = t("Güvenli Logo Eşleşmeleri", "Safe Logo Matches", "Potriviri sigure de logo")
    val catalogsOverview get() = t("Katalog Veri Tabanları", "Catalogs Database", "Baze de date cataloage")
    val ignoreChannel get() = t("Kanalı Yoksay", "Ignore Channel", "Ignoră canalul")
    val unignoreChannel get() = t("Yoksaymayı Kaldır", "Unignore Channel", "Anulează ignorarea")
    val ignoredChannelsSetting get() = t("Yoksayılan Kanallar", "Ignored Channels", "Canale ignorate")
    fun ignoredChannelsDesc(count: Int) = t(
        "$count kanal eksik listesinde gizleniyor.",
        "$count channels hidden from missing list.",
        "$count canale ascunse din lista celor lipsă."
    )
    val resetIgnored get() = t("Yoksayılanları Temizle", "Clear Ignored", "Șterge canalele ignorate")
    val ignoredMark get() = t("Yoksayıldı", "Ignored", "Ignorat")
    val alphabetJumpHint get() = t(
        "Harf atlamak için kumandadan >> / << tuşlarını kullanabilirsiniz.",
        "Use >> / << remote keys to jump between letter groups.",
        "Folosiți tastele >> / << de pe telecomandă pentru a sări între litere."
    )
}
