# Channel Logos (Kanal Logoları)

An Android TV app that finds the channels without a logo in the TV's channel list, suggests a
fitting logo and writes it into the TV's channel database. Built and tested on a Philips (TP Vision)
TPM171E running Android 8.0; UI in Turkish and English.

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

## Logo sources

| Source | What | Notes |
|--------|------|-------|
| USB | PNG/JPEG/WebP files on a USB stick, matched by file name | preferred, treated as safe |
| TV table | logos built into Philips firmware (`tvcontentprovider.apk`) | read-only, fixed at the firmware's date |
| GitHub | [tv-logo/tv-logos](https://github.com/tv-logo/tv-logos), country + international/UK/US folders | no license: downloaded on the TV for personal use, nothing bundled |
| iptv-org | [iptv-org](https://github.com/iptv-org/database) channel database (public domain) | logo links to many hosts; never preselected |
| Drawn | the channel name drawn on a tile | for channels no source knows; never preselected |

Names are compared as keys (lowercase ASCII, Turkish letters folded, "HD/SD/TİCARİ" handled).
Logos from the TV's own country rank first; only close matches from USB, the TV table and GitHub's
country folder are preselected, everything else is marked "check".

## Build

```bash
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew :app:testDebugUnitTest    # name matching, ranking, both languages
adb install -r app/build/outputs/apk/release/app-release.apk
```

Use the release build on the TV: a debuggable build runs several times slower on its CPU.

## Limitations

See [ROADMAP.md](ROADMAP.md): Android 11+ wireless-debugging pairing, other brands' logo caches and
private channel databases are not handled yet.

## License

MIT. Channel names and logos are trademarks of their owners; this independent project is not
affiliated with Philips, TP Vision, Google, the broadcasters, tv-logo or iptv-org.
