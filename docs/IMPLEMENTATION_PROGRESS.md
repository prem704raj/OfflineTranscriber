# OfflineTranscriber: Implementation Progress & Roadmap Tracker

**Repository:** OfflineTranscriber (Android)  
**Initial Audit Date:** 24 October 2026  
**Status:** Local hardening branch; CI and physical-device verification pending — NOT a shippable release candidate

> Verification scope: the entries below describe implemented code. The authoritative release status and evidence ledger is `AUDIT_FINDINGS.md` at the repository root. CI has not run the hardened branch, and no physical-device, Play Console, or release-signing verification has been completed.

---

## 1. Phase Implementation Overview

| Phase | Description | Status | Target Date / Milestone |
|---|---|---|---|
| **Phase 1** | Critical Reliability & Data-Safety Fixes | Implemented; local verification only | Device and fault-injection verification pending |
| **Phase 2** | Transcription Performance, Memory Optimization & Indic Languages | Implemented; local verification only | Accuracy/latency benchmarks pending |
| **Phase 3** | Missing Productivity & Transcript-Management Features | Implemented; local verification only | Device/UI verification pending |
| **Phase 4** | UX Polish, Monetization, Quick Settings & Release Readiness | Implemented; local verification only | Play Console and release-signing verification pending |

---

## 2. Phase 1: Critical Reliability & Data-Safety Fixes

### A. Audio Recording Safety
- [x] **P0-1: Immediate FGS Promotion before Hardware Access**
  - Problem: `MediaRecorder.prepare()` / `start()` were called prior to `ServiceCompat.startForeground()` in `RecordingForegroundService.kt` (violating Android 14+ mic FGS security requirements).
  - Fix: Promoted service to foreground with `FOREGROUND_SERVICE_TYPE_MICROPHONE` before `MediaRecorder.prepare()`; wrapped hardware errors cleanly without tearing down notification abruptly.
- [x] **P0-2: Audio Recording Durable Recovery & Journaling**
  - Problem: Unexpected process death leaves corrupt unfinalized M4A without `moov` atom; session state was purely in-memory.
  - Fix: Created `RecordingDraftJournal.kt` with atomic draft persistence (`active_recording_draft.json`), heartbeat updates every second, recoverable draft inspection, and 1-tap recovery UI on `RecordScreen.kt`.
- [x] **P0-3: Storage Headroom & Error Recovery**
  - Problem: On recording error, `handleError()` unconditionally called `outputFile?.delete()`, erasing recorded audio.
  - Fix: Storage check (< 15 MB) halts safely before disk fills; `handleError()` preserves audio file if bytes were written and writes an interrupted draft; surfaces "Transcribe Preserved Audio" button on error state.

### B. Transcription Pipeline & Queue Idempotency
- [x] **P1-1: Idempotent Queue Job Finalization**
  - Problem: Database save and queue completion were split; if killed between save and complete, recovery re-ran the job and duplicated transcript records.
  - Fix: Placed `database.transcriptionJobDao().complete(request.jobId, newId)` inside the atomic `database.withTransaction` block alongside `transcriptDao.insertTranscript(entity)` in `AppTranscriptionExecutionGateway.kt`. Added skip in `TranscriptionQueueManager.kt` if job is already marked `COMPLETED` or has `resultTranscriptId != null`.
- [x] **P1-2: Chunk Checkpoint Robustness**
  - Problem: Checkpoint JSON serialized all segments repeatedly; queue job needed verification against redundant processing.
  - Fix: Checkpoints cleared atomically upon transcript commit in Room transaction.

### C. Model Management
- [x] **P0-4: SHA-256 Verified Installation & Model Repair Center**
  - Problem: `isInstalled()` only checked file size. Corrupted models blocked downloads and crashed inference.
  - Fix: Implemented `isModelReady(spec)` with SHA-256 verification and cache validation in `ModelManager.kt`. In `download()`, corrupt existing files are detected, deleted, and re-downloaded clean. Added `repair(spec)` to explicitly quarantine and re-download corrupt models. Updated `selectedOrBestInstalled()` to select SHA-verified models only. Bridged legacy and modern model managers.

### D. Compatibility & Diagnostics
- [x] **P1-3: FGS Timeout & Service Lifecycle Audit**
  - Problem: Android 14/15/16 6-hour daily budget for dataSync/mediaProcessing services; need verified timeout handlers.
  - Fix: Audited all 5 foreground services and implemented `onTimeout(startId: Int, fgsType: Int)` across:
    - `TranscriptionForegroundService.kt`
    - `RecordingForegroundService.kt`
    - `SpeakerDiarizationForegroundService.kt`
    - `CaptionExportForegroundService.kt`
    - `BackupRestoreForegroundService.kt`

