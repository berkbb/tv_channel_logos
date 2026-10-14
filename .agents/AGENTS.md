# AGENTS.md — Channel Logos

Rules for AI agents working in this repository. Read this first; it is deliberately short.
Architecture, protocols and matching rules live in [DEVELOPMENT.md](../DEVELOPMENT.md) — read the
relevant section there instead of re-deriving it from the code.

## The project

An Android TV app (`com.babadogan.channellogos`, minSdk 26, Kotlin + Jetpack Compose, one module)
that finds broadcast channels without a logo, suggests one and writes it into the TV's channel
database. Reference device: Philips (TP Vision) TPM171E, Android 8.0, 1080p.

## Commands

```bash
./gradlew :app:testDebugUnitTest     # must pass before any task is reported done
./gradlew :app:assembleDebug         # must compile
./gradlew :app:assembleRelease       # the build to install and measure on a TV
adb logcat ChannelLogos:V AndroidRuntime:E '*:S'
```

Python helper scripts use `/opt/homebrew/bin/python3`.

## Invariants

- **TV access goes only through `TvLogoTool`** (run as the shell user over the TV's own ADB on
  `localhost:5555`). Do not use `adb push` (Android 8 adbd drops the connection); write PNGs to the
  app's external files directory. Keep the connection retry in `TvShell`.
- **Never lose a user's logo:** before the first write to a channel its original is saved
  (`LogoBackups.saveOriginal`). Every path that writes a new logo must go through
  `writeKeepingOriginals` (only "restore to original" writes with `writeFiles` directly).
- **Preselection stays conservative:** only `LogoMatching.isSafe` matches are preselected. Do not
  widen it without new test cases in `LogoMatchingTest`.
- **Sources without a bundling license are never bundled** (tv-logos: none; picons: GPL-3.0); they
  are downloaded on the TV.
- **Performance on TV CPUs:** name keys use the plain character loop in `NameMatcher.key` — no
  `Normalizer`/regex per name. Measure on the TV with the release build only.

## UI rules

- Every text lives in `ui/Strings.kt` as `t(turkish, english, romanian)`. No hard-coded UI text
  except proper names (GitHub, picons, iptv-org, USB, language names). `StringsTest` enforces it.
- Dates: `Strings.dateTime(...)`. Upper case: `uppercase(Strings.locale)`. Never the TV's default
  locale — the app language can differ from the TV's.
- Everything must work with a remote (D-pad + OK + Back): every control focusable with a visible
  focus frame, dialogs closable, scroll areas reachable. Long tasks use the full-screen busy screen
  and end in an `Outcome`; never a status line the user may not see.
- Test the layout at 960×540 dp (1080p TV): the side panel and dialogs must not need guessing.

## Documentation and git

- Update `CHANGELOG.md` (Unreleased, Keep a Changelog sections) for user-facing changes;
  `README.md` / `DEVELOPMENT.md` only for structural changes. Small fixes need no doc edit.
- The roadmap is [ROADMAP.md](../ROADMAP.md) (not a copy under `.agents/`); remove items when done.
- Rejected ideas: [context/user_rejected_spec.md](context/user_rejected_spec.md) — never re-propose them.
- Commit only when asked. Format: `modify type @ scope: 'description' - [tag]`, one line per change.
- Build output and `local.properties` are git-ignored; keep them out of commits.
