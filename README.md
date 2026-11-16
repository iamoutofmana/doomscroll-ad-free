# Doomscroll Ad Free

[![Android CI](https://github.com/iamoutofmana/doomscroll-ad-free/actions/workflows/android-ci.yml/badge.svg)](https://github.com/iamoutofmana/doomscroll-ad-free/actions/workflows/android-ci.yml)

**An ad-free, open-source replacement for your phone's "media page."**
News, videos, podcasts, and community posts from sources *you* choose — zero ads, zero sponsored items, zero tracking.

Started because the Samsung media page shows ads and there is no way to replace it with your own app. This is the workaround — and it's better.

## The three promises

1. **No ads. No sponsored items. Ever.** No ad SDKs, no analytics, no trackers — plus an on-device filter that removes sponsored/advertorial items publishers sneak into feeds.
2. **Your data stays on your phone.** No account, no telemetry. The app only ever talks to the feeds *you* configure — plus the article pages you open a preview for.
3. **Open source, forever.** GPL-3.0, FOSS-only dependencies, intended for F-Droid.

## Roadmap

| What | Phase |
|---|---|
| Google News topics, searches & local feeds (Discover-style, via public RSS) + any RSS/Atom/JSON feed | 1 |
| Home-screen feed widget — the actual media-page replacement | 2 |
| Sponsored-item filtering + built-in ad-free reader mode | 2 |
| YouTube channels, podcasts, Reddit, Lemmy, Hacker News | 3 |
| On-device "For You" ranking | 4 |
| First stable release (F-Droid, IzzyOnDroid) | 5 |

## Screenshots

| Feed | Reader mode |
|---|---|
| ![Feed](docs/screenshots/feed.png) | ![Reader](docs/screenshots/reader.png) |

| Home-screen widget | In-app help |
|---|---|
| ![Widget](docs/screenshots/widget.png) | ![Help](docs/screenshots/help.png) |

Full plan: [`docs/PLAN.md`](docs/PLAN.md)

## Install

**v1.1.0 is released.** Download the signed APK from
[GitHub Releases](https://github.com/iamoutofmana/doomscroll-ad-free/releases) and sideload it
(Android 8+). F-Droid inclusion is pending the project-age requirement (3 months of
maintenance before first inclusion); IzzyOnDroid submission is planned.

> **Signing note:** GitHub builds are signed with the project key; F-Droid builds use
> F-Droid's key. Pick one distribution and stick with it — switching requires uninstalling.

### Replacing your media page (once the widget ships — Phase 2)

1. Home screen settings → turn **off** the media page / Google Discover panel
2. Long-press the wallpaper → **Widgets** → *Doomscroll Ad Free* → drop the 4×5 feed widget on a dedicated page
3. Done — swipe over whenever you want your feeds

## Build from source

Requires JDK 17.

```
git clone https://github.com/iamoutofmana/doomscroll-ad-free
cd doomscroll-ad-free
./gradlew assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Privacy

*Doomscroll Ad Free collects nothing.* No accounts, no analytics, no crash reporting, no ads. All feeds, preferences, and reading history live in the app's local database on your device.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and ideas are welcome any time.

## License

[GPL-3.0-or-later](LICENSE) © 2026 iamoutofmana
