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
5. Configure `CLERK_AUTHORIZED_PARTIES` to the trusted app origins used by the deployment.

The Android SDK integration follows the official [Clerk Android quickstart](https://clerk.com/docs/android/getting-started/quickstart). SDK version pinned to 1.4.1.

## Local Android build

Open `android/` in Android Studio and build the `app` debug variant, or run `gradle -p android :app:assembleDebug` with Gradle 8.13+ installed. Add the publishable key to local `gradle.properties` first. A placeholder is used only to allow CI compilation without a real credential.

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

The requested 20 app / 25 executor languages will be introduced as supported toolchains, SDKs, adapters and test fixtures where useful. The first execution image prioritizes Bash, Python, Node.js, Go, Rust, C/C++ and Java. No unused languages will be added just to inflate a count.