---

## 3. Phase 2: Performance, Low-RAM & Indian Languages

- [x] **P1-4: Punjabi (`pa`) & Expanded Indic Language Support**
  - Added Punjabi (`pa`), Gujarati (`gu`), Kannada (`kn`), Malayalam (`ml`), Nepali (`ne`), Sanskrit (`sa`), Assamese (`as`), Sindhi (`sd`), and Sinhala (`si`) to `LanguageCatalog.kt`.
  - Added `LanguageCatalog.isIndic(code)` helper.
  - Updated `SettingsScreen.kt` language selection dialog to use scrollable Column (`verticalScroll(rememberScrollState())`) supporting all 29 languages seamlessly.
- [x] **P1-5: Hindi/Hinglish Context Prompting & Bilingual Code-Switching**
  - Added JNI native method `fullTranscribeWithPrompt` in `whisper_jni.cpp` and `WhisperNative.kt` configuring `params.initial_prompt` with `params.carry_initial_prompt = true`.
  - Implemented `WhisperEngine.buildEffectivePrompt(language, customPrompt)` supplying bilingual Hindi/Indic tokens alongside English loanwords to eliminate hallucinations during code-switched Hinglish lectures/meetings.
- [x] **P1-6: Custom Vocabulary & Terminology Engine & UI**
  - Added `customVocabulary` string key to DataStore `AppSettings.kt` and `AppSettingsRepository.kt`.
  - Wired custom vocabulary through `TranscriptionQueueManager.kt` into `ExecutionRequest.initialPrompt` and `AppTranscriptionExecutionGateway.kt`.
  - Added user-facing "Custom vocabulary & jargon" setting card and dialog in `SettingsScreen.kt`.
- [x] **P1-7: Low-RAM Memory Tuning & Thread Optimization**
  - Bound native thread count in `WhisperEngine.kt` to `Runtime.getRuntime().availableProcessors().coerceIn(2, 4)` to eliminate CPU thermal throttling and thread contention on 4–6 GB mid-range devices.
  - Verified `DeviceModelAdvisor.kt` hardware heuristic recommending Balanced (`base-q5`, 57 MB) for 4–6 GB RAM and Fast (`tiny-q5`, 31 MB) for low-RAM devices (< 4 GB).

---

## 4. Phase 3: Productivity & Transcript Management

- [x] **P2-1: Find and Replace in Transcript with FTS4 Auto-Sync**
  - Implemented `TranscriptRepository.searchAndReplace(transcriptId, findText, replaceText, matchCase)` updating `transcript_segments` rows, serialized `transcripts.segmentsJson`, and `transcripts.fullText`.
  - Automatically synchronizes SQLite FTS4 table `transcript_segments_fts`.
  - Added Search & Replace dialog in `TranscriptDetailScreen.kt` with live occurrence count and case-matching toggle.
- [x] **P2-2: Delete Audio Recording (Keep Transcript Text & Search)**
  - Implemented `deleteAudioRetainingTranscript(transcriptId)` in `TranscriptRepository.kt` and `clearAudioUri(id)` in `TranscriptDao.kt`.
  - Safely deletes heavy physical audio files from disk while preserving all text, timestamps, bookmarks, notes, and search index.
  - Exposed via 3-dot overflow menu and confirmation dialog in `TranscriptDetailScreen.kt`.
- [x] **P2-3: Inline Speaker Renaming in Transcript View**
  - Integrated `RenameSpeakerDialog` directly into `TranscriptDetailScreen.kt` speaker filter chips (selected chip shows edit action).
  - Renaming updates `speaker_clusters.customName` in Room, immediately updating all assigned segments via reactive Flow.

---

## 5. Phase 4: UX Polish, Monetization & Release

- [x] **P3-1: One-Screen First-Use Onboarding & Device Model Advisor**
  - Verified `OnboardingScreen.kt` dynamically picks the optimal model using `DeviceModelAdvisor` (Balanced for 4–6 GB RAM).
- [x] **P3-2: Android Quick Settings Recording Tile (`QuickRecordTileService`)**
  - Created `QuickRecordTileService.kt` allowing users to trigger recordings directly from the Android notification shade.
  - Registered in `AndroidManifest.xml` with `BIND_QUICK_SETTINGS_TILE` permission.
  - Handles Android 14+ `PendingIntent` requirements and Android 7.0–13 backward compatibility.
- [x] **P3-3: 16 KB Memory Page-Size Alignment**
  - Configured `target_link_options` in `CMakeLists.txt` with `-Wl,-z,max-page-size=16384` and `-Wl,-z,common-page-size=16384` for Android 15/16 and Google Play 2026 requirements.
