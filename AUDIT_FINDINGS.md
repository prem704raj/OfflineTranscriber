# OfflineTranscriber — Production Hardening Audit (Updated)

**Audit date:** 2026-10-25  
**Audited baseline:** `main` @ `8624d072e5099a8f26bc2b9e637d61595dd6801a`  
**Hardening branch:** `hardening/production-audit` (uncommitted working tree)  
**Original evidence package:** `OfflineTranscriber_Hardening_2026-10-25/`  
**Release status:** **NOT READY FOR PRODUCTION**

## 1. Executive summary

The original audit found CI broken by the deprecated Android SDK `tools` package, unsafe recording journal replacement/deletion, shallow WAV validation, unchecked WAV size narrowing, shared recorder I/O races, size-only legacy model readiness, suppressed release lint, and unsupported release claims.

This pass implemented local fixes for the actionable findings and verified them with the CI-equivalent Gradle tasks under JDK 17:

- Unit tests: **204 passed / 68 suites / 0 failed / 0 skipped**.
- `lintDebug`: **0 errors, 77 warnings**.
- `lintRelease`: **0 errors, 77 warnings**; release lint is now enabled.
- `assembleDebug` and `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**.
- Native debug build for `arm64-v8a`, `armeabi-v7a`, `x86_64`: **BUILD SUCCESSFUL**.
- APK shared-library ZIP alignment: `zipalign -c -P 16` **passes**.
- ELF PT_LOAD audit: all 64-bit libraries use `0x4000` alignment. The 32-bit `armeabi-v7a/libomp.so` remains `0x1000` aligned.

The fixes have **not** been pushed or run in GitHub Actions, and no physical Android device or Play Console verification has been performed. The branch is not a release candidate.

## 2. Verification evidence

Environment: Windows, JDK 17.0.20.1 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`), Gradle 9.5.0, Android SDK 36, build-tools 36.0.0, NDK 27.1.12297006, CMake 3.22.1.

| Command / check | Result |
|---|---|
| `gradlew testDebugUnitTest --rerun-tasks` | PASS — 204 tests, 68 suites, 0 failures, 0 errors, 0 skipped |
| `gradlew lintDebug` | PASS — 0 errors, 77 warnings |
| `gradlew lintRelease` | PASS — 0 errors, 77 warnings |
| `gradlew assembleDebug` | PASS — `app-debug.apk` (134,855,215 bytes) |
| `gradlew assembleDebugAndroidTest` | PASS — `app-debug-androidTest.apk` (998,850 bytes) |
| `zipalign -c -P 16 -v 4 app-debug.apk` | PASS — `Verification successful` |
| ELF PT_LOAD alignment (`llvm-readelf -lW`) | 23/24 libraries at `0x4000`; `armeabi-v7a/libomp.so` at `0x1000` |
| `adb devices -l` | No devices attached |
| GitHub Actions | NOT RUN on hardened working tree |

## 3. Findings ledger

### OT-001 — CI setup used removed SDK package
- **Priority:** P0
- **Status:** FIXED LOCALLY; CI NOT RUN
- **File:** `.github/workflows/android.yml`
- **Change:** `android-actions/setup-android@v4`, explicit installation of `platforms;android-36`, `build-tools;36.0.0`, `ndk;27.1.12297006`, `cmake;3.22.1`, plus debug/release lint and instrumentation APK builds.
- **Additional fix:** removed the Windows-only `org.gradle.java.home` from `gradle.properties`.
- **Evidence:** equivalent build/test/lint commands pass locally under JDK 17.
- **Remaining:** push and observe an actual GitHub Actions run.

