# Teamwork Project Prompt — Draft

> Status: Step 1 — Eliciting project idea / Audit check
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: [none — teamwork routes from the description]

Comprehensive production-hardening, static/dynamic audit verification, and Google Play release gating for the OfflineTranscriber Android application.

Working directory: P:\Projects\OfflineTranscriber

## Requirements

### R1. Complete Android Build and Verification Pipeline
Verify that all Gradle targets (`testDebugUnitTest`, `lintDebug`, `lintRelease`, `assembleDebug`, `assembleDebugAndroidTest`) build cleanly with zero errors, verifying 16 KB ELF PT_LOAD alignment across native `.so` libraries and zip alignment.

### R2. Core Engine and Data-Safety Integrity
Validate audio recording durability (crash-safe WAV header repair, atomic draft journal, bounds-checked RIFF sizing), model integrity (SHA-256 verification before loading/inference), Room database transaction atomicity, and cancellation propagation.

### R3. Google Play Release & Policy Verification Gates
Audit foreground services (Android 14+ microphone promotion before hardware access, timeout callbacks), permissions, privacy disclosures (on-device only, zero cloud transmission), and signed release artifact generation.

## Acceptance Criteria

### Verification Gates
- [ ] 100% passing unit tests without regressions or swallowed errors
- [ ] `lintDebug` and `lintRelease` succeed with 0 errors
- [ ] `assembleDebug` and `assembleDebugAndroidTest` produce valid APKs
- [ ] All 64-bit native libraries (`arm64-v8a`, `x86_64`) verify at 16 KB ELF alignment (`0x4000`)
- [ ] No unhandled P0/P1 reliability defects remain in the local codebase
- [ ] Clear documentation of real-device tests (`VERIFY_DEVICE`) and Play Console checks (`VERIFY_PLAY_CONSOLE`)

---
*Next: when approved → delegate via invoke_subagent (see Delegation Protocol)*

