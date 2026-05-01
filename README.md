# aitrader-android

Android client for the Janus. Part of [`janus`](https://github.com/sppidy/janus).

- Kotlin + Jetpack Compose
- Retrofit2 + Room (offline cache)
- WebSocket live log streaming
- Biometric auth (fingerprint / face)
- Configurable base URL — the same APK talks to NSE main, NSE eval, or Forex backends

## Quick start

```bash
# Set defaults via gradle.properties (or override per-build with -P flags)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Override the bundled defaults:

```bash
./gradlew assembleDebug \
  -PapiHost=your.backend.host \
  -PapiPort=8443 \
  -PapiKey=your-api-key \
  -PforexUrl=https://forex.example.com
```

## Release build (signed)

Provide a keystore and the four release signing properties:

```bash
./gradlew assembleRelease \
  -PRELEASE_STORE_FILE=/path/to/keystore.jks \
  -PRELEASE_STORE_PASSWORD=$STORE_PASS \
  -PRELEASE_KEY_ALIAS=$ALIAS \
  -PRELEASE_KEY_PASSWORD=$KEY_PASS
```

CI builds via `.github/workflows/android-build.yml`, which expects the four `RELEASE_*` GitHub Actions secrets (the keystore is base64-encoded and stored as `RELEASE_KEYSTORE_BASE64`).

## Screens

Bottom-nav: Dashboard · Portfolio (manual BUY/SELL) · Scanner · Charts · Agent chat · Logs · Settings.

## License

[Apache-2.0](LICENSE). Contributing guidelines and security policy live in the [super-repo](https://github.com/sppidy/janus).
