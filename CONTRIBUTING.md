# Contributing to Doomscroll Ad Free

Thanks for wanting to help! The project is in early phased development — see [`docs/PLAN.md`](docs/PLAN.md) for the roadmap and how phases are defined.

## Ground rules

- **No ads, no trackers, no proprietary blobs — ever.** Dependencies must be FOSS (F-Droid compatible), and the app may only make network calls to feeds the user explicitly configures.
- Code style is enforced by ktlint. Run `./gradlew ktlintCheck` (or `ktlintFormat`) before committing.
- Keep PRs focused: one feature or fix per PR, referencing the phase it belongs to.

## Development setup

1. JDK 17
2. Clone and open in Android Studio, or build from the CLI with `./gradlew assembleDebug`
3. Full check: `./gradlew ktlintCheck test assembleDebug`

## Pull requests

1. Fork and branch (`feature/…` or `fix/…`)
2. Commit with clear messages
3. Open a PR describing what and why; link any related issue
4. CI (ktlint, unit tests, debug build) must pass

## Reporting bugs

Use GitHub issues. Include: device, Android version, the feed URL (if relevant), and what you expected vs. what happened.
