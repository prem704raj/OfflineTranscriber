# Google Play Data Safety Final Review

This document provides the audited answers and justifications for the Google Play Data Safety form for Offline Transcriber (v1.2, Target SDK 36).

---

## 1. Overview & Data Collection Summary

| Question | Answer | Reason / Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** (except standard Play Billing managed directly by Google Play) | All audio, video, transcripts, and notes are processed and stored exclusively on-device. No developer backend exists. |
| **Is all user data collected by your app encrypted in transit?** | **Yes** | HTTPS is strictly enforced for all network connections (model downloads and Google Play Billing). Cleartext traffic is disabled. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Built-in "Privacy & Data Controls" screen provides complete local data deletion and cache clearing. |

---

## 2. Category-by-Category Audit

### A. Location
- **Approximate Location**: Not collected.
- **Precise Location**: Not collected.

### B. Personal Info
- **Name, Email, User IDs, Address, Phone, Race/Ethnicity, Political/Religious beliefs, Sexual orientation**: Not collected. No account registration is required or supported.

### C. Financial Info
- **Credit Card, Debit Card, Bank Info**: Not collected by developer. Google Play Billing handles all in-app purchase financial transactions on Google's secure servers.
- **Purchase History**: Processed locally via Google Play Billing Client for entitlement verification. Not sent to any developer server.

### D. Health and Fitness
- **Health & Fitness Data**: Not collected.

### E. Messages & Emails
- **Emails, SMS/MMS, In-app messages**: Not collected.

### F. Photos and Videos
- **Photos**: Not collected.
- **Videos**: User imports videos locally via Storage Access Framework (SAF) for transcription or subtitle burn-in. All processing occurs locally on the device; videos are never collected or transmitted.

### G. Audio Files
- **Voice or Sound Recordings**: User records audio or imports audio files locally for transcription. Audio is processed entirely on-device by Whisper / sherpa-onnx. Audio files are never collected or transmitted.

### H. Files and Documents
- **Files / Docs**: Exported documents (PDF, DOCX, TXT, SRT) and backup files are saved directly to user-selected destinations on the device. Not collected.

### I. Calendar & Contacts
- **Calendar events, Contacts**: Not collected.

### J. App Activity & Performance
- **App interactions, in-app search history, installed apps**: Not collected. No analytics SDK is bundled.
- **Crash logs, diagnostics**: Collected only locally when the user explicitly taps "Export Diagnostics" in Settings and shares it via the Android Sharesheet.

### K. Device or Other IDs
- **Device IDs, Advertising ID, IMEI**: Not collected. `com.google.android.gms.permission.AD_ID` is not in the manifest.

---

## 3. Play Console Submission Checklist
- [x] Data collection declared as None for developer-transmitted data.
- [x] App does not contain ads.
- [x] Privacy Policy URL provided and points to public HTTPS policy document.
- [x] Data deletion mechanism described in policy (local Privacy & Data Controls + app uninstall).
