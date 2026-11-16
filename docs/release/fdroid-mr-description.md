# MR description — paste into the fdroiddata merge request

## Abstract

Doomscroll Ad Free is an ad-free, open-source feed reader for Android that replaces the
OEM "media page" with your own sources: Google News topics and keyword searches, any
RSS/Atom/JSON feed, YouTube channels, Reddit, Lemmy and Hacker News. The app aggregates
these into one clean scrolling feed, extracts full articles into a built-in reader mode
without publisher ads, and ships a home-screen widget that mimics a media page.

Usage: pick sources (Google News topics/searches or any feed URL), browse the feed,
tap a story once for a summary + other coverage, tap again to read it in-app; swipe right
to hide, and add keyword/domain hide rules. Everything stays on the device.

Screenshots: https://github.com/iamoutofmana/doomscroll-ad-free/tree/main/fastlane/metadata/android/en-US/images/phoneScreenshots

## Why I wrote this app

I use a Samsung Galaxy S23 Ultra, and the stock media page (swipe-left panel) only offers
Google Discover or Samsung News — both of which inject sponsored cards, and neither can be
replaced by a third-party app. I built Doomscroll Ad Free to get the same "media page"
experience with sources I choose and zero ads/sponsored items, and decided to open-source
it so others can use it too.

## Unique functions vs. similar apps in F-Droid

- Google News integration out of the box: topic feeds, local/geo feeds and keyword searches
  with operators (intitle:, site:, OR, when:7d) — most FOSS readers require manually
  finding feeds.
- On-device sponsored/advertorial heuristics plus user keyword/domain rules applied at
  ingest; every hidden item is logged with a reason and one-tap reversible.
- Similar-story grouping ("Also covered by") by on-device title similarity with a 48h
  window — a Discover-style clustering that other readers don't offer.
- Lazy article previews (og:image + article paragraphs) and a built-in reader that renders
  extracted plain text — no WebView, no JavaScript, no publisher ads.
- Two widget variants designed to replace the OEM media page: a scrollable feed for
  launchers with scrollable-widget support and a fit-to-page "front page" for One UI.
- Privacy/security posture: no accounts, no analytics, no tracking, HTTPS-only, DDL/XXE
  hardened feed parsing, private/loopback URL blocking, and byte caps on responses.

## Checklist

- **Policy:** App complies with the inclusion policy. All dependencies are FOSS
  (AndroidX/Compose, Glance, Room, WorkManager, OkHttp, Coil, jsoup; no GMS/Firebase).
- **Author:** I am the original author (iamoutofmana); this is my submission.
- **Upstream metadata:** `fastlane/metadata/android/en-US/` contains title,
  short/full description, changelog for versionCode 10, icon.png and four phone
  screenshots (including the en-US locale).
- **MR setup:** Title follows "New app: Doomscroll Ad Free"; fork is public,
  branch is unprotected; a single app is submitted; no rebase needed (no conflicts);
  no RFP issue exists — this MR is the initial submission (label: New App).
- **Metadata:** `metadata/io.github.iamoutofmana.doomscroll.yml`, valid YAML, LF endings;
  descriptions/images live upstream only; releases are tagged (v1.0.0) and auto-update
  is enabled (UpdateCheckMode: Tags); issue tracker provided; AuthorName set; no
  submodules needed.
- **Reproducible builds:** enabled — `Binaries:` points at the GitHub Release APK and
  `AllowedAPKSigningKeys` contains the project release certificate SHA-256
  (B4:F9:52:…:EC). The release APK was built from the same commit with Gradle wrapper
  8.10.2 / JDK 17 / the pinned AGP.
- **ABI split:** not applicable — the APK is 2.78 MB with no significant native code
  (single tiny support library), so splits would not help.
- **Versions:** metadata contains only version 1.0.0 (versionCode 10); no disabled
  versions. `commit:` is the full 40-character hash.
- **Pipeline:** I will address warnings/errors from the pipeline; written to be
  buildserver-compatible (no proprietary dependencies; unsigned build succeeds without
  the upstream release keystore). If GitLab asks for phone/credit-card verification,
  I will leave a note here instead of submitting.
