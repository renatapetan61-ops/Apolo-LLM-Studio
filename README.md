# Apolo LLM Studio

Android-first development environment with a real remote Linux x86-64 terminal.

> **Current stage: foundation scaffold.** The Android client, Clerk integration point, API and PTY executor are being built incrementally. A working remote host and production isolation are not yet provisioned.

## Architecture

- `android/`: native Kotlin + Jetpack Compose app.
- `services/control-plane/`: Node.js API, Clerk token verification and WebSocket terminal gateway.
- `deploy/`: x86-64 Docker Compose development deployment.
- The terminal runs Bash in a Linux x86-64 container on the remote host. It is not an in-browser shell emulator.

## Requirements

- Android Studio / JDK 17+, Android SDK 36.
- Node.js 22+ for the control plane.
- Linux x86-64 host with Docker Engine and Compose v2 for the remote executor.
- The Clerk application already selected in the Clerk Dashboard.

## Clerk setup

1. In the selected Clerk application, enable **Native API** under **Native applications**.
2. Register the Android application namespace/package: `com.apolo.llmstudio`.
3. Put the Clerk **publishable key** in local `android/gradle.properties` as `APOLO_CLERK_PUBLISHABLE_KEY=pk_test_...` (do not commit it).
4. Put the Clerk **secret key** in the server environment as `CLERK_SECRET_KEY`. Never put it in Android or source control.
5. For GitHub Actions Release builds, add the Clerk **publishable key** as the repository Actions secret `APOLO_CLERK_PUBLISHABLE_KEY`. The Android app checks the key before initializing Clerk; if it is absent, the app opens a setup screen instead of crashing. Never add the Clerk secret key to this Android secret or APK.
6. Configure `CLERK_AUTHORIZED_PARTIES` to the trusted app origins used by the deployment.

The Android SDK integration follows the official [Clerk Android quickstart](https://clerk.com/docs/android/getting-started/quickstart). SDK version pinned to 1.4.1.

## Local Android build

Open `android/` in Android Studio and build the `app` debug variant, or run `gradle -p android :app:assembleDebug` with Gradle 8.13+ installed. Add the publishable key to local `gradle.properties` first. A placeholder is used only to allow CI compilation without a real credential.

## Create a signing key and build a signed APK

Keep the upload keystore private and make at least two encrypted backups. Losing it can prevent updates from being signed with the same key. Do not send the keystore or passwords in chat or commit them to Git.

1. From a trusted computer with JDK 17+, create a keystore (choose a strong password when prompted):

```bash
keytool -genkeypair -v -keystore apolo-upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias apolo-upload
```

2. Copy `apolo-upload.jks` into a private folder. Create `android/keystore.properties` from `android/keystore.properties.example`, then set:
   - `storeFile`: absolute path to the JKS file
   - `storePassword`: keystore password
   - `keyAlias`: `apolo-upload`
   - `keyPassword`: key password

3. Confirm `android/keystore.properties` and the JKS are not staged in Git. They are excluded by `.gitignore`.

4. Build the signed release APK:

```bash
gradle -p android :app:assembleRelease
```

5. The APK will be at:

```
android/app/build/outputs/apk/release/app-release.apk
```

6. Verify its signature:

```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose --print-certs android/app/build/outputs/apk/release/app-release.apk
```

For Android Studio, use **Build > Generate Signed Bundle / APK > APK**, select the same keystore, alias and passwords, then choose the release build.

**Important:** keep using the same signing key for every update distributed outside Google Play. If publishing through Google Play, use Play App Signing and protect the upload key separately. A debug APK is not a production release.

## Start the x86-64 development executor

On a Linux x86-64 server:

```bash
bash deploy/preflight.sh
cp deploy/.env.example deploy/.env
# Edit deploy/.env locally; never commit it.
docker compose -f deploy/compose.yaml up --build -d
```

The development API listens on port 8080 bound to localhost. Do not expose it publicly until TLS, rate limits, per-user container isolation, resource quotas, persistent workspace policy and production Clerk configuration are complete.

## Important security boundary

The initial PTY worker is a development executor running inside its own container. It is **not yet a multi-tenant production sandbox**. Do not offer arbitrary public shell access until each user receives an isolated container/VM with CPU, RAM, disk, process, network and time limits. Never mount the host Docker socket into the public-facing API.

## Roadmap

1. Native Android navigation, Clerk sign-in and terminal UI.
2. Authenticated API and real PTY/WebSocket transport.
3. Per-user isolated x86-64 execution workers and persistent workspaces.
4. Projects/files, language toolchains, build jobs and resource quotas.
5. AI provider routing with server-side secrets only.
6. CI, automated tests, release signing and production deployment.

## Language strategy

The requested 20 app / 25 executor languages will be introduced as supported toolchains, SDKs, adapters and test fixtures where useful. The first execution image prioritizes Bash, Python, Node.js, Go, Rust, C/C++ and Java. No unused languages will be added just to inflate the count.
