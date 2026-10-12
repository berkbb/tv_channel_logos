# Roadmap and open ideas

## Next: Arçelik's logo service as a fourth source

**Observation (12 October 2026):** an Arçelik Android 11 TV on the same network shows channel logos
that the Philips TPM171E lacks. Philips only has the logo table frozen in its 2021 firmware; Arçelik
apparently keeps its logos up to date, most likely from its own server.

**Idea:** find where Arçelik's TV gets its logos (server and address pattern) and, if it can be
queried by channel name or broadcast IDs, add it as a source next to the TV table, GitHub, iptv-org
and USB.

**State:** the Arçelik TV (192.168.50.216) has network debugging enabled (port 5555 open); the
first `adb connect` is waiting for the "Allow USB debugging?" approval on that TV.

**Steps:**
1. `adb connect 192.168.50.216:5555` and approve on the Arçelik TV ("Always allow").
2. Find the app that owns the channel logos:
   `adb shell content query --uri content://android.media.tv/channel --projection _id:display_name:input_id:package_name`
   and which process writes them (`adb logcat | grep -i logo` during a channel update).
3. Look for the logo URLs: the channel rows' `internal_provider_data` and `app_link_*` columns,
   the owner app's own databases/preferences (if readable), its network requests in logcat, or its
   APK (`adb shell pm path <package>`, then look for URL patterns in the dex/resources).
4. Check how a logo is addressed (channel name, service ID, ONID/TSID/SID, an internal ID) and
   whether the service answers requests from other devices.
5. If usable: add an `ArcelikLogoSource` (catalogue tier like GitHub's country folder), cache like
   the other online sources, and a setting to turn it off.

**Caution:** this is likely an undocumented service run for Arçelik's own devices. Before shipping
it, check its terms; keep it opt-in and for the owner's personal use, like the other sources.

## Reaching more TVs

- **Android 11+ "Wireless debugging":** newer TVs (most Google TVs) offer only TLS pairing with a
  code (`adb pair`) instead of plain port 5555. The dadb library does not implement pairing; add it
  (SPAKE2 pairing + TLS) so the user can type the 6-digit code shown by the TV.
- **Refreshing the TV's logo cache on other brands:** today the Philips live TV app
  (`org.droidtv.playtv`) is restarted after writing, because it caches logos in memory. Find the
  live TV app generically (the activity handling `android.media.tv` channel URIs / the TV input's
  setup package) instead of naming the package.
- **TVs with a private channel database:** some manufacturers keep the channel list outside
  Android's TV provider; logos written there do not show. Detect this (no tuner channels in the
  provider) and say so in the app.

## Smaller items

- Export the logo backup to a USB stick, to move prepared logos to another TV.
- Long-run check on the TV: memory and CPU while writing a few hundred logos.
- Startup: first iptv-org index build takes ~20 s on the TPM171E; consider a prebuilt index.
