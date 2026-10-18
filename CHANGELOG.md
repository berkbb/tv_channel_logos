# Changelog

All notable changes to **Channel Logos** (`com.berkbb.channellogos`) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.4.1] - 2026-10-18

### Added
- **Search catalogues in logo picker**: added free-text query search in the manual logo picker to instantly search across all enabled catalogues (TV, GitHub, picons, iptv-org, and USB files) for channels with non-standard broadcast names.
- **Logo resolution and dimensions display**: the Logo Details (INFO) window and the enlarged zoom view now show exact pixel dimensions (`W × H px`) directly from the decoded image bitmaps.
- **Coverage & health report**: clicking the status badges in the header or the new report button opens an overview displaying channel coverage %, TV vs Radio breakdown, safe match counts, catalog databases sizes, and ignored channels.
- **Ignore channel from missing list**: holding OK on a channel opens the quick menu with an option to ignore/unignore channels (e.g. promo, test or placeholder channels), preventing them from cluttering the Missing view; includes a reset option in Settings.
- **Alphabet jump in channel list**: pressing remote Fast-Forward/Next (`>>`) or Rewind/Previous (`<<`) keys jumps smoothly to the next or previous letter group in the channel list.

---

## [1.4.0] - 2026-10-18

### Added
- **Storage and cache management**: Settings displays the exact disk space used by downloaded logo caches (picons,
  GitHub, iptv-org) and offers a one-click "Clear Cache" action to free up limited TV storage.
- **Export channel list as CSV**: added "Export Channel List (CSV)" to the Backup window to output a complete channel
  report (channel number, name, type, existing logo status, suggested logo, source and match strength) to USB or internal storage.
- **Channel list sorting modes**: added quick sorting controls on the action panel to reorder channels by Number
  (broadcast order), Alphabetically (A-Z), or Missing First (unbadged channels placed at top).
- **Media type filtering**: added dedicated media filter buttons (All Media, TV Only, Radio Only) to work seamlessly
  alongside the Missing/All view toggle.
- **Zoom logo preview**: in the Logo Details (INFO) window, users can click the preview card or press "Enlarge Logo"
  to inspect a large, centered preview tile with checkerboard background and asset metadata.
- **DVB broadcast identifiers in logo inspection**: the Logo Details (INFO) window now decodes and displays
  the channel's DVB triplet (`SID`, `TSID`, `ONID`) in decimal and hex formats for easy frequency verification.
- **Picons logo style variant**: added a Picons Style setting (Default, Light, Dark) to download and render
  optimized light or dark logo variants tailored for the TV's dark channel bar (OSD).
- **Batch assign drawn typography logos**: added a quick action in the selection panel to instantly assign clean,
  generated typography logos to all channels currently lacking any catalog match.
- **Live TV UI restart toggle**: added a setting to control whether the TV's live channel screen is automatically
  restarted in the background after writing to refresh memory caches immediately without rebooting the TV.
- **Restore backup visual preview**: restoring a backup now presents a full preview screen displaying cards for
  the matched channels and their restored logos before committing them to the TV database.

---

## [1.3.0] - 2026-10-18

### Changed
- **New look, shared with Pitwall Updater and AynaCast:** darker background and cards, an accent
  bar before the title and window titles, small grey captions, and buttons that fill light with
  dark text when focused (the chosen option is filled blue). Buttons carry symbols (⚙️ ℹ️ 💾 ✓ ↺).
- **Simpler main screen with a taller channel list:** the title sits at the top with Backup,
  Settings and About on the right, and small badges under it show the channel, with-logo and
  without-logo counts and the connection. The actions and list/selection choices are a narrow
  panel beside the channel list; channel rows are slimmer, so more of them fit.
- The sources line (logo counts per source, last update) moved to Settings → Sources.
- **Backup** (back up, restore, export, import) moved from the side panel into its own window.
- **Settings, About, Pick a logo and Import open full screen** with a close button in the corner
  and Close at the bottom; questions and the quick menu stay small windows.
