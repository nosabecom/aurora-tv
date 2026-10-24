# Audit

This independent private repository retains the audited CleanYourTV source at
86a69372f9886ab6392ad2c3cf95f9a0e1c54644 and its MIT/Outfit notices. It is separate from Kathrine.
The final launcher preserves the complete upstream MainActivity, layout resources, AppAdapter and
LauncherSettingsActivity. MainActivity has no changes from the audited upstream implementation.

The original app tile design and default clock/greeting remain. All original top-bar controls remain.
The theme explicitly sets top_menu=false and launcher_settings_button=false: Settings opens Android TV
system settings directly. Wi-Fi/network and Inputs pills remain enabled. Wallpaper is a locally supplied
image, rendered by upstream WallpaperPlayer using CENTER_CROP; no network is needed for Home.

The new app ID is com.nosabecom.auroratv. The original com.gathin.tvhome installation and private baseline
APK remain available for rollback. HomeKeyService, InputKeyService and RemoteKeys are unchanged. Resource
defaults preserve the Google TV window redirect and key_filter=false. Google TV and setupwraith remain
enabled. No debloat, network, Tailscale, default-HOME priority, assistant, casting or streaming changes.

The rejected replacement UI and photo-sync implementation are absent from the shipped source and APK.
There is no INTERNET permission, photo job component, new runtime dependency, remote photo request or
credential provisioner in the final app. Private experiment records are outside Git. Existing private app
data is not cleared; old jobs were canceled during rollback and their component is removed.

The original photo-access research remains useful context: Library API shared-album access was removed
in March 2025 (https://developers.google.com/photos/support/updates), and Ambient API requires partner
acceptance (https://developers.google.com/photos/partner-program/overview). An anonymous full-album audit
observed 746 media items across three pages, including 678 photos and 68 videos. No sharing URL, credential,
photo, screenshot, runtime ledger, APK or signing key is committed. The final app uses a single local wallpaper.

The original unit tests and release lint/build are the release checks. The firmware key-filter test enforces
false for this derivative. The upstream maintenance bot is archived and add-on execution is gated to the upstream repository.
Private CI validates the inherited tooling and this launcher without publishing binaries. See deployment notes for final physical verification.

## Source/build verification

Final source identity checks confirm MainActivity, LauncherSettingsActivity, AppAdapter, HomeKeyService,
InputKeyService, RemoteKeys, the manifest and all layout resources match the pinned upstream source.
The deliberate differences are application identity/version, launcher display name, firmware redirect/key-filter
defaults, the local deployment profile, private-repository instructions and maintenance workflow gating.

On the Windows build host, release 1.0.1/code101 passed 226 Android unit tests, release lint with zero
errors, and assembly. The inherited Python suite ran 874 tests successfully (12 skips).
APK size: 509,081 bytes. APK SHA-256:
`606b1a78d6d6ba52f034c035274b27406fd3f181241b1e1729b237d46751f5b8`.
The final manifest has no INTERNET permission, new job service or additional runtime dependency.

On the Android TV emulator, the local wallpaper rendered using the original full-screen renderer and
clicking the original Settings pill focused `com.android.tv.settings/.MainSettings` directly.
Private screenshots are retained locally, not in this repository. Physical-TV deployment/closure records
remain private; casting diagnosis holds exclusive device ownership until explicitly returned.
