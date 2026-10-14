# Changelog

All notable changes to **Channel Logos** (`com.babadogan.channellogos`) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

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
  (`Android/data/com.babadogan.channellogos/files/ChannelLogosBackup`); "Import Backup from USB"
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
