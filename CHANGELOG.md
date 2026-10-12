# Changelog

All notable changes to **Channel Logos** (`com.babadogan.channellogos`) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Known issues
- Only TVs with plain network debugging (port 5555) can be reached; Android 11+ "Wireless
  debugging" needs pairing, which is not implemented yet (see ROADMAP.md).
- The logo cache refresh after writing knows only the Philips live TV app; on other brands a
  replaced logo may show its old picture until the TV restarts.

### Ideas
- Arçelik's logo service as a fourth source (ROADMAP.md).

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