- **Language setting:** only Türkçe, English and Română; until one is chosen the TV's language is
  used and shown as chosen ("System" is gone).
- **About:** developer (berkbb) and website cards as in Pitwall Updater; the company line is gone.
- **Package identifier**: standardized to `com.berkbb.channellogos`.

### Added
- **Full-screen read-only license viewer**: like in Pitwall Updater, the About window now features a
  "View License" button that opens a full-screen, D-pad scrollable viewer displaying the complete
  GNU General Public License v3.0 text (`assets/LICENSE`) in monospace formatting.
- **Unsupported TV lock screen**: when run on TVs or devices that do not support TV broadcast logos or
  block ADB shell access to the channel database, the app displays a full-screen notification and locks
  itself, preventing broken operations and offering a single Exit action.
- **Active logo endpoint probe**: proactively probes the TV Provider and channel logo URI (`/channel/1/logo`)
  on launch via `TvLogoTool probe` to detect TVs without logo support upfront.
- **Remote hardware navigation & number jump**: use Channel +/- or Page Up/Down on the TV remote to scroll
  through channel lists by page; typing digits jumps directly to matching channel numbers.
- **Remote color key shortcuts**: TV remote color keys (Red: clear/deselect, Green: select safe, Yellow: toggle
  missing/all view, Blue: write selection) with a legend in the action panel.
- **Checkerboard transparency background**: subtle checkerboard tile behind all logo previews ensures transparent
  PNGs with white text or borders are clearly visible.
- **In-app update checker**: About window includes a "Check for Updates" action querying GitHub Releases, and
  surfaces a badge in the header when a newer version is available.
- **Write confirmation visual preview**: the confirmation screen renders preview cards showing sample channels
  and their new logos before writing to the TV database.
- **Automatic USB mount & unmount detection**: registers a media broadcast receiver that automatically refreshes
  external file logos when USB storage is connected or disconnected.
- **Radio channel badges**: radio broadcast channels display a distinct 📻 radio indicator in channel list rows.
- **Remote INFO key logo details**: pressing INFO or GUIDE on the remote (or via the row quick menu) displays
  detailed logo matching information (source, reference, tier, match strength and quality).
- **Automatic USB backup mirroring**: whenever an automatic or manual backup snapshot is taken, a duplicate is
  mirrored to mounted USB storage (`ChannelLogosBackup`) for off-device safety.
- **Logos from the TV's storage**: images in the TV's own storage are matched by name like those
  on a USB stick ("USB" in the sources setting is now "Files"). App data (`Android`), lost files
  and the logo backups are not searched; for the same name a stick's file wins.
- **Folder browser**: "Change" → "Browse Folders" opens the USB sticks and the TV's storage and
  walks their folders, so any image can be picked whatever its name. Back goes up a folder.
  "Pick from USB" is now "Pick from Files" and lists the images of both places.

