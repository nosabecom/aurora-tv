# Implementation audit

The private derivative starts from CleanYourTV commit
`86a69372f9886ab6392ad2c3cf95f9a0e1c54644` and retains its MIT and Outfit OFL notices. It is separate
from Kathrine. Substantial photo networking belongs in this independent repository rather than an
upstream theme or contribution. Cosy Home supplies the visual reference for the large light clock.

## Interface and firmware behavior

Keep the original app tiles, grid, dock, library, reorder, module-card behavior, Inputs, network/VPN pill,
and direct system Settings action. The focused change enlarges the clock to 150sp, makes the existing
localized greeting more visible, places the date below the clock, and keeps the original controls at the
top right. No custom app-grid layout is added. The dock profile remains Netflix, Prime Video, Disney+, YouTube.

HomeKeyService, InputKeyService, RemoteKeys and AppAdapter remain unchanged from the audited upstream.
The saved Google TV window redirect and key_filter=false are preserved. Google TV and setupwraith stay
enabled. No assistant, casting, firmware update, streaming, Bluetooth, input, keyboard, Play Store/Services,
Tailscale, or default-HOME priority changes are part of this implementation.

The new package remains com.nosabecom.auroratv; the original com.gathin.tvhome stays installed for rollback.
Both packages initially registered the same market://launch bridge. A read-only on-device query found two
handlers and Android's ResolverActivity as the result. The duplicate AppLaunchActivity in the new package
is disabled in its manifest; the original package supplies the unchanged bridge. This removes that link
ambiguity without modifying the rollback package. It does not by itself prove the physical remote's OEM
shortcut path uses that URI; real shortcut behavior must still be checked.

## Current photo access and protocol review

Google removed broad/shared-album Library API access after March 31, 2025:
[API changes](https://developers.google.com/photos/support/updates). Ambient API requires partner
acceptance: [partner program](https://developers.google.com/photos/partner-program/overview).
Picker requires explicit selection and does not subscribe to future contributions automatically.

The opt-in adapter reads the supplied anonymous shared-page bootstrap and continuation responses, rather
than the removed Library API. This web format is undocumented and can change. Protocol research was
reviewed against the pinned source credited in THIRD_PARTY_NOTICES.md; no third-party package or installer
is executed/imported. Android uses native JSON and HTTPS. A JSON dependency is pinned only for JVM tests.

Full live enumeration by both the initial audit and the Java adapter observed 746 unique media items,
678 photos, 68 videos, across3 pages. These are observations, not limits. The shared URL is a bearer capability
and belongs only in private internal app configuration. There are no Google account credentials/cookies,
analytics, downloaded executable code, public photo endpoints, or WebView integration.

Every page must complete, tokens must advance, and unique media count must match the album's reported
size. The 1,000-page resource bound is an explicit error, never a partial success. Video entries are counted
and excluded. There is no 100-photo cap in the family-photo cache/player.

## Data, network and lifecycle boundaries

Added normal permissions: Internet and boot completion for persisted JobScheduler. An app-local signature
permission protects photo-status broadcasts. There are no media-library, storage or account permissions.
Every request/redirect validates HTTPS, allowlisted Google Photos/image host, port and user-info. Responses,
image dimensions and download sizes are bounded. Three download workers stay off the UI thread.

ThemeProvider retains WRITE_SECURE_SETTINGS caller protection and its path allowlist. photos.json is mapped
to internal storage, separate from external theme files. The protected photo-status query exposes only
counts, timestamps and local hashes; never sharing/photo URLs, account data or credentials. App backup is off.
Credentials, cached photos, manifest, rotation ledger, screenshots, APKs and signing keys stay out of Git.

A complete membership snapshot is published atomically only after every eligible photo is cached and
validated. A failed/partial request, parser change or storage shortage retains the last complete cache and
records paused status. Successful downloads are reused on retry. Deletions are applied only after successful
full reconciliation; offline operation cannot know about remote deletions until a later successful sync.

The player preserves the original UI and adds only a photo background layer. Photos fill the display with
center cropping, including portraits. A translucent shade preserves text contrast. The original local
wallpaper remains a fallback while the initial complete cache is unavailable.

The current photo, source identity, timestamp and seen set commit before presentation. Only decoded photos
are marked. A failed history commit pauses rotation. The ledger survives app restarts, standby and reboot;
resuming a current frame is not a fresh selection. Additions join the unseen set without resetting survivors,
deletions leave eligibility, and another cycle begins only after all eligible photos are seen. Immediate
cycle-boundary repeats are avoided when more than one photo exists.

Decoding is bounded to 1920×1080 and runs on one worker. Home stops its rotation timer while hidden and
releases images on memory pressure. Android owns roughly 15-minute network-constrained persisted sync jobs;
standby policy can defer them. No wake lock or always-running polling service is added.

## Checks

The Android suite includes full-cycle reconstruction, additions/deletions, empty/singleton collections,
failed decode eligibility, multi-page enumeration beyond 300 items, count mismatch, repeated tokens, sharing
failure and URL restrictions. Physical verification also checks counts/current-photo history through the
protected maintenance query, screenshots, direct system Settings, network/Inputs controls, and Home behavior.
The family cache/ledger from earlier versions is reused without clearing data.

The personal derivative's APK budget is 640 KiB; the upstream no-network launcher budget was 512 KiB. The
photo implementation adds no runtime library and remains below this bounded personal-build budget. The
maintenance bot is archived; add-on execution is gated to upstream. Private CI builds/tests/lints without
publishing binaries. Device-specific logs, endpoints, credentials and screenshots are private operator records.

## Approved build

Version 1.1/code 102 passed 240 Android unit tests, release lint and assembly on the build host. The tooling
suite ran 874 tests successfully, with 12 skips. The release APK is 562,973 bytes, below the 640 KiB personal
build budget. SHA-256: `bbd44849af721cd474e154bbcce5704a9762f2bdbe5b4e6e4d11679f42974990`.
The emulator preview confirmed the large clock, greeting and full-screen family-photo renderer; all 678 cached
photos remain eligible and the retained shuffle ledger advances without resetting. A 24-hour clock frame
was also checked before approval. Screenshots and live state remain private.