### OT-002 — Unverified test claims
- **Priority:** P1
- **Status:** RESOLVED LOCALLY; CI NOT RUN
- **Evidence:** 204 tests across 68 suites, 0 failures/errors/skips, from `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
- **Remaining:** CI must reproduce the result; counts are branch-local, not a `main` verification.

### OT-003 — Non-atomic recording journal replacement
- **Priority:** P1
- **Status:** FIXED; CRASH-INJECTION VERIFICATION PENDING
- **File:** `.../recorder/RecordingDraftJournal.kt`, `writeAtomic()`
- **Change:** fsync temporary JSON, then `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`; fallback replacement without deleting the previous journal first.
- **Tests:** journal persistence/heartbeat/path-trust suite passes.
- **Remaining:** process-kill fault injection and parent-directory durability on real filesystems.

### OT-004 — Journal path trust in delete/recovery
- **Priority:** P1
- **Status:** FIXED
- **File:** `.../recorder/RecordingDraftJournal.kt`, `discardOwned()`, `findRecoverableDraft()`
- **Change:** canonical-path ownership checks restricted to app files and app-specific external recordings, `.wav`/`.m4a` suffix only; untrusted paths are ignored.
- **Tests:** `discardDoesNotDeleteOutsideAppRoot`, `recoveryDoesNotRepairUntrustedWavPath` pass.

### OT-005 — Recorder writer/flush/stop concurrency
- **Priority:** P1
- **Status:** HARDENED; VERIFY_DEVICE
- **Files:** `.../recorder/CrashSafeAudioRecorder.kt`, `RecordingForegroundService.kt`
- **Change:** all writes, flushes, header repairs, and stream close paths use `ioLock`; recorder checks the stopped flag after `AudioRecord.read`; stop unblocks capture before joining; write-error state resets on each start; maximum-size capture stops gracefully.
- **Tests:** `CrashSafeAudioRecorderTest`, `CrashSafeWavRecordingTest` pass.
- **Remaining:** pause/stop/mic-unplug/slow-disk races require device testing.

### OT-006 — Shallow WAV validation
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **File:** `.../recorder/WavAudioWriter.kt`, `isValidWav()`
- **Change:** validates RIFF/WAVE/fmt/data identifiers, canonical PCM fmt size, mono/stereo channel bounds, sample rate, bits per sample, block alignment, byte rate, data size vs physical payload, sample alignment, and RIFF bounds.
- **Tests:** crash-recovery WAV tests pass; M4A files are rejected.
- **Remaining:** independent decoder/`ffprobe` corpus and device playback.

### OT-007 — WAV 32-bit size overflow
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **File:** `.../recorder/WavAudioWriter.kt`, `createWavHeader()`, `repairWavHeader()`, `CrashSafeAudioRecorder.kt`
- **Change:** explicit RIFF upper bound (`MAX_WAV_AUDIO_BYTES`), repair refuses oversized files instead of wrapping, recorder stops before crossing the limit, and removed the unsafe synthetic-header rewrite of non-RIFF files.
- **Tests:** existing WAV tests pass.
- **Remaining:** no multi-GB allocation test; limit behavior still needs device/storage fault injection.

### OT-008 — Legacy M4A size-only recovery
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **File:** `.../recorder/RecordingDraftJournal.kt`, `isValidM4a()`
- **Change:** requires an `ftyp` box at the start with a plausible box size; removed the “any file ≥ 2048 bytes” fallback.
- **Tests:** valid M4A recovery passes; M4A without `ftyp` is rejected.
- **Remaining:** full media metadata/decode validation and device playback.

### OT-009 — Legacy model manager size-only readiness
- **Priority:** P1
- **Status:** FIXED LOCALLY; INSTRUMENTATION TEST PENDING
- **File:** `.../transcription/ModelManager.kt`
- **Change:** legacy and canonical paths are SHA-256 verified against a pinned map for all six legacy model files; cache keys include path/size/mtime; corrupt canonical files are not returned as ready; downloads are SHA-verified before rename; deletion removes both legacy locations.
- **Pinned SHA-256 values** (from the Hugging Face `ggerganov/whisper.cpp` repository):
  - `ggml-tiny.en.bin`: `921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f`
  - `ggml-tiny.bin`: `be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21`
  - `ggml-base.bin`: `60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe`
  - `ggml-base.en.bin`: `a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002`
  - `ggml-base-q5_1.bin`: `422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898`
  - `ggml-small.bin`: `1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b`
- **Remaining:** Android-context tests for download/delete/corrupt-file behavior.

### OT-010 — Canonical model selection used size-only readiness
- **Priority:** P1
- **Status:** FIXED (pre-existing working-tree change)
- **File:** `.../modelmanager/ModelManager.kt`
- **Change:** selection and fallback use SHA-verified readiness.
- **Tests:** `ModelManagerVerificationTest` passes.

### OT-011 — Native 16 KB compatibility
- **Priority:** P1
- **Status:** PARTIAL ARTIFACT VERIFICATION; VERIFY_DEVICE
- **Evidence:** `zipalign -c -P 16` passes. All `arm64-v8a` and `x86_64` `.so` files have `0x4000` PT_LOAD alignment. `armeabi-v7a/libomp.so` has `0x1000` alignment; `armeabi-v7a` is a 32-bit ABI and is not used on 16 KB-page 64-bit devices.
- **Remaining:** run on a 16 KB-page physical device; decide whether to keep the 32-bit ABI for Play; inspect release AAB, not only debug APK.

### OT-015 — Release lint disabled
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **Files:** `app/build.gradle.kts`, `.github/workflows/android.yml`
- **Change:** `checkReleaseBuilds = true`; CI runs `lintDebug lintRelease`.
- **Evidence:** `lintRelease` passes with 0 errors and 77 warnings.

### OT-018 — Documentation overstated verification
- **Priority:** P1
- **Status:** PARTIAL
- **Files:** `docs/IMPLEMENTATION_PROGRESS.md`
- **Change:** top-level status now says local hardening with CI/device verification pending; phase table distinguishes code implementation from release verification; local test/lint/native evidence updated.
- **Remaining:** other historical documents and release notes were not exhaustively reconciled.

### OT-019 — Queue checkpoint could resume against rebuilt/truncated PCM
- **Priority:** P1
- **Status:** FIXED LOCALLY; INSTRUMENTATION TEST PENDING
- **File:** `.../queue/AppTranscriptionExecutionGateway.kt`
- **Change:** preparation now writes to a temporary PCM and renames it into place only on success, followed by a `.ready` marker. Rebuilding a missing/empty/incomplete PCM clears the checkpoint first; checkpoint samples must fall within the current PCM and align with chunk boundaries; inconsistent checkpoints are discarded instead of producing truncated or duplicated transcripts. Completion and discard remove the PCM, temporary file, and marker.
- **Remaining:** no instrumentation test yet for process death at each preparation/finalization boundary.

### OT-020 — Whisper cancellation could be lost before inference
- **Priority:** P1
- **Status:** FIXED LOCALLY; TEST PENDING
- **Files:** `.../transcription/WhisperEngine.kt`, `.../queue/AppTranscriptionExecutionGateway.kt`
- **Change:** `transcribe()` accepts an external `shouldCancel` predicate, checked by the native abort callback and before/after inference; the gateway passes the job-specific cancellation predicate.

### OT-021 — JNI null handling
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **File:** `app/src/main/cpp/whisper_jni.cpp`
- **Change:** null audio arrays, failed `GetFloatArrayElements`, and failed `GetStringUTFChars` for model/asset/language strings now return failure instead of dereferencing null.
- **Evidence:** native debug build succeeds for all three ABIs.

### OT-022 — Legacy transcript persistence was non-atomic / swallowed cancellation
- **Priority:** P1
- **Status:** FIXED LOCALLY
- **File:** `.../data/repository/TranscriptRepository.kt`
- **Change:** transcript + segment inserts use one Room transaction; bundled-sample persistence also uses a transaction; `CancellationException` is rethrown; model-load failure is checked before sample transcription.

## 4. Release blockers remaining

- GitHub Actions has not executed the hardened branch; local results are not CI verification.
- No physical-device testing: recording pause/stop/mic loss, crash recovery, 16 KB page devices, low storage, playback/export.
- Play Console declarations, billing, data-safety, and signed AAB verification are not performed (`VERIFY_PLAY_CONSOLE`).
- Release signing and R8 release build are not verified.
- Indic accuracy and long-audio/checkpoint behavior need a consented-audio benchmark and instrumentation tests.
- The untracked audit package and `.vscode/` remain outside the commit scope.

## 5. Release decision

**NOT READY FOR PRODUCTION.** The local hardened working tree passes unit tests, debug/release lint, debug assembly, instrumentation APK assembly, native compilation, and ZIP/64-bit ELF alignment checks. CI, physical-device, release-signing, Play Console, and accuracy verification remain outstanding. No merge, push, or release is authorized by this audit.
