# Channel Logos

An Android TV app that finds the channels without a logo in the TV's channel list, suggests a
fitting logo and writes it into the TV's channel database. Built and tested on a Philips (TP Vision)
TPM171E running Android 8.0; UI in Turkish, English and Romanian.

## How it works

Only the system and the ADB shell may change the logos of the TV's broadcast channels (Android's
`ACCESS_ALL_EPG_DATA` permission). The app therefore connects to the TV's own ADB over the network
(`localhost:5555`, via [dadb](https://github.com/mobile-dev-inc/dadb)) and runs a small helper,
`TvLogoTool`, from its own APK with `app_process` as the shell user. The helper lists the channels
with their logo state and writes PNGs into `content://android.media.tv/channel/<id>/logo`.

The first connection makes the TV ask "Allow USB debugging?" once; network debugging must be on
(Settings → Developer options).

After writing, the Philips live TV app (`org.droidtv.playtv`) is restarted, because it caches logos
in memory and would keep showing the old picture of a replaced logo.

## Using it

The side panel holds the main actions (write the selected logos, restore every changed channel to
its original), the list view (TV channels without a logo / all), selection, rescanning, updating
the online sources and the logo backup. Type on a keyboard (or the remote's digits) anywhere to
search the list by name or number. Hold OK on a row for a quick menu (write this logo now,
change, restore original). Before writing, a full-screen check lists how many logos replace
existing ones and which look poor; the first write backs up all logos automatically, and the
backup can be exported to a USB stick for another TV. Long tasks show a full-screen progress ring
and end with a full-screen result or an explained error. "Change" on a row shows the current logo
and picks another one, any image from a USB stick or the TV's storage (by name or through the
folder browser), or none.

## Logo sources

| Source | What | Notes |
|--------|------|-------|
| Files | PNG/JPEG/WebP files on a USB stick or in the TV's storage, matched by file name or picked in the folder browser | preferred, treated as safe |
| TV table | logos built into Philips firmware (`tvcontentprovider.apk`) | read-only, fixed at the firmware's date |
| GitHub | [tv-logo/tv-logos](https://github.com/tv-logo/tv-logos), country + international/UK/US folders | no license: downloaded on the TV for personal use, nothing bundled |
| picons | [picons/picons](https://github.com/picons/picons), logos linked to DVB services (SID/TSID/ONID) and service names | GPL-3.0: downloaded on the TV, nothing bundled; a service match whose name agrees is preselected |
| iptv-org | [iptv-org](https://github.com/iptv-org/database) channel database (public domain) | a snapshot of the name list ships with the app and is refreshed weekly; logo links to many hosts; never preselected |
| Drawn | the channel name drawn on a tile | for channels no source knows; never preselected |

Names are compared as keys (lowercase ASCII, Turkish letters folded, "HD/SD/TİCARİ" handled).
Logos from the TV's own country rank first; only close matches from USB, the TV table and GitHub's
country folder, and picons linked to the channel's own broadcast IDs, are preselected; everything
else is marked "check".

## Build

```bash
./gradlew :app:assembleRelease      # builds the release APK (one APK for every TV)
./gradlew :app:testDebugUnitTest    # name matching, ranking, all three languages
./package.sh                        # packages the universal APK and SHA256SUMS into dist/
adb install -r dist/channel-logos-v1.4.5-universal-release.apk
```

Use the release build on the TV: a debuggable build runs several times slower on its CPU.

**Release signing.** Installed copies only accept an update signed with the same key, and the in-app update (About > Check for Updates) depends on it. `release-cert.sha256` holds the expected signing certificate; the packaging script compares every release against it and prints a **SIGNING KEY MISMATCH** warning when they differ. A build with a different key cannot update existing installations: on those devices the app must be uninstalled first and the new version installed again, which removes its data. To keep the key, package on the original machine or copy its `~/.android/debug.keystore`.

To check a new version on a TV, follow [TESTING.md](TESTING.md).

## Limitations

See [ROADMAP.md](ROADMAP.md): Android 11+ wireless-debugging pairing, other brands' logo caches and
private channel databases are not handled yet.

## License

Copyright (C) 2026 berkbb.

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0** (or, at your option, any later version) — see [LICENSE](LICENSE). It is distributed WITHOUT ANY WARRANTY.

Channel names and logos are trademarks of their owners; this independent project is not
affiliated with Philips, TP Vision, Google, the broadcasters, tv-logo, picons or iptv-org.
