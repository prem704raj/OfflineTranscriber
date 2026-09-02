# Google Play Data Safety Final Worksheet

This document provides accurate declarations for Google Play Console Data Safety questionnaire based on an audit of the codebase, dependencies, and manifest.

---

## 1. Overview & Data Collection Summary

- **Does the app collect or share any user data?** Yes (via Google Play Billing platform service and optional diagnostic logs when user chooses to share).
- **Is all user data encrypted in transit?** Yes (all network connections use HTTPS).
- **Does the app provide a way for users to request data deletion?** Yes (built-in full data deletion controls in Settings > Privacy & data controls).

---

## 2. Specific Data Categories

### Financial Info
- **Purchase history:** Handled directly by Google Play Billing library.
- **Collection:** Yes (by Google Play for billing purposes).
- **Sharing:** No (developer does not store payment cards or share purchase history).
- **Purpose:** App functionality (entitlement verification), Fraud prevention.
- **Is it optional?** Only relevant if purchasing Offline Transcriber Pro.

### Audio files / Voice recordings
- **Collection:** No (all audio and microphone recordings are processed purely on-device via native whisper.cpp; never transmitted to developer servers).
- **Sharing:** No (only shared if user taps Android Share).

### Files & docs (Transcripts, SRT, VTT)
- **Collection:** No (stored in local SQLite Room database).
- **Sharing:** No (only shared if user explicitly exports).

### App info & performance (Diagnostics)
- **Crash logs / Diagnostics:** Collected locally in on-device database without PII or transcript text.
- **Collection:** No automatic transmission. Export occurs only if user taps "Share Diagnostics".
- **Purpose:** Diagnostics / Troubleshooting.

---

## 3. Play Console Submission Checklist
- [x] No third-party ad networks declared.
- [x] No analytics SDKs declared.
- [x] Google Play Billing declared as functional payment mechanism.
- [x] On-device speech processing accurately distinguished from cloud processing.
