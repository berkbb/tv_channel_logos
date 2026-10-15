# Development Guide

## 1. Prerequisites

- JDK 21, Android SDK with platform 37 (compileSdk 37.2) and build tools 36.
- Gradle wrapper 9.7 / AGP 9.4 (Kotlin built into AGP, Compose compiler plugin 2.2.10).
- `local.properties` with `sdk.dir=/path/to/Android/sdk` (not committed).
- A TV with network debugging on (Settings → Developer options), reachable with `adb connect TV:5555`.

## 2. Code map

```
app/src/main/java/com/babadogan/channellogos/
├── MainActivity.kt              # Compose host; asks for the storage permission (logo files)
├── tool/TvLogoTool.java         # Runs ON the TV as the shell user (app_process); list / write
├── tv/TvShell.kt                # dadb connection to localhost:5555, runs TvLogoTool, retries, refreshes the TV UI
├── logos/NameMatcher.kt         # Name → key, channel keys (HD/SD/TV/TİCARİ variants), logo keys
├── logos/LogoMatching.kt        # Ranking, safe matches, candidates for picking by hand
├── logos/LogoSources.kt         # Files (USB + TV storage), TV table, GitHub, picons, iptv-org sources; LogoImage (fit / SVG / drawn logo / quality)
├── logos/LogoQuality.kt         # Flags small logos and dark logos on transparency (pure pixel maths)
├── logos/ForeignChannels.kt     # Does the TV carry another country's channels (Country.EXTRA)?
├── backup/LogoBackups.kt        # Originals (for "Reset to Default") and the logo backup snapshot
├── settings/AppSettings.kt      # Settings + SharedPreferences store
└── ui/
    ├── MainViewModel.kt         # State, tasks (busy → full-screen progress, Outcome → full-screen result)
    ├── MainScreen.kt            # Side panel, searchable channel list, dialogs (picker, settings, about)
    ├── ChannelSearch.kt         # Type-to-filter: name keys word by word, or channel number prefix
    └── Strings.kt               # TR/EN/RO texts; locale for dates and upper case
```

## 3. Talking to the TV

The TV provider lets only the system and the shell (which holds `ACCESS_ALL_EPG_DATA`) change other
inputs' channels. The app:

1. connects to the TV's adbd with its own RSA key (`files/adb/adbkey`, the TV asks once);
2. runs `app_process -Djava.class.path=<own APK> /system/bin com.babadogan.channellogos.tool.TvLogoTool …`;
3. `TvLogoTool` reaches the TV provider like the `content` command does (`IActivityManager.getContentProviderExternal`
   and the hidden `IContentProvider`), with arguments matched by type so Android 8 to 12 all work.

Commands and output:

| Command | Output |
|---------|--------|
| `list` | one line per channel: `id \t number \t name \t inputId \t logoBytes \t serviceType \t onid \t tsid \t sid` |
| `write ID PNG [ID PNG …]` | `OK id` or `FAIL id reason` per channel |
| `dump ID [ID …]` | `id \t base64 PNG` for each channel that has a logo |
| `remove ID [ID …]` | `OK id` or `FAIL id reason`; the channel is left without a logo |

PNG paths must be readable by the shell: the app puts them in its external files directory
(`/storage/emulated/0/Android/data/com.babadogan.channellogos/files/logos`). `adb push` is not used;
on Android 8 adbd dropped the connection during sync transfers.

After a write the Philips live TV app is restarted (`am force-stop org.droidtv.playtv`): it caches
logos by URI (`ZapbarUtils: getBitmapFromUri() in cache`) and ignores the provider's change notice.
`am kill` does not stop it (system service process).

Try the helper by hand:

```bash
APK=$(adb shell pm path com.babadogan.channellogos | sed 's/package://')
adb shell app_process -Djava.class.path=$APK /system/bin com.babadogan.channellogos.tool.TvLogoTool list | head
```

## 4. Logo sources and caches

