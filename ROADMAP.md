# Roadmap and open ideas

## Next: more logo sources for Turkish channels

The tv-logo/tv-logos Turkey folder (302 files) and iptv-org (435 TR channels) leave gaps, mostly
regional and smaller national channels. Candidates checked on 13 October 2026:

| Source | What it offers | Verdict |
|--------|----------------|---------|
| [picons/picons](https://github.com/picons/picons) (`build-source/`) | ~17,800 files; `srp.index` maps `SID_TSID_ONID_namespace` to a logo name, 518 entries / 353 logos for Türksat 42E (`_1A40000`); `snp.index` maps service names | **Best next source.** GPL-3.0, so download on the TV like GitHub, don't bundle. Match by the DVB triplet (`service_id`, `transport_stream_id`, `original_network_id` columns in TvProvider) before names. 42E service refs are spotty for the big channels (Show, ATV, Kanal D missing from `srp.index`), so keep `snp.index` + name keys as fallback. Logos are SVG or PNG, often with `.light`/`.dark` variants. |
| Türksat frequency list (`turksat.com.tr/uydu/yayincilik-hizmetleri/turksat-frekans-listesi?page=0..15`) | 751 rows (TV + radio), name, frequency, polarisation, SR, FEC, V-PID/A-PID, satellite; 191 rows carry an inline base64 PNG (`img.kanal-logo`, `alt` = channel name), 154 unique | Official names, but 144 of the logos are 52×39 thumbnails (Show, Star, Kanal D, TV8, TRT 1…). Only ~40 are large (TRT channels 1001×1001, TV100, CNBC-e, Bengü Türk). No service IDs. Useful as a last-resort tier and as a name list for matching; scraping HTML is fragile. |
| Türksat Kablo channel PDF | 155 images, 147 of them 80×60, no names attached to images | Not usable. |
| Wikidata (`P154` logo, country Turkey) | 25 TV channels | Too thin. |
| LyngSat Logo, MediaPortal "Türksat Logo Pack" | large sets | Copyrighted collection / outdated (Türksat 2A/3A era). Skip. |
| TKGS (Türksat channel update system) | channel list over the satellite | Carries no logos (the "TKGS LOGO" strings in MediaTek LiveTV are only debug tags). |

**picons: implemented (unreleased).** Still to check on the Philips TPM171E: that its TV provider
fills `service_id`/`transport_stream_id`/`original_network_id` (look at the last three columns of
`TvLogoTool list`); if they are 0 there, only the name fallback works. Also compare how picons'
`default` logos look on Philips' channel bar next to the built-in ones (picons also has `light`
variants).

## Closed: Arçelik as a logo source

**Checked 13 October 2026** over adb (Arçelik Android 11, MediaTek m7322): the TV has no
broadcast-channel logos to borrow. MediaTek LiveTV (`com.mediatek.wwtv.tvcenter`) shows no logos in
the channel list or EPG in the EU region, the Channel Editor (`com.arcelik.ChannelEditor`) lists
number + name only, and nothing writes `channel/<id>/logo`. The home-screen "Live TV" row uses tiles
that `com.mediatek.tv.service` draws from the channel number, name and programme — probably what
looked like logos on 12 October. The shell user also lacks `ACCESS_ALL_EPG_DATA` there, so the
app's `TvLogoTool` route cannot list or write channels on this TV.

## Reaching more TVs

- **Android 11+ "Wireless debugging" — blocked (checked 14 October 2026).** A library exists:
  [Kadb](https://github.com/flyfishxu/Kadb) (Apache-2.0, a dadb fork) does SPAKE2 pairing and the
  STLS upgrade; 2.1.1 is the newest built with Kotlin 2.3 (2.1.3+ need Kotlin 2.4 metadata, AGP
  9.4.1 bundles 2.2), and its SPAKE2 artifact is only on JitPack. The blocker is the TV itself:
  the pairing code is shown in a Settings dialog, and Android stops pairing when that dialog
  leaves the screen, so this app cannot be in front to take the code. Phones solve it with a
  notification reply or split screen (Shizuku); Android TV has neither. Options to explore: pair
  from a phone app that hands this app's key over, or an overlay (needs `SYSTEM_ALERT_WINDOW`,
  rarely grantable on TVs). Many Android 11+ TVs still accept plain port 5555 with "USB debugging"
  on, which already works.
- **Refreshing the logo cache — check on other brands:** the app now also restarts whatever
  answers Android's "view channel" intent (tuner input packages excluded). Confirm on a Sony /
  Google TV that the new logos show without a reboot.

## Smaller items

- Long-run check on the TV: memory and CPU while writing a few hundred logos.
- Refresh `app/src/main/assets/iptv-org-channels.tsv.gz` before each release
  (`python3 tools/iptv_org_snapshot.py`).
- On the TPM171E, measure the start with the bundled iptv-org list (names are keyed at load:
  ~40,000 rows).
- Check on a TV: the quick menu's held-OK detection, the write check screen, USB export on
  Android 8 (app folder on the stick), the internal backup copy through the shell
  (`/sdcard/ChannelLogosBackup` and back into the app's folder) on Android 8 and 11+.
