# NKC Android application

NKC is an APK-installed Android application whose **engine is fixed in the APK** while its non-executable character and UI content can refresh over HTTPS. It has no Google Play dependency and deliberately contains no APK installer, package replacement, or "install unknown apps" functionality.

## Build and install

1. Install Android SDK Platform 35 and set `ANDROID_HOME` (or configure Android Studio).
2. Build: `./gradlew assembleDebug`.
3. Install the initial APK manually: `adb install app/build/outputs/apk/debug/app-debug.apk`.

That manual installation is the only installation step. Once it is installed, Android's unknown-apps permission can be disabled; content updates are ordinary HTTPS downloads stored in the app sandbox.

## Content updates

On startup (and from **Check for updates**) the app fetches the HTTPS manifest defined by `BuildConfig.CONTENT_MANIFEST_URL`, compares `contentVersion` with its active content, downloads a newer ZIP, verifies its SHA-256 digest, validates it, and atomically activates it. A bad manifest, unavailable server, interrupted download, incompatible `minimumAppVersion`, bad digest, invalid ZIP, or forbidden executable extension leaves the active content unchanged.

The manifest endpoint is configured in `app/build.gradle.kts` using the `CONTENT_MANIFEST_URL` build config field. This is deliberately injected into `ContentUpdater`; its update logic has no server-specific URL.

Example manifest:

```json
{"contentVersion":17,"minimumAppVersion":1,"contentUrl":"https://example.com/nkc/content-17.zip","sha256":"<64 lowercase-or-uppercase hex characters>"}
```

The ZIP must contain `content/config.json`; it may also contain `persona.json`, `prompts.json`, `responses.json`, `emotions.json`, and `assets/`. Packages must not contain DEX, JAR, native libraries, APKs, or other executable code. UI code accesses active data only through `ContentRepository`, keeping future persona, prompt, emotion, TTS, UI, asset, LLM endpoint, and model-selection configuration remote-updateable without making it executable.

## Publishing and rollback

To publish, create a new ZIP with the required `content/` root, calculate its SHA-256, host both ZIP and manifest on HTTPS, then publish a manifest with a higher `contentVersion`. To roll back, publish a previously known-good package using a **new higher** content version (for example, republish version 16's files as version 18). The app retains `last-known-good` files locally if activation fails.

## Versions and APK updates

`versionCode` is the APK/application version; `contentVersion` is independent. A package requiring a higher `minimumAppVersion` is deferred, not installed. Future APK update support can be designed separately, but this project intentionally never downloads, executes, installs, or replaces application binaries.
