# Implementation audit

This private derivative starts from CleanYourTV commit
`86a69372f9886ab6392ad2c3cf95f9a0e1c54644`, retains MIT/Outfit OFL notices, and is separate from Kathrine.
Cosy Home supplies the visual reference for the large light clock; no scene/weather code is imported.

## Current interface

The original app tiles, dock, library, reorder, module cards, Inputs, network/VPN pill and direct system
Settings action remain. The clock is 150sp with a visible localized time-sensitive greeting and date below.
The dock profile remains Netflix, Prime Video, Disney+, YouTube. The existing app-size setting is retained.

Down from the dock scales the existing clock, date and greeting in place at the top left while the app
rows scroll into view. The original controls stay fixed at the top right. A fixed viewport avoids font
changes and repeated grid reflow during the animation; reversing direction retains the current frame.
Up to the dock, Back or Home restores the large clock. The first app row keeps clearance for focus/glow.
The adapter uses the original Home height budget when sizing tiles for additional visible rows.

At the owner's request, the family-photo renderer, sync jobs, shared-album parser and network permissions
were removed. The deployment profile uses a single private local pixel-art wallpaper with rotation disabled and 32-bit colour decoding. Old
private photo cache/history can remain inert without clearing app data; the private source configuration
and scheduled photo jobs are removed during this update. No photos or sharing URLs belong in Git.

## Firmware and launch paths

HomeKeyService, InputKeyService and RemoteKeys remain unchanged from the audited upstream.
AppAdapter retains the original tiles, with an inset correction for the stable header viewport.
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
package enabled state or network setting. Save the prior mode privately for rollback. The owner physically verified both Netflix → YouTube and YouTube → Netflix after this change.
A shell-launched activity cannot certify this path.

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

Candidate version 1.2/code 106 passed the 227-test Android suite, release lint and assembly on the build
host. The release APK is 511,641 bytes, SHA-256
`e61bfadef41184e1f6d88f8a9c0555fe5c316e86786623af54a2da9b04d8b22a`.
The tooling suite ran 874 tests successfully (12 skipped). Emulator screenshots and a recording verified
the left-anchored transition, first-row clearance, fixed navigation and rapid direction reversals without
runtime crashes. The final build also supports a genuine single-wallpaper PNG through the protected
provider, with JPG fallback covered by regression tests. Preview review and physical deployment remain
pending; the TV is on code 105 and stays undisturbed while the family watches.
