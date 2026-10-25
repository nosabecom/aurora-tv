# Deployment and rollback

Build on the existing Windows build host with its retained signing key, JDK 17, Android SDK 34 and Gradle 8.9:

```powershell
.\gradlew.bat --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease
```

Keep the original CleanYourTV installed. Install the new com.nosabecom.auroratv APK alongside it; Java
components remain under com.gathin.tvhome. Use explicit new package/components, since inherited tools default
to upstream. Do not run the HOME/debloat installer on this firmware. Keep device/host identifiers, ADB port,
keys and private recovery records outside Git. Coordinate exclusive TV ownership and announce interruptions.

Provision deploy/config.json and a single private local wallpaper.png (existing JPG themes remain supported) through the permission-protected
<applicationId>.theme provider. Rotation is disabled. Before updating a photo-enabled build, remove its
internal photos.json through the old provider and cancel its own JobScheduler jobs 4101/4102. The new build
has no photo renderer, sync service or Internet permission. Preserve app data and private rollback records.

The header retains the large clock and greeting. Down from the dock gives the app rows the screen by
scaling the clock/date/greeting in place at the top left; Up to the dock, Back, or Home restores the large header.
The transition scrolls the grid while scaling the existing text, without changing fonts or moving the
original top-right controls. Reduced motion applies the final state immediately. The first focused row
has clearance for its glow. Enable wallpaper_true_color for 32-bit decoding; keep the original desktop
PNG bytes lossless in wallpaper.png through the protected provider. Personal wallpaper files are not bundled or committed.
Settings opens Android TV settings directly. The dock remains Netflix, Prime Video, Disney+, YouTube.

The retained baseline package owns market://launch handling; the duplicate AppLaunchActivity is disabled
only in the new manifest to avoid an ambiguous resolver while both packages stay installed. Do not disable
or uninstall the baseline package. Physical OEM app-shortcut behavior requires a real remote test.


If native app buttons work from Home but fail inside apps, inspect ActivityTaskManager background-start
denials and map the caller UID to its package. On this firmware the rejected caller was Google TV, which
remains the registered Home app while Aurora is the active Home process. Google TV declares the overlay
permission, but its default app-op rejected it. Record its existing mode in a private recovery file before
applying this package-only exception:

```powershell
& $adb -P $adbPort -s $tvDevice shell cmd appops get --user 0 com.google.android.apps.tv.launcherx SYSTEM_ALERT_WINDOW
& $adb -P $adbPort -s $tvDevice shell cmd appops set --user 0 com.google.android.apps.tv.launcherx SYSTEM_ALERT_WINDOW allow
```

This is the system's Display over other apps permission for the stock Google TV package. Verify real
Netflix → YouTube and YouTube → Netflix shortcut presses afterward. If it does not resolve the traced
failure, restore the saved mode with the same set command; do not assume another device's prior mode.
Keep key_filter=false and the original Home redirect. No helper rebind or APK update is needed.

Grant the declared location permission only for the original Wi-Fi name display, as upstream's installer does.
No WRITE_SECURE_SETTINGS grant to the app is required. Replace only the baseline HomeKeyService entry with
com.nosabecom.auroratv/com.gathin.tvhome.HomeKeyService, preserving all unrelated helpers. Do not run both
redirectors together. Preserve stock_home_redirect=com.google.android.apps.tv.launcherx and key_filter=false.
Never disable Google TV/setupwraith or change network, Tailscale, default-HOME priority or streaming state.

Verify APK hash, static wallpaper, Down/Up/Back/Home navigation, original controls, direct Settings,
screenshots and real remote buttons. After a normal reboot verify helper binding and network/VPN connectivity;
an existing Tailscale app launch may be needed to bring its retained connection back. Do not change its account,
routes, preferences or exit-node choice as part of launcher work. Casting troubleshooting is deferred by the owner; do not reopen it during launcher verification.

Rollback restores the exact saved accessibility values to com.gathin.tvhome/.HomeKeyService and starts the
original MainActivity. Both packages stay installed; no data clear, reinstall or network reset is necessary.

After final physical verification, close USB/network debugging, revoke ADB trust and verify the network ADB
port is closed from the build host. Preserve private closure evidence. Announce any required normal restart;
never interrupt someone watching without coordinating an available window.
