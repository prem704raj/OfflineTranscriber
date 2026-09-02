# Google Play Data Safety Answers

This document provides accurate, audited responses for completing the Google Play Console Data Safety questionnaire for **Offline Transcriber (com.example.transcriber)**.

---

## 1. Overview & Data Collection Summary

| Question | Answer | Rationale |
| :--- | :--- | :--- |
| **Does your app collect or share any user data?** | **Yes** (Only standard Play Billing purchase history handled by Google Play & user-initiated file exports) | The app itself does NOT collect or upload personal data to any developer server. However, Google Play Billing handles financial transaction data. |
| **Is all user data encrypted in transit?** | **Yes** | Model downloads (Hugging Face) and Google Play Billing use HTTPS/TLS. |
| **Do you provide a way for users to request data deletion?** | **Yes** | Users can delete transcripts, recordings, study packs, models, and all app data directly inside the app via **Settings > Privacy & Data**. |

---

## 2. Detailed Data Category Breakdown

### A. Audio Files / Voice Recordings
- **Collected?**: **No**.
- **Shared?**: **No**.
- **Processed Locally?**: **Yes**.
- **Details**: Audio files and microphone recordings selected or created by the user are processed **100% locally on-device** using whisper.cpp. Audio data is **never** transmitted to any external server or cloud service for transcription.

### B. Financial Info (Purchase History)
- **Collected?**: **Yes** (Collected by Google Play).
- **Shared?**: **No**.
- **Purpose**: **App Functionality / Fraud Prevention**.
- **Processed Ephemerally?**: No (stored in Google Play account).
- **Required / Optional**: Optional (only if purchasing Lifetime Pro).
- **Details**: Google Play Billing processes and manages in-app purchase transactions (`pro_lifetime`). The developer receives purchase confirmation status from Google Play, but never receives credit card numbers, banking details, or billing addresses.

### C. App Info and Performance (Diagnostics)
- **Collected?**: **No** (Local only, unless user manually shares diagnostic report).
- **Shared?**: **No automatic sharing**.
- **Details**: Local diagnostic metrics (CPU cores, RAM size, installed model name, database row count) remain on-device. If a user explicitly clicks "Share diagnostics", the data is handed over to the Android Share Sheet to a destination chosen solely by the user.

### D. Files and Docs (Transcripts, Subtitles, Study Data)
- **Collected?**: **No**.
- **Shared?**: **No automatic sharing**.
- **Details**: Transcripts, SRT/VTT subtitles, bookmarks, collections, and study flashcards/quizzes are stored strictly in the local SQLite Room database on the device.

---

## 3. Network Communication Audit

1. **Whisper Model Downloads**:
   - **Endpoint**: `https://huggingface.co/`
   - **Data Sent**: Standard HTTP GET request for `.bin` model files.
   - **Data Received**: GGML Whisper model weights. Zero user data is sent.

2. **Google Play Billing**:
   - **Endpoint**: Google Play Services (IPC)
   - **Data Sent**: Product ID (`pro_lifetime`) query and purchase verification.
   - **Data Received**: Purchase token and SKU entitlement status.

3. **Gemini Nano (Optional Study Enhancement)**:
   - **Endpoint**: Google ML Kit / Android AICore System Service
   - **Data Sent**: On-device prompt text.
   - **Data Received**: On-device generated study points/flashcards. Processed locally via system AICore.
