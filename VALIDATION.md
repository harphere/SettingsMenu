# Validation for 1.0.0

- All three Java source files compile without errors or warnings using Eclipse ECJ
  3.37.0 (Java 17), Android 15 API classes and the real Xposed API 82.
- Manifest and resources parse as XML; workflow parses as YAML.
- Checked module entrypoint, recommended scope and APK artifact path.
- Reviewed homepage-only registration, inherited lifecycle hooks, dynamic additions,
  UID-gated provider, private preferences, UI-thread mutations and visibility restoration.

A complete Gradle APK build and Android lint were not run locally: the environment
has no Android SDK or Gradle installation. GitHub Actions performs both.
No phone/emulator runtime test has been performed. Vector homepage class compatibility,
provider access and Digital Wellbeing discovery must be confirmed on the device.