| Source | Where it comes from | Cached as |
|--------|--------------------|-----------|
| Files | `/storage/<volume>/…`, then the TV's storage (depth 3, ≤ 3000 images; hidden files and `Android`, `LOST.DIR`, `ChannelLogosBackup` skipped) | rescanned on every scan |
| TV table | `com.example.tvcontentprovider` APK: `assets/Logos/ChannelLogoDatabase.db` + `assets/Logos/sys/<id>.png` | `files/tv-logo-index-<country>-<apk size>-<apk date>.tsv` |
| GitHub | `api.github.com/repos/tv-logo/tv-logos/contents/countries/<folder>` | `files/github-<folder>.json` (1 day), images in `cache/github/` |
| picons | `raw.githubusercontent.com/picons/picons/master/build-source/{srp,snp}.index`; logos `build-source/logos/<name>.default.png`, else `.svg` (drawn with AndroidSVG) | `files/picons-srp.index`, `files/picons-snp.index` (1 day), images as PNG in `cache/picons/` |
| iptv-org | bundled `assets/iptv-org-channels.tsv.gz` (name, logo URL, country); `iptv-org.github.io/api/logos.json` + `channels.json` (streamed, ~14 MB) in the background | `files/iptv-org-channels.tsv` (refreshed in the background after 7 days), images in `cache/iptv/` |

"Update Logos from the Internet" ignores the ages and drops the downloaded images.

Refresh the bundled iptv-org snapshot before a release: `python3 tools/iptv_org_snapshot.py`.

**Other countries' channels:** `Country.EXTRA` (today Romania) is added when `ForeignChannels`
finds at least three channels whose names only that country's GitHub folder knows. Its GitHub
folder, iptv-org rows and picons satellites are then ranked `INTERNATIONAL` (after the TV's own
country, never preselected).

## Backups

`LogoBackups` keeps the snapshot in `files/backup/snapshot`. A backup folder (the snapshot, a USB
copy, or `/sdcard/ChannelLogosBackup`) holds:

| File | Content |
|------|---------|
| `manifest.tsv` | `number \t name \t file` per logo; restore matches channels by number and name |
| `<id>.png` | the logos |
| `backup.properties` | `created`, `app_version` and `setting.*` (exports only) |
| `checksums.sha256` | SHA-256 of every file above, `sha256sum` format; a damaged manifest refuses the backup, damaged logos are skipped |

USB exports go to the app's own folder on the stick (no permission needed). The internal copy is
written to app storage and copied by the shell user (`TvShell.copyFolder`), and read back the
same way, since apps may not use shared storage freely on newer Android versions.

## 5. Matching rules

- **Keys:** lowercase ASCII letters and digits; every Turkish i (I, İ, ı) becomes `i`, accents are
  dropped, `&` → `and`, `+` → `plus`. A plain character loop: Normalizer and regex cost ~1 ms per
  name on a TV CPU and the TV table has ~10,000 names.
- **Channel keys, best first:** exact; without a trailing HD/SD/FHD/UHD/4K; then with/without a
  trailing "tv". The word "TİCARİ" (commercial feed) is ignored.
- **Logo keys:** each logo is filed under its key and, if it ends in a quality marker, without it.
- **Ranking:** catalogue tier (TV's country 0, international 1, other 2) → closeness → source
  (USB, TV, GitHub, picons, iptv-org, drawn). The tier comes first: the international "ShowHD" is a
  different channel from Turkey's "Show TV".
- **Safe (preselected):** tier 0, not a loose match, source USB/TV/GitHub/picons.
- **picons by service:** the channel's `service_id`/`transport_stream_id`/`original_network_id`
  as uppercase hex `SID_TSID_ONID` is looked up in `srp.index` (namespace dropped; the provider
  does not say which satellite). If satellites disagree, the country's preferred namespace wins
  (Türkiye: `1A40000`, 42°E), otherwise the service is skipped. A service match is tier 0 and
  exact only if the picon's name and the channel's share a base; otherwise it is offered as a
  loose international match. `snp.index` names are tier 1 (international).

## 6. Testing

```bash
./gradlew :app:testDebugUnitTest
```

Unit tests cover name keys, matching and ranking (with the real-world cases above) and that every
UI text exists in every language. On the TV, measure with the **release** build: a debuggable build
runs several times slower (the TV table took 13 s instead of 0.5 s).

Useful logcat filter: `adb logcat ChannelLogos:V AndroidRuntime:E '*:S'`.

## 7. Localization

All texts live in `ui/Strings.kt` as `t(turkish, english, romanian)` getters; the language is
Compose state, so changing it in Settings redraws at once. A TV in any other language gets English.
Dates (`Strings.dateTime`) and upper case (`uppercase(Strings.locale)`) follow the chosen language,
not the TV's: never call `DateFormat` or `uppercase()` without it. Add new texts to all three languages; `StringsTest` fails if one is blank or if a text is
accidentally the same in two languages (proper names and shared words like "Sistem" are listed
there).
