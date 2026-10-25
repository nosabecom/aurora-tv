# Implementation audit

This private derivative starts from CleanYourTV commit
`86a69372f9886ab6392ad2c3cf95f9a0e1c54644`, retains MIT/Outfit OFL notices, and is separate from Kathrine.
Cosy Home supplies the visual reference for the large light clock; no scene/weather code is imported.

## Current interface

The original app tiles, dock, library, reorder, module cards, Inputs, network/VPN pill and direct system
Settings action remain. The clock is 150sp with a visible localized time-sensitive greeting and date below.
The dock profile remains Netflix, Prime Video, Disney+, YouTube. The existing app-size setting is retained.

Down from the dock shrinks the clock, date and greeting into the top-right corner and slides the app rows
into the expanded grid. The original controls stay fixed at the top right; the compact header sits below them. Up to the dock, Back from the apps, or Home restores
the large header. A 320 ms transform animation follows one layout pass; reduced motion applies it immediately.
There is clearance above the first app row for zoom/lift/glow, including when scrolling back upwards.
Grid resizing waits for layout before restoring the focused tile and highlight.

At the owner's request, the family-photo renderer, sync jobs, shared-album parser and network permissions
were removed. The deployment profile uses a single local Aurora wallpaper with rotation disabled. Old
private photo cache/history can remain inert without clearing app data; the private source configuration
and scheduled photo jobs are removed during this update. No photos or sharing URLs belong in Git.

## Firmware and launch paths

HomeKeyService, InputKeyService, RemoteKeys and AppAdapter remain unchanged from the audited upstream.
Preserve stock_home_redirect=com.google.android.apps.tv.launcherx and key_filter=false. Google TV and
setupwraith stay enabled; do not change default-HOME priority, assistant, casting, firmware updates,
streaming apps, Bluetooth, inputs, keyboard, Play Store/Services or Tailscale.

The package is com.nosabecom.auroratv; original com.gathin.tvhome stays installed for rollback. Both initially
registered market://launch. A live query found two handlers and Android's ResolverActivity. The duplicate
AppLaunchActivity in the new package is disabled; the original supplies the unchanged bridge. This removes
link ambiguity without modifying rollback. Physical OEM shortcut behavior still requires a real remote test.


## Native app-shortcut launches

App shortcuts opening from Home does not establish that app-to-app switching works. Android 12's
[ActivityStarter](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-12.0.0_r1/services/core/java/com/android/server/wm/ActivityStarter.java)
uses the active Home process for its launcher background-start exception, before falling back to the
registered Home app. Aurora can be that active process while Google TV retains the Home role/resolver.
The same Android 12 code permits background starts for callers allowed SYSTEM_ALERT_WINDOW.

A failed app-switch trace identified rejected background starts from Google TV, together with rejection
of its declared SYSTEM_ALERT_WINDOW permission. The targeted device fix allows only Google TV's package
app-op for that permission. It changes no launcher code, Home role, key filtering, accessibility state,
package enabled state or network setting. Save the prior mode privately for rollback. Physical verification
of Netflix → YouTube and YouTube → Netflix is pending; a shell-launched activity cannot certify this path.

## Earlier photo-access research

Google removed broad/shared-album Library API access after March 31, 2025:
[API changes](https://developers.google.com/photos/support/updates). Ambient API requires partner acceptance:
[partner program](https://developers.google.com/photos/partner-program/overview). Picker selection does not
subscribe to future contributions. The removed prototype used an undocumented anonymous web protocol,
fully enumerated across pages rather than assuming a 100-photo limit. That integration is no longer shipped.
Historical research attribution is retained in THIRD_PARTY_NOTICES.md.

## Validation

Build, Android unit tests and release lint run on the build host using the retained signing key. Emulator
checks cover the large Home clock, Down into apps, further navigation, Up/Back/Home restoration, and static
wallpaper. Live checks preserve helper binding, original redirect configuration, network/VPN connectivity,
package enabled states and the sole launch-URI handler. Screenshots, APKs, runtime data, source URLs,
credentials and private device records stay outside Git. Physical remote verification cannot be replaced
by injected key events.

Private CI tests/builds/lints without publishing binaries. The personal APK limit remains 640 KiB. The
maintenance bot is archived and add-on execution is gated to upstream. Casting troubleshooting was ended
by the owner: Google Home mirroring is the accepted path; S21 SmartView remains deferred, not fixed.

Version 1.1.3/code 105 passed the 226-test Android suite, release lint and assembly on the build host.
The tooling suite ran 874 tests successfully (12 skipped). The release APK is 511,253 bytes, SHA-256
`dd63534ad5d4832f007e4ef8dbd2c8074da37d688b349194a21a488eb8fd9db6`.
