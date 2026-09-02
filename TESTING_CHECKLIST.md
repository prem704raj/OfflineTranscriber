# Offline Transcriber — Release Candidate Testing & QA Checklist

This checklist must be executed prior to each production release to Google Play.

---

## 1. Core Transcription & Models
- [ ] **Sample Test (JFK)**: Run built-in sample test; verify transcript text, progress, and segment timestamps.
- [ ] **Audio File Import**: Import `.mp3`, `.m4a`, `.wav`, `.ogg` from device storage; verify complete transcription.
- [ ] **Microphone Recording**: Record a 2-minute live audio note; verify background recording service notification and automatic transcription enqueueing.
- [ ] **Model Switching**: Download and activate Fast (`tiny-q5`), Balanced (`base-q5`), and Accurate (`small-q5`).
- [ ] **Language Selection**: Transcribe non-English speech with specific language code selected (e.g., Spanish, German, Hindi).
- [ ] **Airplane Mode**: Transcribe in airplane mode; verify zero internet connection is required after model download.

## 2. Playback, Subtitles & Study Intelligence
- [ ] **Synced Playback**: Play back audio in Transcript Detail screen; verify highlighting scrolls synchronously with audio.
- [ ] **Segment Bookmark**: Add, toggle, and view bookmarked segments.
- [ ] **Collections**: Create collections, add transcripts, and view filtered collection details.
- [ ] **Video Subtitle Studio (Pro)**: Import video (`.mp4`), edit subtitle text, view video preview with overlays.
- [ ] **SRT / VTT Export (Pro)**: Export subtitles and share to another application.
- [ ] **Study Intelligence (Pro)**: Generate Flashcards, Summary, Quiz, and Key Points; test flashcard flip and quiz answer evaluation.

## 3. Persistent Queue & Process Death Recovery
- [ ] **Batch Enqueue**: Enqueue 3 files simultaneously; verify sequential FIFO execution.
- [ ] **Process Death Simulation**: Kill app during active transcription via Android Studio / ADB; reopen app and verify job automatically resumes or recovers.

## 4. Monetization & Billing (Google Play)
- [ ] **Paywall Entry Points**: Verify Paywall triggers on Video Import, Accurate Model, SRT Export, Study Mode Generation, 4th Collection, and 4th Unfinished Queue Job on Free tier.
- [ ] **Google Play Purchase**: Test purchase flow using Google Play License Testing account.
- [ ] **Purchase Restoration**: Reinstall app or tap "Restore purchase" in Settings; verify Lifetime Pro unlocks without re-purchasing.

## 5. Privacy, Data Safety & Diagnostics
- [ ] **Clean Temporary Files**: Run temporary file cleanup in Settings > Privacy & Data; verify storage decreases without deleting transcripts or models.
- [ ] **Delete Transcription Content**: Run content deletion; verify DB rows and recordings are wiped while models and Pro entitlement remain.
- [ ] **Delete Downloaded Models**: Verify model files are removed and storage is reclaimed.
- [ ] **Delete All Local App Data**: Type `DELETE` to confirm; verify complete reset.
- [ ] **Diagnostics Report**: Open Diagnostics; verify specs display correctly and "Share diagnostics" creates a sanitized `.txt` report containing zero transcript text or purchase tokens.
- [ ] **Missing Source Resilience**: Delete source audio or video file from device; open transcript detail and verify app displays "Source unavailable" banner without crashing.

## 6. Build & Release Hardening
- [ ] **R8 Minification**: Verify release build compiles cleanly with `isMinifyEnabled = true`.
- [ ] **JNI Integrity**: Verify whisper.cpp native C++ library loads and functions under ProGuard obfuscation.
- [ ] **Cloud Backup Exclusion**: Verify transcripts and DB files are excluded from Android cloud backups.
- [ ] **Zero Sensitive Logs**: Verify Logcat in release variant emits zero transcript texts, prompts, or tokens.
