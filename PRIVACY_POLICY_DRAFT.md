# Privacy Policy for Offline Transcriber

**Last Updated: September 1, 2026**

Offline Transcriber ("we", "our", or "the app") is committed to protecting your privacy. Offline Transcriber is built with an **offline-first, privacy-by-design** architecture: core audio and video transcription runs entirely on your device, and your media is never uploaded to any remote transcription server.

---

## 1. Local On-Device Processing
- **Audio & Video Processing**: All speech-to-text transcription is executed locally on your Android device using bundled or downloaded Whisper speech models powered by `whisper.cpp`. Your audio files, voice recordings, and video media are never transmitted over the internet to us or any third party for transcription.
- **Transcript Storage**: All generated transcript text, synchronized timestamps, bookmarks, collections, and study materials are stored in a private local database on your device.
- **Study Mode**: AI study intelligence (summaries, flashcards, quizzes) runs on-device using local rule-based intelligence or Google ML Kit / Gemini Nano on-device system AI (where supported). No transcript text is sent to cloud AI servers.

---

## 2. Information We Do NOT Collect
- We do **not** collect your audio files, recordings, or video files.
- We do **not** collect or read your transcript texts.
- We do **not** require user registration, account creation, passwords, or emails.
- We do **not** embed advertising SDKs, behavioral trackers, or third-party analytics (e.g., Firebase Analytics, Facebook SDK, Google Analytics).

---

## 3. Network Connections & Third-Party Services
Offline Transcriber connects to the internet strictly for the following essential functionalities:
1. **Model Downloads**: When you choose to download speech models (Fast, Balanced, Accurate), the app connects via HTTPS to Hugging Face to download the open-source GGML model binary files.
2. **Google Play In-App Purchases**: In-app purchases (Lifetime Pro) are processed securely by Google Play Billing. Google Play handles payment card information in accordance with the Google Privacy Policy. We only receive purchase entitlement status (active/inactive) to unlock Pro features.
3. **User-Initiated Sharing**: When you tap "Share transcript", "Export SRT", "Export VTT", or "Share diagnostics", data is transferred via standard Android Share mechanisms exclusively to the destination app or cloud service that you explicitly choose.

---

## 4. Backup & Data Security
- To ensure maximum privacy, Offline Transcriber explicitly disables Android cloud backup for your private transcripts, databases, and audio cache via application-level data extraction rules.
- Local diagnostics reports never contain audio recordings, transcript contents, file paths, URIs, or purchase tokens.

---

## 5. Your Data Controls & Deletion
You maintain full control over your data at all times. In **Settings > Privacy & Data**, you can:
- **Clean Temporary Files**: Delete cached export files safely.
- **Delete Transcription Content**: Permanently delete all transcripts, segments, bookmarks, collections, study packs, queue records, and app-owned recordings.
- **Delete Downloaded Models**: Remove stored speech model files to reclaim device storage.
- **Delete All Local App Data**: Perform a complete, irreversible wipe of all local data, settings, and caches.

---

## 6. Children's Privacy
Offline Transcriber does not knowingly collect personal information from children. Because all transcription processing is local, children's voice recordings remain strictly on the device.

---

## 7. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please contact:
- **Developer**: Offline Transcriber Support Team
- **Email**: `support@offlinetranscriber.app`
