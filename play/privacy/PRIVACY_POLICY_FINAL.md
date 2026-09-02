# Privacy Policy for Offline Transcriber

**Last Updated**: September 2, 2026

Offline Transcriber ("we", "our", or "the app") is committed to protecting your privacy. This privacy policy explains our on-device processing architecture, data handling practices, and your rights.

## 1. Core Principle: Local On-Device Processing
Offline Transcriber is designed from the ground up as a private, local-first application. Transcription, audio processing, speaker identification, and text analysis are performed locally on your Android device. We do not operate a remote cloud transcription server or transmit your audio/video files to any remote server for transcription.

## 2. Audio, Video, and Transcript Data
- **Local Storage**: All recorded audio, imported audio/video files, transcript segments, notes, study packs, AI chat conversations, and meeting action items are stored in your device's private application sandbox (`AppDatabase` SQLite / Room database and local app files directory).
- **No Cloud Upload**: Your recordings and transcripts are never automatically uploaded to our servers, cloud providers, or third parties.
- **User-Directed Sharing**: When you choose to export transcripts (e.g., as PDF, DOCX, TXT, Markdown, SRT, VTT, or captioned video) or share via Android's system Sharesheet, the content is transferred solely to the destination or recipient application you explicitly select.

## 3. Network Usage & Internet Permissions
The app requests standard Internet access (`android.permission.INTERNET`) strictly for the following purposes:
1. **On-Device Model Downloads**: Downloading optional offline speech recognition models (Whisper) and speaker diarization neural networks directly from public release repositories (e.g., Hugging Face, GitHub).
2. **Google Play Billing**: Connecting to the Google Play Store to verify and complete purchases for the optional, one-time lifetime Pro unlock.

No user audio, video, transcript content, or search queries are transmitted during these network operations.

## 4. Google Play Billing
- In-app purchases are handled directly by Google Play Billing services.
- We do not receive, process, or store your credit card numbers, billing addresses, or personal financial details.
- Pro entitlement status is cached locally on your device.

## 5. On-Device AI & Intelligence (Classic & Gemini Nano)
- **Classic On-Device Processing**: Core summarization, chapter generation, quiz creation, and Q&A operate locally using built-in deterministic algorithms.
- **Gemini Nano / System AI**: When supported by your device's Android system capabilities (via Google ML Kit On-Device GenAI Prompt API), queries are processed locally by the on-device system AI model. No prompt text or transcript excerpts leave your device.

## 6. Speaker Intelligence & Diarization
- Speaker diarization neural embeddings and clustering are calculated locally on your device using on-device ML runtimes (ONNX Runtime / Sherpa-ONNX).
- Audio voiceprints are processed in transient memory and are not stored in biometric databases or transmitted externally.

## 7. Portable Backups & Encryption
- You can generate a portable `.otbackup` archive of your library.
- Backups can be encrypted with industry-standard AES-256-GCM using a user-provided password.
- Backups are stored in your chosen device directory or storage provider via the Android Storage Access Framework (SAF).

## 8. App Permissions & Justifications
- `RECORD_AUDIO`: Required to record live audio when you press the Record button.
- `POST_NOTIFICATIONS`: Required on Android 13+ to display background processing status (transcription, recording, video export).
- `FOREGROUND_SERVICE` (Microphone, Data Sync, Media Processing): Required to ensure ongoing recording, transcription, and video rendering tasks complete without being prematurely terminated when the app is minimized.

## 9. Data Retention and Deletion
You have complete control over your data:
- You can delete individual transcripts, recordings, collections, and study decks at any time from the app UI.
- The **Privacy & Data Controls** screen allows you to clean temporary caches, delete downloaded models, or perform a complete, permanent erasure of all app data.
- Uninstalling the application automatically deletes all sandboxed application data from your device.

## 10. No Advertising, Analytics, or Tracking
- The application contains **zero advertisements**.
- The application contains **no third-party tracking or analytics SDKs** (such as Firebase Analytics, Facebook SDK, or AdMob).

## 11. Children's Privacy
The application does not collect personal information from any user, including children under 13.

## 12. Changes to This Policy
Any future updates to this policy will be reflected on this page with an updated revision date.

## 13. Contact Us
For questions regarding this privacy policy, please contact our support team at:
`support@offlinetranscriber.app`
