# Validation for 1.0.1

- The original 1.0.0 Java source files compiled without errors or warnings using Eclipse ECJ
  3.37.0 (Java 17), Android 15 API classes and the real Xposed API 82.
- Manifest and resources parse as XML; workflow parses as YAML.
- Checked module entrypoint, recommended scope and APK artifact path.
- Reviewed homepage-only registration, inherited lifecycle hooks, dynamic additions,
  UID-gated provider, private preferences, UI-thread mutations and visibility restoration.

A complete Gradle APK build and Android lint were not run locally: the environment
has no Android SDK or Gradle installation. GitHub Actions performs both.
No phone/emulator runtime test has been performed. Vector homepage class compatibility,
provider access and Digital Wellbeing discovery must be confirmed on the device.

## 1.0.1 correction

Replaced both numeric layout orientations with LinearLayout.VERTICAL to address
the two WrongConstant lint errors reported by GitHub Actions. Lint remains enabled.
Full Android lint is still run by the GitHub Actions workflow, not locally.

For 1.0.1, both orientation call sites and ZIP integrity were checked. The previous
compiler dependencies are no longer available locally, so compilation was not rerun.
