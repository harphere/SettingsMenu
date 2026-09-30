# Settings Menu Hider 1.0.1

Standalone legacy LSPosed / Vector Legacy Bridge module for Android Settings.
Designed for your Android 16 / Vector phone. Phone compatibility still needs testing.

## Build the APK with GitHub Actions

1. Extract this ZIP. Upload the **contents** of SettingsMenuHider to a GitHub repository root, including `.github/workflows/build.yml`.
2. Open Actions → Build APK. The workflow runs on push; it can also be run manually.
3. Open the completed run. Download **SettingsMenuHider-v1.0.1-APK** under Artifacts.
4. Extract that artifact and install `app-debug.apk`.

No obsolete `tools` SDK package, setup-android action, compileSdk 37 dependency,
or modern libxposed API is used. The workflow uses Gradle 8.11.1, Java 17,
AGP 8.9.2 and compileSdk 35. An Android 16 phone can run this APK.
The APK is debug-signed; uninstall a prior build if an update reports an incompatible signature.

## Enable and discover

1. Enable **Settings Menu Hider** in LSPosed / Vector Legacy Bridge.
2. Scope **only Settings (`com.android.settings`)**. No System Framework,
   System UI, Launcher, Gboard or Digital Wellbeing scope is needed.
3. Reboot once after enabling the module.
4. Open the main Android Settings menu, then return to the module app.
5. Check **Digital Wellbeing & parental controls**, or any other discovered row,
   to hide it. A checked row means **hide**.
6. Close Settings from Recents and reopen it after configuration changes.
   A reboot is not normally necessary for changing selections.

Starts with no entries hidden. Inventory is collected from your Settings homepage,
including dynamically added entries, rather than guessed from a preset title list.
Titles and keys are searchable. A scan timestamp helps distinguish a working hook
from an app that has not yet received an inventory.

## Behavior and recovery

- Hide only. Search, direct Intents and other navigation paths remain available.
- No packages are disabled or uninstalled. No Settings APK is modified.
- The module hides preferences in place, retains their hierarchy and tracks their
  original visibility. It does not force ROM-hidden entries visible.
- Only `TopLevelSettings` homepage trees are registered. Submenu trees are not scanned.
- Identity is the preference key, falling back to the Intent or fragment class.
  Entries without a stable identifier are left untouched.
- The provider uses private preferences and accepts only the module UID or a UID
  associated with `com.android.settings` (Settings can share a system UID).
  External calls can read selections and publish discovery, never change selections.
- **Restore all menu entries** clears selections. The master switch disables hiding.
  Close and reopen Settings to apply. Disabling the module in LSPosed and rebooting
  also removes every hook.
- Inventory retains previously seen entries across scans/ROM updates. Old entries
  may remain listed; changing their selection has no effect if the key no longer exists.

## If nothing is discovered

Confirm scope and reboot; open the main Settings homepage. If the scan timestamp
still does not appear, provide the LSPosed log lines containing `SettingsMenuHider`
and your Vector version. This release expects AOSP's
`com.android.settings.homepage.TopLevelSettings` and AndroidX Preferences.
A ROM that replaces this class needs a hook adjustment; it fails without blocking Settings.

## Validation

See VALIDATION.md for the checks performed before delivery.
