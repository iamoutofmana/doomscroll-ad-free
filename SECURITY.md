# Security Policy

## Design posture

- The app collects nothing: no accounts, no telemetry, no crash reporting.
- Network access is limited to feeds the user explicitly configures, plus the article pages whose previews the user requests.
- No dynamic code loading, no proprietary blobs; all dependencies are FOSS.
- Feed content is treated as untrusted input: parsed defensively and sanitized before rendering (hardened further with Phase 2's reader mode).

## Reporting a vulnerability

Use GitHub's **private vulnerability reporting** on this repo (Security → "Report a vulnerability"). Include reproduction steps and affected versions. Please avoid public disclosure until a fix is released.

## Supported versions

During pre-1.0 development, only the latest tagged release receives security fixes.
