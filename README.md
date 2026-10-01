# Offline Transcriber

Private, on-device audio and video transcription for Android.

Offline Transcriber turns recordings, lectures, meetings, interviews, and videos into searchable transcripts while keeping speech processing on the device after the required model is available.

## Features

- Import audio files and transcribe them on-device
- Record audio directly in the app
- Import audio or video through Android share intents
- Process transcription jobs in a foreground service
- Search and organize saved transcripts
- Synchronized transcript and media playback
- Timestamped transcript navigation
- Subtitle-oriented video workflows
- Background transcription queue
- Local transcript storage with Room
- Optional on-device study/GenAI features
- Backup/restore and privacy controls
- Google Play Billing integration for premium functionality

## Privacy

The application is designed around local processing. Android cloud backup is disabled for the app, cleartext network traffic is disabled, and transcription work is handled on-device by the native speech stack after models are available.

The repository also includes:

- `PRIVACY_POLICY_DRAFT.md`
- `DATA_SAFETY_ANSWERS.md`
- `PLAY_STORE_RELEASE_CHECKLIST.md`
- `OPEN_SOURCE_NOTICES.md`
- `TESTING_CHECKLIST.md`

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin + C/C++ |
| UI | Jetpack Compose + Material 3 |
| Local database | Room |
| Media | AndroidX Media3 |
| Speech / native inference | Native C++ speech stack + sherpa-onnx |
| On-device GenAI | Google ML Kit GenAI Prompt API |
| Preferences | DataStore |
| Billing | Google Play Billing |
| Build | Gradle Kotlin DSL + CMake |
| Java | 17 |
| Minimum Android | API 26 |
| Target / Compile SDK | API 36 |

## Project Structure

```text
OfflineTranscriber/
├── app/
│   ├── src/main/
│   │   ├── java/          # Android application code
│   │   ├── cpp/           # Native speech/inference sources
│   │   ├── assets/        # Bundled sample media/assets
│   │   └── AndroidManifest.xml
│   ├── libs/              # Native/runtime Android libraries
│   └── build.gradle.kts
├── .github/workflows/     # Android CI
├── DATA_SAFETY_ANSWERS.md
├── OPEN_SOURCE_NOTICES.md
├── PLAY_STORE_RELEASE_CHECKLIST.md
├── PRIVACY_POLICY_DRAFT.md
├── STORE_LISTING_DRAFT.md
└── TESTING_CHECKLIST.md
```

## Build

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Android NDK 27.1.12297006
- CMake 3.22.1

### Debug build

```bash
./gradlew assembleDebug
```

### Unit tests

```bash
./gradlew testDebugUnitTest
```

The GitHub Actions workflow runs unit tests and assembles a debug APK on pushes to `main` and release branches.

## Release Signing

Release signing is configured through environment variables:

```text
OT_UPLOAD_STORE_FILE
OT_UPLOAD_STORE_PASSWORD
OT_UPLOAD_KEY_ALIAS
OT_UPLOAD_KEY_PASSWORD
```

A release build fails if those values are not configured, preventing an accidental fallback to debug signing.

## Repository

https://github.com/prem704raj/OfflineTranscriber