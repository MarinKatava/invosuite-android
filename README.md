# InvoiSuite Android

Java 21 Android client for InvoiSuite. Sign in with email/password or Google,
manage one or more companies, their clients and projects, and build invoices
that generate a PDF or Excel file on the backend.

## Stack

- Java 21, Android Gradle Plugin 8.5.2, Gradle 8.9
- `minSdk` 26 (Android 8.0), `compileSdk`/`targetSdk` 34
- Material Components, ViewBinding, RecyclerView
- Retrofit + OkHttp + Gson for the [invoisuite-server](../invoisuite-server) API
- Google Sign-In (`play-services-auth`) for the Google login option
- `androidx.security` `EncryptedSharedPreferences` for storing the JWT

## Running it

1. Start the backend first (see the server project's README) — by default the
   app talks to `http://10.0.2.2:8080/` (the Android emulator's alias for the
   host machine's `localhost`), configured in `app/build.gradle`'s
   `API_BASE_URL` build config field. Point it at a real host/IP for a
   physical device or a deployed backend.
2. Open this folder in Android Studio (or run `./gradlew assembleDebug` from
   the command line) and let it sync/download dependencies.
3. **Google Sign-In setup** (optional — email/password works out of the box):
   - Create an OAuth 2.0 **Web** client ID in Google Cloud Console.
   - Set it as `GOOGLE_CLIENT_ID` on the backend.
   - Replace `google_web_client_id` in `app/build.gradle`'s `resValue` with
     the same client ID (Android's Google Sign-In SDK needs the *server's*
     web client ID, not an Android-type client ID, to mint an ID token the
     backend can verify).
   - You'll also need to register the app's package name + SHA-1 signing
     certificate with that Google Cloud project for sign-in to succeed on
     a real build.
4. Run the app on an emulator or device (API 26+).

## Building with Docker

An Android app has no headless "server" mode — it's a UI you run on a device
or emulator, which a plain container can't provide. What Docker *can* do
reproducibly is **build** the APK, without installing Android Studio or the
SDK locally:

```bash
docker build -t invoisuite-android-build .
docker create --name invoisuite-apk-extract invoisuite-android-build
docker cp invoisuite-apk-extract:/app/app/build/outputs/apk/debug/app-debug.apk .
docker rm invoisuite-apk-extract
```

That gives you `app-debug.apk` on the host, which you install on a device or
emulator with `adb install app-debug.apk`. The image pins `linux/amd64`
(emulated on Apple Silicon) because `aapt2`, part of the Android build tools,
only ships x86_64 Linux binaries.

If you actually need the app *running* inside a container (e.g. for
automated screenshot testing), that requires a full Android emulator image
with a virtual display, such as `budtmo/docker-android` — a much heavier
setup than this build-only image, and only worth it if you have that
specific need.

## App structure

- `auth` — login/register, Google Sign-In
- `session` — encrypted local storage of the JWT + signed-in user
- `api` — Retrofit service interface and client setup (auth header injection,
  401 → auto sign-out via `common.SessionEvents`)
- `company` — list/create/edit companies, and the tabbed company workspace
  (Clients / Projects / Invoices)
- `client`, `project`, `invoice` — CRUD screens for each resource; the
  invoice form has an editable, dynamically-sized list of line items
- `model` — plain `record` DTOs mirroring the backend's JSON shapes

## Notes

- PDF/XLSX downloads are cached under the app's private cache dir and opened
  via a `FileProvider` share/view chooser, rather than written to the public
  Downloads folder — this avoids needing storage permissions or MediaStore
  plumbing while still letting the user save or share the file from whatever
  viewer they pick.
- There's no local database/offline cache: every screen fetches fresh from
  the API. Given the traffic pattern (occasional invoice generation, not a
  high-frequency app) this is a reasonable simplification.
