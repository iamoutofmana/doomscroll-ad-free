# Doomscroll Ad Free — Project Plan

**An ad-free, open-source replacement for the Samsung media page.**

- **App name:** Doomscroll Ad Free (decided — Nov 2026)
- **Package/applicationId:** `io.github.iamoutofmana.doomscroll` (locked)
- **Repo:** [github.com/iamoutofmana/doomscroll-ad-free](https://github.com/iamoutofmana/doomscroll-ad-free) (created, public, empty — Nov 2026)
- **Target device:** Samsung Galaxy S23 Ultra (One UI), any modern Android 8+ phone
- **Stack:** Kotlin + Jetpack Compose (locked)
- **License:** GPL-3.0 (locked)
- **Distribution:** GitHub Releases → IzzyOnDroid → F-Droid (all FOSS)
- **Build style:** vibe-coded with AI in small, shippable phases — each phase has a Definition of Done

---

## 1. Vision

A fast, private, beautiful "media page" app that shows **news, videos, podcasts, and community content from sources *you* choose — with zero ads, zero sponsored items, and zero tracking.** It replaces the Samsung media page on your home screen via a large scrollable widget, and it's open source so anyone can use, inspect, and improve it.

### Goals
- Replace the Samsung media page experience (widget on a dedicated home page + full app)
- Discover-like content via **Google News public RSS** (topics, locations, keyword searches)
- Plus: any RSS/Atom/JSON Feed, YouTube channels, podcasts, Reddit, Lemmy, Hacker News
- Never show ads or sponsored/promoted content — actively filter them out
- 100% on-device: no accounts, no telemetry, network calls only to feeds you configure
- FOSS forever: GPL-3.0, F-Droid-compatible, no proprietary dependencies

### Non-goals (v1)
- iOS (widget/system integration is Android-first)
- Scraping Google Discover's private endpoints (see §2 — no API exists; scraping violates ToS and is brittle)
- Server-side sync service (optional self-hosted FreshRSS/Miniflux sync is post-1.0)
- Social features (posting, comments)

---

## 2. Key constraints — research findings (don't re-litigate)

1. **Samsung will not let a third-party app be the media page.** One UI's swipe-left panel only supports Google Discover or Samsung News; community requests for "any app as media page" have gone unanswered for years. **Our play:** turn the media page OFF and place our large scrollable widget on a dedicated home screen page. Visually and functionally nearly identical (§8).
2. **Google Discover has no API.** It never had one, and the old Feed API was killed in 2016. Personalized Discover is tied to your Google account, so third parties can't fetch "your" Discover anyway. **The legitimate, stable proxy is Google News public RSS** — topic feeds, local/geo feeds, and keyword-search feeds with powerful operators. This covers most of what Discover surfaces, minus the ads.
3. **Google News RSS quirks** (handled by a Phase 1 spike + risk plan, §11):
   - Links are `news.google.com/rss/articles/<base64>` redirects, not publisher URLs. Newer ones require calling Google's internal `batchexecute` endpoint to decode (well-documented in open-source decoders like `googlenewsdecoder`). **Fallback:** just open the redirect URL — browsers resolve it to the publisher fine.
   - The feed format is community-documented, not officially documented → pin formats with test fixtures.
4. **Other sources are easy:** YouTube exposes per-channel RSS; podcasts are RSS with enclosures; Reddit and Lemmy expose public RSS (Reddit needs a polite custom User-Agent); Hacker News has the free Algolia JSON API.
5. **Scrollable feed widgets are fully supported** via Jetpack Glance (`LazyColumn`) — this is what makes a "media page" widget possible.

---

## 3. Product design (high level)

**App tabs** (mirrors the old media page, plus communities):

| Tab | Content | Sources |
|---|---|---|
| **For You** | Ranked mix from all your sources | Everything (on-device ranking, Phase 4) |
| **News** | Articles with images | Google News topics/searches + any RSS/Atom feed |
| **Videos** | Video cards | YouTube channels |
| **Podcasts** | Episode list | Podcast RSS feeds |
| **Communities** | Posts/discussions | Reddit, Lemmy, Hacker News |

**Card anatomy:** hero image, source name + favicon, title, age, media badge. Actions: open (Custom Tabs or built-in Reader), save, hide ("show me less like this"), share.

**Reader mode:** extracted article text rendered in-app (no web ads), offline cache, font/theme controls. One-tap "open original site" stays available — support the publishers you like.

**Design:** Material 3 + dynamic color (matches One UI theming), AMOLED-black dark option, large-type "media" feel. Edge-to-edge, pull-to-refresh.

**Widget (the media page replacement):** scrollable 4×3 / 4×4 / 4×5 feed widget. Tap a card → open article; tap header → open app. Refresh: periodic + on phone unlock + on app open. Configurable per instance (For You / News / Videos / Podcasts).

**Anti-ad guarantee:** no ad SDKs, no analytics, no trackers — we render every card ourselves, so nothing *can* inject an ad. Layered filtering per §7.

---

## 4. Tech stack (locked)

| Layer | Choice | License | Why |
|---|---|---|---|
| Language | Kotlin 2.x, JDK 17 | — | Native, FOSS-standard |
| UI | Jetpack Compose + Material 3 + dynamic color | Apache-2.0 | Modern, One UI-friendly |
| Widgets | androidx.glance 1.1+ (`LazyColumn`) | Apache-2.0 | Scrollable feed widget |
| Database | Room | Apache-2.0 | Local-first storage |
| Background sync | WorkManager | Apache-2.0 | Battery-friendly periodic sync |
| Network | OkHttp + kotlinx.serialization | Apache-2.0 | Robust HTTP, conditional GET |
| Feed parsing | Custom RSS 2.0 / Atom / JSON Feed / Media-RSS parser (ours) | project GPL-3.0 | Small, fixture-tested, handles real-world feed weirdness |
| Reader extraction | jsoup | MIT | Article text extraction + HTML sanitization |
| Images | Coil | Apache-2.0 | Async image loading |
| Audio (v2) | Media3 ExoPlayer | Apache-2.0 | Podcast playback later |
| Links out | Chrome Custom Tabs | — | Fast, private-ish article opening |
| DI | Manual lightweight DI | — | Keeps build + F-Droid simple |
| Min/target SDK | 26 / current stable | — | Covers ~95%+ of devices |

All dependencies are FOSS → **F-Droid compatible**.

---

## 5. Architecture (high level)

Start as a **single-module app** with clean package boundaries; extract Gradle modules only if it grows:

```
ui/         Compose screens, navigation, theming
domain/     models, filter engine, For-You ranking (pure Kotlin, testable)
data/       Room DB, repositories, sync orchestration (WorkManager)
feedparser/ pure Kotlin parser: RSS/Atom/JSON Feed/Media-RSS (fixture-tested)
widget/     Glance widgets
```

**Data flow:** WorkManager sync → fetch (conditional GET with ETag) → parse → normalize → **filter engine** (user rules + sponsored heuristics + dedupe) → Room → observed by Compose UI and the Glance widget.

**Core entities (sketch):**
- `Source(id, type: GN_TOPIC|GN_SEARCH|RSS|YOUTUBE|PODCAST|REDDIT|LEMMY|HN|JSONFEED, url, title, category, enabled, syncInterval, lastSync, etag)`
- `Article(id, sourceId, guid, title, resolvedUrl, author, publishedAt, image, summary, mediaType, mediaUrl, read, saved, hidden, hiddenReason, dedupeKey)`
- `Rule(id, type: HIDE_KEYWORD|HIDE_DOMAIN|BLOCKLIST, pattern, enabled)`
- `Interaction(articleTags, action: OPEN|HIDE|SAVE, timestamp)` — local signals for For You ranking

---

## 6. Roadmap — vibe-code phases (each shippable and testable)

> Rhythm: build a phase with AI → install on the S23 Ultra → use it for a few days → note what's wrong → next phase. Timeline assumes part-time sessions (~4–8 weeks total to v1.0).

### Phase 0 — Foundations (1 session)
Local dev setup on this Windows machine (Git + GitHub CLI — see §13), GitHub repo, GPL-3.0 LICENSE, README stub, CI (ktlint + unit tests + debug APK), app scaffold, base theme, manual DI wiring.
**DoD:** CI green; app installs on your phone showing a themed empty state.

### Phase 1 — MVP news reader (1–2 weeks of sessions)
- Google News source picker: topics list, keyword-search builder (operators preview), local/geo feeds, locale selector
- Add any RSS/Atom/JSON Feed URL
- Fetch + parse + store; unified News feed; pull-to-refresh; background sync every ~2h
- Image cards; open in Custom Tabs with tracking params stripped
- Mark-read, save, basic hide
- **Spike: Google News redirect decoding** (batchexecute technique; fallback = open redirect URL)
**DoD:** daily-driver usable for news — you read it for 3 days without opening Samsung's page.

### Phase 2 — The "media page moment" (1–2 weeks)
- Glance widget 4×5 (+4×3, 4×4): scrollable For You feed, per-card deep links, refresh strategy (§8)
- Sponsored/advertorial filter engine + user rules (hide keywords/domains) + story dedupe
- Reader mode (jsoup extraction, sanitization, offline cache, font controls)
- OPML import/export (bring subscriptions from other apps)
**DoD:** media page turned OFF on your phone; the widget replaces it; zero sponsored items after a week of use.

### Phase 3 — Full media (1–2 weeks)
- YouTube channels (per-channel RSS; open with YouTube / NewPipe / browser — user choice)
- Podcasts (subscribe to feeds, episode list; v1 hands off to external player, v2 in-app Media3 playback)
- Reddit (`r/x/.rss` with custom User-Agent), Lemmy community feeds, Hacker News front page (Algolia API)
- Videos, Podcasts, Communities tabs live
**DoD:** all four content types work; widget can be configured per tab.

### Phase 4 — For You (1 week)
- On-device ranking: recency + source weights + topic affinity from your open/hide/save interactions
- "Show me less like this" feedback loop
- Per-source weight settings
**DoD:** For You tab feels noticeably better than chronology; all tuning stays on-device.

### Phase 5 — v1.0 release (a few days)
- Icon + branding (name locked: "Doomscroll Ad Free"), screenshots, fastlane metadata
- Privacy policy ("collects nothing"), CHANGELOG, semver tag
- Signed GitHub Release APK; IzzyOnDroid submission; F-Droid submission
- Weblate-ready strings
**DoD:** a stranger can install it in under a minute and get an ad-free media page.

### Post-1.0 backlog (ideas, not commitments)
FreshRSS/Miniflux sync · Mastodon sources · "news near me" geo feed · multi-widget dashboards · tablet/Two UI layouts · TTS "listen queue" for articles · on-device sponsored classifier trained on your feedback.

---

## 7. Anti-ad & anti-sponsored spec (the core promise)

1. **Architecture:** we render our own cards; no ad SDK, no analytics, no trackers — nothing *can* show an ad.
2. **Link hygiene:** strip `utm_*`, `fbclid`, `gclid`, `ref` and similar tracking params; unwrap AMP links to canonical URLs.
3. **Sponsored-content detection** (publishers still sneak "native ads" into feeds):
   - Heuristics: regex over title/categories/author — *sponsored, advertorial, paid post/content/promotion, partner content, brought to you by, presented by, promoted, native ad, #ad, affiliate*
   - Structured: some feeds mark sponsorships in categories or `dc:creator` — honor those
   - User rules: hide by keyword or domain; optional community-maintained ad-farm blocklist file (importable, default off)
   - Every match is **logged with its reason and one-tap reversible** — never silently wrong; conservative defaults; user rules always win
4. **Dedupe:** cluster near-identical stories (normalized-title similarity + time window) so one story from 8 outlets appears once.
5. **Reader mode** avoids publisher-page ads for in-app reading; original-site opening stays one tap away.
6. *(Later, optional)* on-device classifier trained on your hide/show feedback — heuristics first.

---

## 8. The widget — how it replaces the media page

**Behavior:** scrollable `LazyColumn` of cards (image, source, title, age). Tap card → article; tap header → app. Sizes 4×3 / 4×4 / 4×5. Each widget instance binds to a view (For You, News, Videos, Podcasts).

**Refresh budget:** WorkManager every 2–4h + refresh on phone unlock + refresh whenever the app opens + manual button. (Doze/battery-saver can defer updates; expectations documented in README.)

**One UI setup guide (ships in README + in-app onboarding):**
1. Home screen settings → turn **off** the media page / Google Discover panel
2. Long-press wallpaper → Widgets → Doomscroll Ad Free → drag the 4×5 widget onto a dedicated page
3. Optional: make that page the swipe-adjacent "first" page — you now swipe to *your* media page

---

## 9. Privacy principles (documented + enforced)

- All data local (Room); no account, no telemetry, no crash reporting
- Network calls **only** to user-configured source URLs (plus their images) — no hardcoded third-party calls
- Cleartext HTTP sources get a visible warning
- Full data export/import: OPML for sources + JSON backup for state
- Privacy policy is one line: *"Doomscroll Ad Free collects nothing."*

---

## 10. Open-source project setup

- **Repo (GitHub, public):** README with screenshots + the §8 setup guide, LICENSE (GPL-3.0), CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, issue templates (bug/feature), GitHub Discussions on, ADR folder (`docs/adr/`) so future-you knows *why*
- **CI (GitHub Actions):** ktlint, unit tests, assemble debug on every PR; release workflow: tag → build → attach signed APK
- **Distribution ladder:** GitHub Releases (day one) → **IzzyOnDroid** (quick FOSS repo) → **F-Droid** (metadata + fastlane screenshots + reproducible build). Play Store optional later ($25 once; app is policy-clean since nothing is scraped)
- **i18n:** externalized strings from day 1; Weblate (free for FOSS) after v1.0
- **Versioning:** semver + CHANGELOG.md; tagged releases
- **Community:** "good first issue" labels, roadmap project board, no-code-of-conduct-drama baseline

---

## 11. Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Google News RSS format drift (undocumented) | Parser breaks | Fixture-based tests, defensive parser, quick patch releases |
| Google News redirect decoding changes | Clean links / reader mode break | Phase 1 spike; fallback = open redirect URL (browser resolves it); community decoders tracked |
| Reddit blocks default User-Agents | Subreddit feeds fail | Custom UA + backoff + caching; per-source sync intervals |
| Publisher RSS gives truncated text | Reader shows short articles | Reader-mode full fetch; graceful fallback to Custom Tabs |
| Widget refresh deferred (Doze) | Stale widget content | Refresh on unlock + on app open; document expectations |
| One UI widget sizing quirks | Broken layout on Samsung | Three widget sizes; S23 Ultra is the first-class test device |
| Sponsor-filter false positives | User misses real stories | Conservative defaults, logged + reversible, user rules override |
| Name/trademark collision | Rename before launch | Name locked: "Doomscroll Ad Free" — no Google/Samsung/Discover marks; existing "Doomscroll" apps are screen-time blockers (different category, no conflict found Nov 2026); re-check stores/domain before Phase 5 |
| Vibe-code drift/quality | Silent regressions | Pure-logic tests (parser, filters), CI gate, small phases, DoD checklists |
| Solo-maintainer burnout | Project stalls | Tight v1.0 scope; backlog parking lot; ADRs keep context cheap |

---

## 12. Decisions status

- [x] **App name: "Doomscroll Ad Free"** (decided Nov 2026 — existing "Doomscroll" apps are screen-time blockers, different category, no conflict found)
- [x] **Repo:** [github.com/iamoutofmana/doomscroll-ad-free](https://github.com/iamoutofmana/doomscroll-ad-free) — created, public, empty (Nov 2026)
- [x] **Android applicationId:** `io.github.iamoutofmana.doomscroll` (locked)
- [ ] App icon direction
- [ ] Default Google News edition (locale: `hl` / `gl` / `ceid` — presumably `en-US` / `US` / `US:en`)
- [ ] Ever ship to Play Store, or FOSS-only channels?

**Org vs. personal:** an org is only worth it for multiple maintainers or a neutral project brand (`github.com/<org>/doomscroll-ad-free`). You can transfer a personal repo to an org later and GitHub auto-redirects all old URLs — zero penalty for starting personal. Orgs are free: `github.com/organizations/new`.

---

## 13. Next session

**Phase 0 status (Nov 13, 2026):**
- [x] Repo created (public, empty): `github.com/iamoutofmana/doomscroll-ad-free`
- [x] applicationId locked: `io.github.iamoutofmana.doomscroll`
- [x] Phase 0 scaffold built locally in `doomscroll-ad-free/` — Gradle/Compose app, GPL-3.0 LICENSE, CI workflow (ktlint + tests + debug APK artifact), themed empty state, identity guard test
- [x] Git 2.56 + GitHub CLI 2.104 installed; authenticated; git credential helper wired
- [x] Android Studio 2026.2.2 installed; first-run SDK setup in progress
- [x] Pushed to `main`; CI green in 2 cycles (run 43508195895 — one ktlint fix cycle: disabled `function-naming` for Compose, conformed to ktlint 1.3 `wrapping` rule)
- [x] First debug APK installed on the S23 Ultra via `adb` (serial R5CW2162RCE, permanently authorized) — **Phase 0 DoD met (Nov 13, 2026)**
- [x] Local toolchain note: Android Studio 2026.2.2 bundles JDK 25, which Gradle 8.10.x cannot run on → Temurin JDK 17.0.21 installed locally; `~/.gradle/gradle.properties` pins `org.gradle.java.home`; Studio's Gradle JDK must stay on 17 until a future Gradle 9 / AGP 9 upgrade (maintenance backlog)

**Phase 0:** I scaffold the project — Gradle/Compose app, GPL-3.0 LICENSE, README stub, CI workflow, theme, empty shell — push it to GitHub, and you install it on the S23 Ultra. Then we roll straight into Phase 1.

**Phase 1 status (Nov 13, 2026) — MVP news reader, code-complete:**
- [x] Feed pipeline: `FeedFetcher` (conditional GET via ETag/Last-Modified, https-only, BOM/charset sniffing) → `FeedParser` (RSS 2.0/RDF, Atom, JSON Feed 1.x, Media RSS, enclosures, CDATA, entity handling, lenient RFC-822/ISO-8601 dates)
- [x] Google News: URL builder for top stories, 8 topics, geo/city feeds, and search with operators (`intitle:`, `site:`, `OR`, `when:1d/7d/1y`) across 8 locales; "Headline - Publisher" title splitting
- [x] `GnRedirectResolver`: offline protobuf/base64 decode → `batchexecute` endpoint → graceful fallback to opening the redirect (circuit breaker: 3 consecutive failures stops the batch; cap 15 resolves/sync)
- [x] Anti-sponsored (first layer of plan §7): conservative title/category heuristics at ingest; everything hidden is logged with a reason and reviewable/reversible via the "Filtered (n)" dialog
- [x] Link hygiene: utm_*/fbclid/gclid/etc. stripped, leading `/amp/` unwrapped before opening or sharing; opens in Custom Tabs
- [x] Storage: Room (sources, articles; unique `(sourceId, guid)`; FK cascade; 30-day prune of unsaved items)
- [x] Sync: `SyncEngine` (mutex-guarded, per-source error capture, RSS sources auto-rename from feed title) + WorkManager periodic every 2h + auto-sync on app open when stale >15min + pull-to-refresh
- [x] UI: Feed / Saved / Sources tabs; article cards (Coil images, publisher · relative time, unread styling); mark-read, mark-all-read, save, hide, share; source list with enable toggle, last-sync/error status, delete; Add-source bottom sheet (topics multi-select, search builder, RSS URL with http:// warning, edition chips)
- [x] Quality gate green locally: 48 unit tests (parser fixtures incl. Google News quirks, dates, URL builders, sanitizer, sponsor filter, redirect decoder), ktlint clean
- [x] ktlint config note: disabled `function-naming` (Compose convention) + the `multiline-expression-wrapping` family (`function-signature`, `function-expression-body`, `property-wrapping`, `class-signature`, `string-template-indent`) — ktlint crashes if a disabled rule's dependents stay enabled; documented in `.editorconfig`
- [x] Pushed to `main`; CI green (run 43525317452 — 2m2s, 63 tests, lint, APK artifact)
- [x] Installed on the S23 Ultra via `adb` streamed install (Nov 13, 2026)
- [x] **Phase 1.5 feedback loop (live user testing):**
    - [x] Cross-source dedupe by normalized title (7-day window) — same headline across GN "Top stories" vs "Technology" now shows once
    - [x] New source types: YouTube channels, Reddit subreddits, Lemmy communities, Hacker News front page
    - [x] `PopularSources.kt` URL builders (shared by UI + SyncEngine)
    - [x] `HackerNewsParser` (custom Algolia JSON; skips Ask/Show self-posts)
    - [x] Tests: 63 passing (added TitleNormalizer, PopularSources, HN parser, Atom/RSS image extraction)
    - [x] AddSourceSheet split into "Google News" / "More sources" tabs
    - [x] Pushed + installed on device (commit c15c948)
- [x] Image extraction: confirmed working for Atom `<content>` (The Verge) and RSS `<description>` (hero images from non-Google-News sources). Google News's public RSS genuinely strips images from items — hero-image extraction for GN items is Phase 2 (reader mode / publisher-page scraping).
- [x] **Phase 1.6 UX feedback (Nov 15, 2026):**
    - [x] Swipe right to hide a card, with Undo snackbar
    - [x] Two-stage tap: first tap expands (summary + related coverage), second tap opens in Custom Tabs
    - [x] Lazy preview enrichment (jsoup): `og:description` + `og:image` from publisher pages — gives Google News cards summaries and hero images on first expand
    - [x] ⋮ pinned to the card's right edge (no overlay, no movement with title length)
    - [x] Story grouping: near-duplicate headlines cluster into one card with "Also covered by" list (title-similarity ≥ 0.5, 48h window)
    - [x] Hidden-story memory: hidden titles skipped at ingest for 30 days — hidden stories never reappear
    - [x] Tests: 74 passing; CI green (run 43733586963 — 2m37s)
- [x] **Phase 1.7 UX feedback (Nov 15, 2026):**
    - [x] Summaries now extract **article body paragraphs** (jsoup, junk-filtered, up to 3 paragraphs/600 chars) and prefer substantive text over headline-echoing `og:description`
    - [x] Old short summaries auto-upgrade on next expand (only replaced when meaningfully longer)
    - [x] Smoother swipe: hide fires **after** the dismiss animation completes, dismissed rows aren't re-grabbable, list reflow animated (`animateItem()`)
    - [x] Tests: 76 passing; CI green (run 43734319164 — 1m24s)
- [x] **Phase 1.8 — enrichment root-cause fix (Nov 15, 2026):**
    - [x] **Root cause found with live experiments:** Google News `/rss/articles/<id>` returns a JS shell (HTTP 200), never a redirect; resolution requires Google's batchexecute endpoint. Google changed the RPC id (`FbvAvj` → `Fbv4je`) and request envelope (`garturlreq`), so our resolver was silently failing every link (old POST → HTTP 400; verified against the maintained `googlenewsdecoder` implementation, which decoded a live link successfully)
    - [x] Resolver ported to the current algorithm (`buildFreq`/`parseBatchexecuteBody`, locale-param page URL, marker-row tolerance); verified on-device: GN link → NBC News URL → `desc=600 chars, image=true`
    - [x] Enrichment diagnostics: `DoomscrollGn` / `DoomscrollEnrich` logcat tags (resolution target, extraction length, failures)
    - [x] Preview **prefetch**: after each sync, top ~6 feed items are warmed so hero images/summaries appear without tapping
    - [x] Bug fix: auto-sync-on-open never fired (one-shot LaunchedEffect saw an empty source flow) — now keyed on source presence
    - [x] Tests: 78 passing; CI green (run 43734964275 — resolver fix; run 43735633898 — prefetch + auto-sync, 1m37s)
- [x] **Phase 2 (Nov 15, 2026) — media-page replacement + reader + filters (phone disconnected; core built & unit-verified):**
    - [x] **Glance home-screen widget** (`FeedWidget`): scrollable text feed (headline + source · time), tap row → reader deep link, tap header → app, dark styling matching the app; re-renders after every sync (UI refresh + WorkManager)
    - [x] **Reader mode** (`ReaderScreen`/`ReaderService`/`ArticleTextExtractor`): jsoup extraction with article→main→body fallbacks, junk filtering, 80-block/30k-char caps; cached in Room (`readerText`); A−/A+ font controls; "Original" opens the publisher
    - [x] **Filter rules** (`RuleEngine`/`RuleRepository`/`FilterRulesDialog`): hide by keyword or domain — applied at ingest, retroactively on creation, and logged as "Rule: …" in the reviewable filtered list
    - [x] DB v2 with additive migration (reader cache columns + `rules` table) — existing articles/sources survive
    - [x] Article card menu gains "Open in browser"; second tap now opens in-app reader (no publisher ads)
    - [x] Tests: 88 passing (added text extractor + rule engine suites); CI green (run 43736630176 — 2m35s)
    - [x] **Device verification (Nov 15):** feed with prefetch hero images, story grouping ("N related"), expand summaries with real body paragraphs, and reader mode all verified via screenshots on the S23 Ultra
    - [x] Reader extraction hardened from device findings: paragraphs-only, repeated-headline/byline/related-link filtering; extraction-cache versioning auto-invalidates stale text; CI green (run 43751108726 — 1m50s)
    - [x] **Widget fits rows to host size (Nov 15, One UI finding):** Samsung's launcher does not deliver scroll gestures to widgets, so scrollable lists always looked "partial". Widget now uses `SizeMode.Exact` + `LocalSize` to render exactly the rows that fit (no clipped row, auto-adjusts on resize); refreshes on app launch too — verified via screenshot; CI green (run 43751927947 — 1m41s)
    - [x] **Two widget variants (Nov 15):** `FeedWidget` = fit-to-host "front page" (One UI-friendly, footer "More in the app →"); `ScrollingFeedWidget` = full 24-item root-`LazyColumn` list that scrolls on launchers supporting scrollable collection widgets (Nova, Kvaesitso, Lawnchair, Pixel). One UI clips the scrolling variant — launcher limitation, confirmed via research + on-device swipe test (swipe on widget opens the app drawer); CI green (run 43752439583 — 2m2s)
- [x] **Pre-release security review (Nov 15, 2026):**
    - [x] Threat model: untrusted feed/article content is the only hostile input (no WebView, no accounts, no server, plain-text rendering only)
    - [x] **URL allow-listing** (`UrlSafety`): article links must be https; loopback/private/link-local/.local hosts blocked (lite-SSRF); Custom Tabs + share + image URLs limited to web schemes — with tests
    - [x] **Feed parser hardening:** DTD processing explicitly disabled (XXE / entity-expansion bombs); tests for external entities, bounded expansion, truncated XML, binary garbage
    - [x] **Response caps:** feeds 5 MB, article pages 2 MB (`HttpBodies.readCapped`); MockWebServer TLS tests for 304/ETag, oversize rejection, cleartext refusal, HTTP errors
    - [x] **Release logging guarded** behind `BuildConfig.DEBUG`; explicit cleartext-ban network security config
    - [x] **Release Lint:** no security findings; **manifest audit:** only required exported components (launcher activity, 2 widget receivers, WorkManager/Glance system-permission components); permissions = INTERNET + WorkManager internals
    - [x] Dependabot configured for Gradle + GitHub Actions
    - [x] Tests: 105 passing
    - [ ] Before public release: signed release APK + MobSF pass; enable Dependabot alerts on repo; optional Jazzer fuzz of the feed parser
- [x] **Branding decided (Nov 15, 2026): "Friendly Doom"** — plum `#1C1226` / cream `#FFF6E9` / coral `#FF5C7A` / lime `#C6FF4A`; doom-cloud adaptive icon (safe-zone checked, verified in app drawer); tagline "Same doom. No ads."; launcher label "Doomscroll", full name in store/README. Spec: `docs/branding/README.md`, board: `docs/branding/brand-board.html` (switched from Ink & Signal after live review); CI green (run 43762687623 — 1m45s)
- [x] **UX round 4 (Nov 15, 2026):**
    - [x] "Hide" removed from the card ⋮ menu (swipe-right is the only hide gesture); menu now Save / Share / Open in browser
    - [x] In-app help: ⓘ in the feed top bar (and "Help" on the empty state) opens a "How to use Doomscroll" sheet — sources, reading, organizing, widget setup, privacy
    - [x] Brand mark in-product: doom-cloud tile beside the title, in the empty state, and in the help header (icon and in-app logo share one vector); title shows "Doomscroll" without truncation
    - [x] Top-bar decluttered: Info + Refresh icons, "Mark all read" and "Filtered (n)" moved to an overflow menu
    - [x] Verified on device via screenshots (help sheet, brand sheet, top bar)
- [x] **v1.1 update work (from live feedback, Nov 16):**
    - [x] Reader renders structured blocks now: styled headings, bullets, hero + inline images (Coil), better spacing; cached as JSON, extraction-cache version bumped
    - [x] Paywall-aware reader failures ("behind a paywall — open the original") + explicit **Open original** action in the error state and in the expanded card when no summary exists
    - [x] More card images: preview prefetch raised to top 10 and runs whenever the top of the feed changes (cooldown-protected)
    - [x] Image quality: logo/icon/banner filtering + one lead image only + capped image height; preview falls back to first content image when og:image is missing
    - [x] Blocked publishers (NYT 403, WSJ/MarketWatch 401) get a specific "blocks in-app reading — open the original" message; verified on device
    - [x] Reader system-back returns to the feed (BackHandler) instead of leaving the app
    - [x] Verified on device (release build): richer feed images (NBC, WDSU, HN/OpenAI), NBC reader shows title → byline → hero → body, NYT shows blocked-publisher message, back returns to feed
- [x] **Release pass (Nov 15, 2026):**
    - [x] Release keystore generated (4096-bit RSA, 10,000 days, alias `doomscroll`) — stored outside the repo at `doomscroll-release/` with backup instructions; `keystore.properties` gitignored
    - [x] Gradle release signing config wired (auto-loads `keystore.properties`); version bumped to **1.0.0 (versionCode 10)**
    - [x] Release APK built: **2.78 MB** (R8 minified, from 9.4 MB debug); signature verified (SHA-256 `b4f9521d…`)
    - [x] `mobsfscan` source scan: **0 Android issues**
    - [x] README screenshots added (feed, reader, widget, help)
    - [ ] Install release build on device (requires uninstall of debug build — data reset, pending user approval)
    - [x] **Release 1.0.0 installed on device (Nov 15)** — old debug data reset as expected; fresh install, sources added through UI, sync + feed verified under R8; zero crashes
    - [x] **GitHub Release v1.0.0 published** — signed APK attached (SHA-256 `58B5365A…`), release notes with signing guidance; README install section updated
    - [x] F-Droid/IzzyOnDroid ready: fastlane metadata (title, short/full description, changelog 10, 4 phone screenshots) + recipe draft at `docs/release/fdroid-metadata.yml`
    - [x] **F-Droid build path validated:** release builds unsigned when `keystore.properties` is absent (exactly what the buildserver does) — `app-release-unsigned.apk` produced successfully
    - [x] Submission walkthrough written: `docs/release/fdroid-submission.md` (GitLab MR steps, likely reviewer requests, IzzyOnDroid lane)
    - [ ] Submit fdroiddata MR (requires the maintainer's GitLab account)
    - [x] **F-Droid MR submitted (Nov 16, 2026):** https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57695 — branch `doomscroll-ad-free-add` head `1194cb9e0`; metadata uses the full commit hash, AuthorName, and reproducible-build fields (Binaries + AllowedAPKSigningKeys)
    - [!] Fork pipeline blocked by GitLab identity verification ("Verify your identity to run this pipeline", 0 jobs). Per F-Droid template guidance: no phone/credit-card verification submitted; MR comment posted requesting maintainer-triggered CI
    - [!] **MR #57695 closed by maintainer `linsui` (Nov 16, 19:06 UTC):** "This app doesn't meet the policy that it needs to be maintained for more than 3 months." — a project-age gate, not a code/metadata issue. **Re-submission window opens ~Feb 16, 2027.** Keep shipping tagged releases until then (auto-update picks up new tags once included); metadata draft already refreshed to 1.1.0
    - [ ] Re-submit fdroiddata MR after 2027-02-16 (new branch, current metadata draft, request maintainer-triggered CI again)
    - [x] **Release v1.1.0 published (Nov 16):** reader structure (headings/bullets/images), preview prefetch 10, blocked-publisher UX, reader back navigation; signed APK + notes on GitHub
    - [x] **v1.1.1 (Nov 16):** hide-story snackbar now auto-dismisses (Material3 defaults to indefinite with an action label) with a close button, and replaces stale bars; release published to also exercise the release-triggered repo workflow
    - [!] **IzzyOnDroid ruled out (checked Nov 16):** tracking moved to Codeberg (`codeberg.org/IzzyOnDroid/repodata/issues`), no signed-in account here; and their App Inclusion Policy **explicitly rejects AI-generated / vibe-coded apps** — an honest request would be rejected, so none was posted. Self-hosted F-Droid repo (GitHub Pages) is the viable no-gate option
    - [x] **Self-hosted F-Droid repo live (Nov 16):** `https://iamoutofmana.github.io/doomscroll-ad-free/repo` (landing page at `/`); CI workflow `.github/workflows/fdroid-repo.yml` builds/signs the index with `fdroidserver` on every GitHub Release and deploys to Pages; repo key `fdroid-repo.jks` (fingerprint `93:93:39:66:…:20:60`) backed up in `doomscroll-release/` and stored as GitHub secrets; verified: signed indexes (v1+v2), v1.1.0 APK (`io.github.iamoutofmana.doomscroll_11.apk`), icon + 4 screenshots, localized metadata, APK hash matches index
    - [ ] Optional: Play Store track; signatures differ per distribution (uninstall to switch)
    - [ ] On-device widget visual pass (add via launcher, check taps/sizing on One UI) — user action
    - [ ] OPML import/export — next session
- [x] **Repo protection (Nov 17):** GitHub rulesets `Protect main` (blocks deletion + force-push on `main`) and `Protect release tags` (blocks deletion/moving of `v*` tags), both active with no bypass; normal fast-forward pushes and tag creation unaffected. F-Droid/repo automation depends on stable tags, hence tag protection.
- [x] **Hardening (Nov 17):** immutable releases enabled (applies to releases published from now on: assets and tag locked, release attestation generated; notes still editable), Dependabot security updates enabled, CI token reduced to `contents: read`, broaden gitignore for signing material (`*keystore*`, `*.jks*`, `*.p12/.pfx/.pem/.key`); secret scanning + push protection were already on. Actions remain tag-pinned (SHA pinning deferred).
- [ ] 3 days of daily driving without Samsung's media page → **Phase 1 DoD** (in progress — collecting annoyances for Phase 2)

---

## Appendix A — Source endpoint catalog (turnkey reference)

**Google News RSS** (all take `hl` / `gl` / `ceid` locale params that must agree, e.g. `hl=en-US&gl=US&ceid=US:en`):
- Top stories: `https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en`
- Topics: `…/rss/headlines/section/topic/{WORLD|NATION|BUSINESS|TECHNOLOGY|ENTERTAINMENT|SPORTS|SCIENCE|HEALTH}?…`
- Local/geo: `…/rss/headlines/section/geo/{City}?…`
- Keyword search: `…/rss/search?q={query}&…` — supports operators: `intitle:`, `allintitle:`, `site:reuters.com`, `OR`, `when:1h|1d|7d|1y`, quoted exact phrases. Up to ~100 items.
- Item links are `news.google.com/rss/articles/<base64>` redirects → decode via `batchexecute` technique (see `googlenewsdecoder`) or open the redirect directly.

**YouTube:** `https://www.youtube.com/feeds/videos.xml?channel_id=UC…` (or `?playlist_id=PL…`)

**Podcasts:** any podcast RSS URL (discover via PodcastIndex.org); OPML import from existing apps

**Reddit:** `https://www.reddit.com/r/{sub}/.rss` (also `search.rss?q=…`); **requires custom User-Agent** like `DoomscrollAdFree/0.1 (github.com/<you>/doomscroll-ad-free)`

**Lemmy:** `https://{instance}/feeds/c/{community}.xml?sort=New`

**Hacker News:** `https://hn.algolia.com/api/v1/search?tags=front_page` (JSON, no key)

**Generic feeds:** RSS 2.0, Atom, JSON Feed 1.1; parser must handle `media:content` + enclosures (images/audio/video), `content:encoded`, CDATA, charset quirks, relative-URL resolution, and Dublin Core dates.

## Appendix B — Sync behavior

Conditional GET (`ETag` / `If-Modified-Since`) · per-source sync intervals · staggered WorkManager jobs · Wi-Fi-only option · battery-not-low constraint · sync on app open + phone unlock (widget freshness).
