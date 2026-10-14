# On-TV test plan (1.1.0 – 1.2.0)

Everything since 1.0.0 passed the unit tests and builds, but none of it has been seen on a real
TV yet. Work through this list on the TV and note the result of each step.

## 1. Set up on a new machine

1. Install JDK 21 and the Android SDK (platform 37, build tools 36), then clone the repository.
2. Create `local.properties` in the project folder (it is git-ignored) with your SDK path:
   ```
   sdk.dir=/path/to/Android/sdk
   ```
3. Build and test:
   ```bash
   ./gradlew :app:testDebugUnitTest :app:assembleRelease
   ```
4. On the TV turn on Settings → Developer options → Network debugging, then:
   ```bash
   adb connect <TV-IP>:5555
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   Use the release build: a debug build runs several times slower on the TV.
5. Start the app on the TV; approve "Allow USB debugging?" with "Always allow".

## 2. Checklist

### Backup (1.2.0) — the riskiest part: the internal copy goes through the ADB shell

- [ ] Export Backup → **TV Internal Storage** ends with "saved … /sdcard/ChannelLogosBackup".
- [ ] Export Backup → **USB Stick** ends with "saved" and a path on the stick.
- [ ] Import Backup lists both, newest first, each "SHA-256 verified" with settings included.
- [ ] Importing one applies its settings (change the theme first, then import: it comes back).
- [ ] Damage test: on a computer, edit one PNG in the USB copy, import → "1 logo files failed
      their SHA-256 check and were skipped". Edit `manifest.tsv` → "channel list is damaged".
- [ ] Restore Backup's confirmation says "N of M backed-up logos will be written".
- [ ] After a channel rescan that renumbers channels, the "channels changed a lot" warning shows
      only when fewer than 80 % match.
- [ ] Internal copy check, if it fails: `adb shell ls -l /sdcard/ChannelLogosBackup`.

### Writing (1.1.0)

- [ ] "Write Selected" first shows "Preparing logos: x / y", then the full-screen check (count,
      replacing, low quality, failed downloads).
- [ ] On a TV without a backup, the check says a backup is taken first, and the result says it
      was taken.
- [ ] "Skip Low Quality" leaves out the logos marked "low quality".
- [ ] New logos show in the channel bar/list right after writing (live TV app restarted).
- [ ] Unplug the TV, start it again: the written logos are still there.

### List and menus (1.1.0)

- [ ] Typing on a keyboard or the remote's digits jumps into search and filters as you type.
- [ ] In "TV channels without logo", searching a channel that has a logo offers "Search All".
- [ ] The counter above the list ("N channels · N selected · N with a suggestion") is right.
- [ ] Holding OK on a row opens the quick menu; a short press only toggles the selection.
- [ ] "Change" shows the channel's current logo above the candidates.
- [ ] Long tasks show the full-screen progress ring; results and errors open full screen.

### Layout (1.1.0)

- [ ] Side panel, Settings and About fit on the 1080p screen; no text cut off or overlapping.
- [ ] Every button is reachable with the D-pad and shows the focus frame.
- [ ] Romanian UI: dates in Romanian, headings without a dotted "İ" ("SELECȚIE").

### Sources and startup (1.1.0)

- [ ] First start after installing takes clearly less than before (iptv-org list bundled). Note
      the time from the logcat line `iptv-org logos: … keys in … ms`.
- [ ] With three or more Romanian channels installed, the status card says Romanian logos are
      searched too; without them it does not.

## 3. Logs to send when something fails

```bash
adb logcat -d ChannelLogos:V AndroidRuntime:E '*:S' > channel-logos.log
```

Also note the TV model, its Android version and which step failed. Remove items from the
"Check on a TV" list in [ROADMAP.md](ROADMAP.md) once they pass.
