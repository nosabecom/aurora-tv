# Deployment and rollback

Build on the existing Windows build host with its retained signing key, JDK 17, Android SDK 34 and Gradle 8.9:

```powershell
.\gradlew.bat --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease
```

Keep the original CleanYourTV installed. Install the new com.nosabecom.auroratv APK alongside it; Java
components remain under com.gathin.tvhome. Use explicit new package/components, since inherited tools default
to upstream. Do not run the HOME/debloat installer on this firmware. Keep device/host identifiers, ADB port,
keys and private recovery records outside Git. Coordinate exclusive TV ownership and announce interruptions.

Provision deploy/config.json through the permission-protected <applicationId>.theme provider. Keep a local
wallpaper.jpg as a fallback. A private photos.json with shared_album_url goes through the same provider,
which maps it to internal storage. Never log/commit the real URL or personal images. The player reuses the
complete internal photo cache and persisted family_photo_cycle ledger; never clear app data during updates.

The header has the large clock and visible localized greeting. App tiles and all original controls remain.
Settings opens Android TV settings directly. The four-app dock remains Netflix, Prime Video, Disney+, YouTube.
Family photos fill/crop the screen and rotate once per minute while Home is visible. New contributions sync
when Android's network/standby policy permits. Use the protected photo-status query to inspect total/eligible/
cached counts, excluded videos, page count and durable current/seen state without exposing private URLs.

The retained baseline package owns market://launch handling; the duplicate AppLaunchActivity is disabled
only in the new manifest to avoid an ambiguous resolver while both packages stay installed. Do not disable
or uninstall the baseline package. Physical OEM app-shortcut behavior requires a real remote test.

Grant the declared location permission only for the original Wi-Fi name display, as upstream's installer does.
No WRITE_SECURE_SETTINGS grant to the app is required. Replace only the baseline HomeKeyService entry with
com.nosabecom.auroratv/com.gathin.tvhome.HomeKeyService, preserving all unrelated helpers. Do not run both
redirectors together. Preserve stock_home_redirect=com.google.android.apps.tv.launcherx and key_filter=false.
Never disable Google TV/setupwraith or change network, Tailscale, default-HOME priority or streaming state.

Verify APK hash, original controls, full photo count, rotation/history persistence, direct Settings, screenshots,
Home return and real remote buttons. After a normal reboot verify helper binding and network/VPN connectivity;
an existing Tailscale app launch may be needed to bring its retained connection back. Do not change its account,
routes, preferences or exit-node choice as part of launcher work. Coordinate casting checks before debug closure.

Rollback restores the exact saved accessibility values to com.gathin.tvhome/.HomeKeyService and starts the
original MainActivity. Both packages stay installed; no data clear, reinstall or network reset is necessary.

After final physical verification, close USB/network debugging, revoke ADB trust and verify the network ADB
port is closed from the build host. Preserve private closure evidence. Announce any required normal restart;
never interrupt someone watching without coordinating an available window.