- [x] **P3-4: App Shortcuts**
  - Verified `shortcuts.xml` and `ShortcutCommandRouter.kt` actions (`RECORD`, `IMPORT_AUDIO`, `OPEN_QUEUE`). Added unit test coverage in `ShortcutActionsTest.kt`.

---

## 6. Verification & Test Log

- **Environment & Build Tuning:** JDK 17 (`Eclipse Adoptium jdk-17.0.20.101-hotspot`), Android SDK API 26–36, Gradle 9.5.0, CMake 3.22.1 with C++17. Tuned JVM args in `gradle.properties`: `-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:CICompilerCount=2`.
- **Unit Tests (`./gradlew testDebugUnitTest` under JDK 17, with `--rerun-tasks` for the full audit run):**
  - **Result: BUILD SUCCESSFUL.**
  - **Total Tests: 204 passed across 68 test suites, 0 failed, 0 errors, 0 skipped.**
  - Full-suite rerun artifacts: `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
  - Verified test suites:
    - `IndicTranscriptionQualityBenchmarkTest` (5 tests: Levenshtein Word Error Rate (WER) & Character Error Rate (CER), bilingual Hinglish context prompt WER improvement, RTF bounds validation (< 1.0x), Indic 224-token bound enforcement)
    - `LegacyM4aSafetyTest` (2 tests: non-WAV container protection verifying `repairWavHeader` refuses to modify M4A files and preserves `ftyp` box byte-for-byte, recoverable draft recovery for legacy M4A)
    - `ModelManagerVerificationTest` (3 tests: model file installation bounds, SHA-256 verification and corruption rejection, cache key invalidation)
    - `CrashSafeAudioRecorderTest` (3 tests: write failure error state & callback triggering, `stop()` failure reporting on write errors, in-place header sealing)
    - `CrashSafeWavRecordingTest` (7 tests: canonical 44-byte WAV header generation, abrupt crash recovery with in-place header repair from physical PCM bytes, non-RIFF files rejected without synthetic rewriting, non-canonical format fields rejected, overclaimed data-chunk sizes rejected, corrupted/empty draft filtering, true duration calculation from physical PCM length)
    - `TranscriptionQueueRetryTest` (2 tests: normalization of jobs with `resultTranscriptId != null` to break infinite queue drain loop, retry clearing `resultTranscriptId = NULL`)
    - `TranscriptSearchReplaceTest` (5 tests: restored transcript segment ID remapping & healing, regex replacement safety with `$` and `\` via `Matcher.quoteReplacement`, live occurrence counting, `DeleteAudioResult` physical deletion vs DB unlinking differentiation)
    - `WhisperEnginePromptTest` (8 tests: Hinglish bilingual markers, Punjabi Gurmukhi hints, custom vocabulary injection, word-boundary prompt truncation enforcing both `MAX_PROMPT_CHARS_INDIC = 400` and `MAX_PROMPT_TOKENS = 224`)
    - `LanguageCatalogTest` (4 tests: Punjabi, Gujarati, Kannada, Malayalam, Indic detection, fallback)
    - `RecordingDraftJournalTest` (9 tests: draft persistence, heartbeat, audio preservation on abort, atomic replacement, ownership/path validation, M4A `ftyp` validation, malformed and missing-`ftyp` rejection)
    - `ShortcutActionsTest` (verification against actual `shortcuts.xml` resource bindings)
- **Android Lint (`./gradlew lintDebug lintRelease`):**
  - **Result: BUILD SUCCESSFUL; 0 errors and 77 warnings in each report.**
  - `checkReleaseBuilds = true` is enabled in `app/build.gradle.kts`, and CI runs both `lintDebug` and `lintRelease`.
  - Resolved `MissingPermission` on foreground service notifications, `ContextCastToActivity` in `PaywallScreen`, `LocalContextGetResourceValueCall` in `AboutScreen` and `SettingsScreen`.
- **Native C++ & Dex Packaging (`./gradlew assembleDebug assembleDebugAndroidTest`):**
  - **Result: BUILD SUCCESSFUL.**
  - Native library `libwhisper_jni.so` compiled and linked cleanly across `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
  - 16 KB alignment evidence: 23 of 24 packaged `.so` files have `0x4000` `PT_LOAD` alignment. The remaining `armeabi-v7a/libomp.so` has `0x1000`; 32-bit ABIs are not required to meet the 16 KB rule, but runtime 16 KB behavior remains device-verification-only.
  - Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (size recorded in `AUDIT_FINDINGS.md`); instrumentation APK is generated by `assembleDebugAndroidTest`.
  - `zipalign -c -P 16 -v 4 app-debug.apk` passes.
- **Not yet verified:** GitHub Actions on the hardened branch, physical-device fault injection and 16 KB runtime behavior, release signing/R8, and Play Console declarations/data safety.

