# Publishing Doomscroll Ad Free on F-Droid

F-Droid builds apps **from source** on its own build servers and signs them with the
**F-Droid key**. That build path is already verified locally: with `keystore.properties`
absent, `./gradlew assembleRelease` succeeds and produces `app-release-unsigned.apk`.

The only step that requires a human account is the merge request to
[fdroiddata on GitLab](https://gitlab.com/fdroid/fdroiddata). Everything else is prepared:

- `metadata/io.github.iamoutofmana.doomscroll.yml` ← copy from `docs/release/fdroid-metadata.yml`
- Fastlane metadata + 4 phone screenshots in `fastlane/metadata/android/en-US/`
- Tag `v1.1.0` pushed (metadata draft updated to 1.1.0 / versionCode 11); Gradle wrapper executable; all dependencies FOSS

## Submission status (Nov 16, 2026)

MR [!57695](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57695) was reviewed and
**closed** by maintainer `linsui`:

> Closing because: This app doesn't meet the policy that it needs to be maintained for more
> than 3 months.

This is a project-age gate applied to first-time submissions, not a code or metadata
problem. **Do not resubmit before ~February 16, 2027.** In the meantime:

- keep tagging releases (F-Droid's `UpdateCheckMode: Tags` will pick them up once the app is
  included);
- keep `docs/release/fdroid-metadata.yml` in sync with the latest release (already at 1.1.0);
- consider IzzyOnDroid (below) for distribution without the age gate.


## Step-by-step (≈10 minutes)

1. **Sign in to GitLab** — https://gitlab.com (GitHub sign-in works).
2. **Fork** `fdroiddata`: https://gitlab.com/fdroid/fdroiddata/-/forks/new
   (project: https://gitlab.com/fdroid/fdroiddata — group `fdroid`, project `fdroiddata`)
   Update checks / MR list: https://gitlab.com/fdroid/fdroiddata/-/merge_requests
   Optional tracking issue (submission queue): https://gitlab.com/fdroid/rfp/issues
3. **Add the metadata file** in your fork:
   - Path: `metadata/io.github.iamoutofmana.doomscroll.yml`
   - Content: everything in `docs/release/fdroid-metadata.yml` (without the leading
     comment block is fine — comments are allowed too)
   - Suggested branch name: `io.github.iamoutofmana.doomscroll`
   - Commit message: `New app: Doomscroll Ad Free (io.github.iamoutofmana.doomscroll)`
4. **Open a merge request** from your branch to `fdroiddata:master`.
   - Title: `New app: Doomscroll Ad Free`
   - In the description, use the “App inclusion request” template and include:
     - Source: https://github.com/iamoutofmana/doomscroll-ad-free
     - License: GPL-3.0-or-later
     - Why it qualifies: fully FOSS dependencies, no tracking/analytics, no network
       calls except user-configured feeds, no ads or sponsored content.
     - Note: the release keystore lives outside the repo; F-Droid should sign with
       its own key.
5. **Respond to review**. F-Droid reviewers will run `fdroid lint` and their build
   pipeline in the MR. Typical requests:
   - add a `prebuild` line if the buildserver needs an SDK platform installed, e.g.
     `prebuild: echo "y" | sdkmanager "platforms;android-35" "build-tools;34.0.0"`
   - adjust `UpdateCheckMode`/`AutoUpdateMode`
   - add `AllowedAPKSigningKeys` only if reproducible builds are configured later
6. **After merge**: the build lands in the next F-Droid index update — usually days to
   a couple of weeks. Then the app is installable from the F-Droid client.

## Optional faster lane: IzzyOnDroid

IzzyOnDroid repackages releases sooner. Submit by opening an issue at
https://gitlab.com/IzzyOnDroid/repo/-/issues with:
- App name, source repo, and the GitHub release URL
  (https://github.com/iamoutofmana/doomscroll-ad-free/releases/tag/v1.0.0)
- Note that fastlane metadata is already in the repo (they read it automatically)

## Distribution warning (put this in the README if not already)

- GitHub build → signed with the **project key**
- F-Droid build → signed with the **F-Droid key**
- Switching between them requires uninstalling (Android signature rule)

Pick one channel and stay on it; that is normal for FOSS Android apps.
