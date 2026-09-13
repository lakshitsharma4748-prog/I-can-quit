# Building a release APK/AAB

Everything here needs a real Android SDK/toolchain — none of it could be
run in the sandbox this project was scaffolded in (see README's "Note on
this development environment"). This is written for whoever picks this up
on a normal development machine.

## 1. Generate a release keystore (one-time, if you don't have one)

```bash
keytool -genkeypair -v \
  -keystore safeshield-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias safeshield
```

Keep this file and its passwords somewhere safe outside the repository —
losing it means you can never publish an update to an app that used it.
**Never commit it.**

## 2. Configure signing

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` with the real path to your `.jks` file and its
passwords. Both `keystore.properties` and any `.jks`/`.keystore` file are
already in `.gitignore` — double-check `git status` shows nothing new
before committing anything after this step.

`app/build.gradle.kts` picks this up automatically: if
`keystore.properties` exists at the repo root, the `release` build type is
signed with it; if it doesn't, `release` still builds (useful for testing
the release build type's behavior) but produces an unsigned artifact you
can't install on a device or upload to Play until it's signed.

## 3. Build

```bash
./gradlew bundleRelease   # .aab, for Play Store upload — app/build/outputs/bundle/release/
./gradlew assembleRelease # .apk, for direct install/testing — app/build/outputs/apk/release/
```

## 4. Before actually shipping a release build

These are things that couldn't be verified in the sandbox this project
was built in and should be checked on a real build before publishing:

- **Confirm the release build actually installs and runs.** This project
  has only ever been built and reviewed by hand (see TESTING.md) —
  `./gradlew build` itself has never completed in the environment it was
  written in.
- **`BLOCKLIST_API_BASE_URL`** in `app/build.gradle.kts` already points at
  the live Railway deployment
  (`https://backend-production-eacd8.up.railway.app` — project
  "safeshield-backend", see `backend/README.md`). If you redeploy to a
  different URL/project, update it here.
- **Consider enabling `isMinifyEnabled = true`** for a smaller release
  APK — left off for now specifically because it hasn't been verified
  against a real build here. If you turn it on, rebuild and re-test
  thoroughly (Room, WorkManager, and kotlinx.serialization all ship
  their own consumer ProGuard rules, so this is expected to be low-risk,
  but "expected" isn't "verified").
- **Bump `versionCode`/`versionName`** in `app/build.gradle.kts` for each
  release you publish.
- Read [DEVICE_OWNER_PROVISIONING.md](DEVICE_OWNER_PROVISIONING.md) if
  you intend to distribute this for Strong Protection/Device Owner use —
  it explains what a real deployment (QR/NFC enrollment, an MDM console)
  looks like beyond the `adb` commands used for development.
