# Google Play Foreground Service (FGS) Declarations

This document contains the exact descriptions, user impact statements, and demonstration video guidelines for Google Play Console foreground service declarations.

---

## 1. Foreground Service Type: `microphone`
- **Service Name**: `app.offlinetranscriber.mobile.recording.RecordingService` (or active recording service)
- **Declared Permissions**: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_MICROPHONE`, `android.permission.RECORD_AUDIO`

### A. Feature Name
> Audio recording

### B. User-Facing Description
> The user taps the Record button to record a live lecture, meeting, interview, or voice memo. Recording must continue uninterrupted while the user switches to other apps, reads documents, or turns off the screen. A persistent notification is displayed showing live recording status and a direct button to stop recording.

### C. User Impact if Deferred
> If deferred, the requested recording would fail to capture the live event, causing irreversible loss of user audio.

### D. User Impact if Interrupted
> If interrupted, the active recording would terminate abruptly and lose the remaining speech content.

### E. Video Demonstration Checklist
1. Open the app and navigate to the Home / Record tab.
2. Tap the microphone / Record button.
3. Grant the runtime microphone permission.
4. Show active recording UI with live timer and audio visualizer.
5. Minimize the app / press the Home button.
6. Pull down the Android notification shade to demonstrate the ongoing persistent recording notification.
7. Tap the notification to return to the app and press Stop Recording.
8. Verify the transcript is generated and saved.

---

## 2. Foreground Service Type: `mediaProcessing`
- **Service Names**:
  - `app.offlinetranscriber.mobile.background.TranscriptionForegroundService`
  - `app.offlinetranscriber.mobile.speaker.background.SpeakerDiarizationForegroundService`
  - `app.offlinetranscriber.mobile.caption.export.background.CaptionExportForegroundService`
- **Declared Permissions**: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_MEDIA_PROCESSING`

### A. Feature Name
> On-device speech transcription, speaker diarization, and video subtitle export

### B. User-Facing Description
> The user initiates long, compute-intensive local media processing tasks—such as transcribing multi-hour audio files using on-device Whisper models, analyzing speaker turns with neural embeddings, or rendering styled burned-in subtitles into video files via Media3 Transformer. These operations run locally on the device and must continue executing when the user backgrounds the app. A persistent notification displays real-time progress percentage, current stage, and a cancel action.

### C. User Impact if Deferred
> The user would be forced to keep the screen active and the app in the foreground for up to several minutes or hours during long audio/video processing.

### D. User Impact if Interrupted
> The processing pipeline would be killed mid-stream, corrupting partial export files and wasting device CPU/battery work.

### E. Video Demonstration Checklist
1. Select an audio or video file to transcribe or export with burned-in subtitles.
2. Start the transcription or video export process.
3. Show the progress indicator starting.
4. Switch to another app or the home screen.
5. Pull down the notification drawer showing the persistent `mediaProcessing` notification with live progress bar and Cancel button.
6. Allow processing to finish and verify completion notification.

---

## 3. Foreground Service Type: `dataSync`
- **Service Name**: `app.offlinetranscriber.mobile.backup.background.BackupRestoreForegroundService`
- **Declared Permissions**: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_DATA_SYNC`

### A. Feature Name
> Portable database backup and library restoration

### B. User-Facing Description
> The user initiates creating a password-encrypted `.otbackup` archive of their entire transcript library or restoring a backup file. The service ensures that multi-megabyte/gigabyte database serialization, checksum hashing, and archive streaming complete reliably even if the app loses foreground focus during large library exports or imports.

### C. User Impact if Deferred / Interrupted
> An interrupted backup or restore would result in an incomplete or corrupted backup file or an uncommitted database transaction.

### D. Video Demonstration Checklist
1. Go to Settings -> Backup & Restore -> Create Backup.
2. Select destination file via Storage Access Framework.
3. Show backup progress notification in background.
4. Verify completed notification.
