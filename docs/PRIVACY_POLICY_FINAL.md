# Privacy Policy — Offline Transcriber

Effective date: September 1, 2026

Offline Transcriber is designed around local processing and user control.

## Media you choose
The app can access audio, video, and microphone recordings that you explicitly select or create for transcription.

## Local transcription
Core speech-to-text processing is performed directly on your device using locally stored speech recognition models. Recordings are never sent to a developer-operated transcription server.

## Local data
The app stores all user data locally on your device:
- Transcripts and segment timestamps
- Bookmarks and collections
- Study packs and flashcard progress
- Questions and answers created in Ask Your Transcript/Library
- Meeting summaries, decisions, action items, follow-up questions and topic timelines
- Speaker diarization runs, speaker cluster labels, and speaker timeline turns
- Transcription queue state
- User preferences
- Downloaded speech-recognition and speaker-intelligence model files

## Speaker Intelligence & Diarization
Speaker separation and identification are processed 100% locally on your device. The app does not construct, persist, or transmit biometric voiceprints. Speaker labels are linked only to relative audio timeline intervals and are purged whenever the parent transcript or local data is deleted.

## Network connections
The app connects to network and platform services solely for:
- Downloading open speech-recognition model files (from HuggingFace/official repositories)
- Google Play purchase processing and ownership restoration via Google Play Billing
- Optional Android/Google on-device AI model availability checks and provisioning
- User-initiated links (such as viewing open-source licenses or this privacy policy)

Core speech transcription does not require an active internet connection once the speech model is installed.

## Google Play Billing
Purchases are processed securely by Google Play. The developer does not receive, process, or store your payment card details.

## Study tools
Supported devices may use optional Android/Google on-device generative AI capabilities (Gemini Nano via ML Kit Prompt API). The app also includes a Classic local offline heuristic fallback.

## Sharing & Export
When you explicitly use Android Share or Subtitle Export, selected content (such as plain text, SRT, or VTT files) is sent only to the destination or application you choose.

## Diagnostics
Diagnostics are completely local by default and are strictly designed to exclude transcript text, media filenames/URIs, prompts, and purchase tokens. They leave your device only if you explicitly choose to share them for troubleshooting.

## Deletion
The Privacy & Data section within Settings allows you to clean temporary files, delete transcription history, delete downloaded models, reset preferences, or completely erase all local app-owned data.

Deleting local billing cache does not refund or cancel a Google Play lifetime purchase; Google Play may restore verified lifetime ownership at any time.

## Advertising and analytics
Offline Transcriber contains zero advertising by design. No developer-operated analytics or behavioral tracking SDKs are integrated into the application.

## Contact
Developer: Offline Transcriber Team
Privacy contact: privacy@offlinetranscriber.app
App: Offline Transcriber
