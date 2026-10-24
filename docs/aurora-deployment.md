# Deployment and rollback

Use the build host's existing signing key, JDK17, Android SDK34 and checked-in Gradle8.9 wrapper. Build:

```powershell
.\gradlew.bat --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease
```

Back up existing package/accessibility states privately. Install the new package alongside CleanYourTV;
never remove the baseline. Use explicit com.nosabecom.auroratv package/components, since inherited tools
default to the upstream package. Java components remain under com.gathin.tvhome. Do not run the upstream
HOME/debloat installer on this firmware. Keep endpoints, ADB port, device identity and signing keys private.

Push a local config.json and wallpaper.jpg through the permission-protected <applicationId>.theme provider.
The config preserves the original four-app dock and enables app_library, tile_menu, and module_cards. Keep
stock_home_redirect set to com.google.android.apps.tv.launcherx and key_filter=false. Use top_menu=false,
launcher_settings_button=false, show_network_pill=true and show_inputs_pill=true. Wallpaper rotation is off.
The local wallpaper fills/crops through the original renderer. No family photo or sharing link belongs in Git.

Grant the declared location permission only for the original Wi-Fi name display, as the upstream installer
does. No WRITE_SECURE_SETTINGS grant to the app is needed. Launch the explicit MainActivity and verify
full original Home UI, Wi-Fi status, direct system Settings, Inputs, all apps, wallpaper and D-pad behavior.

Replace only the baseline HomeKeyService entry in the existing enabled accessibility list with
com.nosabecom.auroratv/com.gathin.tvhome.HomeKeyService, preserving all unrelated helpers. Do not run both
redirectors at once. Never disable Google TV or setupwraith or change the firmware HOME priority.

Reboot and verify the helper rebound, unchanged disabled-package list, connected Tailscale, and returning
Home from a streaming app/Kathrine. Ask for real remote Home, Back, D-pad, held volume, Input, assistant and
dedicated streaming buttons. Injected keys are not proof for accessibility behavior.

Rollback: restore the exact saved accessibility settings to com.gathin.tvhome/.HomeKeyService, and start
the baseline MainActivity. Both packages stay installed; no network reset, data clearing or reinstall needed.

After final physical confirmation, close debugging, revoke ADB trust, reboot normally and verify network
ADB is closed from the build host. Coordinate with the issues thread before ending access. Do not claim
closure merely from writing a setting. All screenshots/recovery artifacts stay in private storage.