### Fixed
- **USB sticks prepared on a Mac**: hidden files (macOS's `._name.png` metadata) are no longer
  taken for logos. They had the same name as the real logo and hid it, and showed up as broken
  images in "Pick from USB".
- Large photos are decoded reduced before they are fitted into the logo box.

---

## [1.2.0] - 2026-10-14

### Added
- **Backup to USB or the TV's internal storage**: "Export Backup" asks where to save; the internal
  copy (`/sdcard/ChannelLogosBackup`, written by the shell user) survives uninstalling the app.
  The backup now carries the settings too, and "Import Backup" applies them.
- **SHA-256 verification**: every backup gets a `checksums.sha256` (the `sha256sum` format, so it
  can be checked on a computer). Importing and restoring skip logos that fail it; a backup whose
  channel list fails it is refused. Backups of 1.1.0 and older have no checksums and are marked
  "not verified".
- **Changed channels warning**: "Import Backup" lists every backup found (place, date, logo count,
  how many channels match this TV, check result); when fewer than 80 % of its logos find their
  channel, the list and the confirmations warn that the channels changed a lot.

### Changed
- "Restore Backup" now says how many of the backed-up logos find their channel before writing.

---

## [1.1.0] - 2026-10-14

### Added
- **picons source** ([picons/picons](https://github.com/picons/picons), GPL-3.0, downloaded on the
  TV, not bundled): logos linked to DVB services. `TvLogoTool list` now also prints each channel's
  original network, transport stream and service ID; a picon linked to the channel's own service
  (`SID_TSID_ONID`, Türksat 42°E preferred when satellites disagree) is preselected if its name
  agrees with the channel's. picons' service-name index is a non-preselected fallback. SVG logos
  are drawn with AndroidSVG. Can be turned off in Settings.
- **Romanian** UI language (Settings → Language → Română, or a TV set to Romanian), with the app
  name "Logo-uri canale".
- **Channel search**: a search box above the channel list filters it as you type (name or number;
  case, accents and Turkish i's ignored). Typing a letter or digit anywhere on the main screen
  jumps into the box; Escape clears it. When the current view hides matching channels, a button
  offers to search all channels.
- **List counter**: "47 channels · 12 selected · 30 with a suggestion" above the list.
- **Quick menu**: holding OK on a channel row (or a long touch) offers "Write This Logo Now",
  select/unselect, "Change" and, for changed channels, "Restore Original".
- **Current logo in the picker**: "Change" shows the logo the channel has now next to the candidates.
- **Check before writing**: logos are downloaded first, then a full-screen check says how many
  will be written, how many replace an existing logo, which look poor and which could not be
  downloaded; "Skip Low Quality" writes only the good ones.
- **Logo quality**: very small logos (longest side under 100 px, e.g. 52×39 thumbnails) and dark
  logos on a transparent background are marked "low quality" in the list and the picker, never
  count as safe, and are left out by "Select Safe Ones".
- **Automatic backup**: the first write on a TV without a backup backs up all its logos first.
- **Backup on USB**: "Export Backup to USB" copies the backup to the stick
  (`Android/data/com.berkbb.channellogos/files/ChannelLogosBackup`); "Import Backup from USB"
  reads it there or from a `ChannelLogosBackup` folder at the top of a stick, on this or another TV.
- **Romanian channels on a non-Romanian TV**: when at least three channels match names that only
  Romania's catalogue knows, Romania's GitHub folder, iptv-org channels and picons satellites
  (Thor 0.8°W, Hellas Sat 39°E, Eutelsat 16°E, Astra 23.5°E) are searched too, ranked after the
  TV's own country and never preselected. Nothing changes for TVs without Romanian channels.
- **No channels / no access explained**: an empty Android channel list (channels not installed,
  or kept in the manufacturer's own database) and TVs whose shell may not read the channel list
  (no `ACCESS_ALL_EPG_DATA`) now get a full-screen explanation instead of an empty list.
- **Full-screen progress and results**: while channels are read, logos written, backed up,
  restored or sources updated, a progress ring with the current step covers the screen; the
  result then opens full screen ("Done"/"Updated", or "Something went wrong" with the reason and
  what to check). Replaces the status line at the bottom of the side panel, which did not fit on
  the screen and made the app look frozen.

### Changed
- "Reset to Default" is now **"Restore All Channels to Original (N)"** and sits right under
  "Write Selected" instead of at the bottom of the side panel.
- **Tidier side panel**: buttons grouped under small headings (List, Selection, Scan & update,
  Backup) with related buttons side by side at equal width, the two main actions full width at the
  top, and the subtitle hidden once connected, so nearly everything fits on a 1080p screen.
- **Settings and About redesigned**: each setting is a card with its name and explanation on the
  left and its choices on the right, grouped under Appearance and Suggestions; logo sources are
  single toggles (✓ when on). About shows version, license and company in the header and the
  texts as cards in two columns. Both dialogs have a Close button for the remote.
- Build output, `local.properties` and `.DS_Store` files are no longer tracked (new `.gitignore`).
- **Faster first start**: iptv-org's channel list ships with the app as a snapshot
  (`assets/iptv-org-channels.tsv.gz`, made by `tools/iptv_org_snapshot.py`) instead of a ~20 s
  download and parse on the TV; a fresh list is downloaded in the background once a week and used
  from the next start. The list is kept for all countries, so tiers no longer need a rebuild.
- **Other brands' logo cache**: after writing, the app that opens TV channels (found through
  Android's "view channel" intent) is restarted as well as Philips' live TV app; the tuner
  inputs' own packages are never stopped.
- "Reset to Default" in the picker is now "Restore Original".

### Fixed
- Dates and section headings followed the TV's language instead of the chosen one: a Romanian UI
  on a Turkish TV showed Turkish month names and a dotted "İ" in upper case ("SELECȚİE").

### Known issues
- Android 11+ "Wireless debugging" (pairing code) is still not supported; see ROADMAP.md for why
  pairing from the TV itself is blocked.
- The app's name in the TV's app list follows the TV's language, not the language chosen in the
  app (Android shows it from the APK's resources).

### Notes
- Arçelik (MediaTek) TVs were checked as a logo source: they show and store no broadcast-channel
  logos, and their shell lacks `ACCESS_ALL_EPG_DATA` (ROADMAP.md).

---

## [1.0.0] - 2026-10-12

### Added
- **Channel scan:** lists the TV's broadcast (tuner) channels with their logo state in one pass,
  through `TvLogoTool` run as the shell user over the TV's own ADB (dadb on `localhost:5555`).
  Radio stations are recognised (`SERVICE_TYPE_AUDIO`) and listed only under "All".
- **Logo sources:** USB stick (by file name), the logo table built into Philips firmware, GitHub
  tv-logo/tv-logos (country, international, UK and US folders), iptv-org's channel database, and a
  logo drawn from the channel name for channels no source knows.
- **Matching:** names compared as ASCII keys with Turkish letters folded; HD/SD/FHD/UHD/4K and
  "TİCARİ" handled on both channel and logo names; "TV" suffix tried both ways. Ranking by
  catalogue (the TV's country, international, others), then closeness, then source.
- **Safe preselection:** only close matches from USB, the TV table and GitHub's country folder are
  preselected; foreign catalogues, iptv-org links, similar names and drawn logos are marked "check".
- **Writing:** selected logos are fitted into 136×104 (aspect kept) and written in batches; the
  Philips live TV app is restarted afterwards so its logo cache shows the new pictures.
- **Picking by hand:** "Change" lists related logos from every source with previews, a drawn logo,
  "Pick from USB" and "Don't assign".
- **Screens:** channel list with previews, selection (all / safe ones / none), rescan, "Update
  Logos from the Internet" with the sources' date; Settings (language, theme, preselection, drawn
  logos, sources) and About (developer, license, legal notice) dialogs with a close button, margins
  and remote-scrollable content.
- **Backup and reset:** before the app first changes a channel's logo, the original (or "no logo")
  is kept; "Reset to Default" puts it back for every changed channel or, from "Change", for one.
  "Back Up Logos" saves all current logos and "Restore Backup" writes them back after a channel
  update, matching channels by number and name. `TvLogoTool` gained `dump` and `remove`.
- **Languages and themes:** Turkish and English (system or chosen); system, dark and light themes
  (dark on TVs older than Android 10, which report no dark mode).

### Fixed (during development on the TV)
- Pushing files over ADB broke the connection on Android 8's adbd ("Broken pipe"); logos are now
  written from the app's external files directory, which the shell can read.
- A broken ADB connection is reopened and the operation retried once.
- "SHOW HD" got the international table's "ShowHD" (Discovery Showcase HD); the TV's own country now
  ranks before closeness.
- Startup took ~20 s: name keys are computed with a plain character loop instead of Normalizer and
  regular expressions, the TV table and the iptv-org index are cached, and the GitHub lists are kept
  for a day.
