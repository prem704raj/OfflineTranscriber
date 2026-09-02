# Google Play App Content Declarations Checklist

## 1. App Access
- **Option:** All functionality is available without special access restrictions.
- **Notes:** Free audio transcription, recording, search, and local features are immediately functional with no login or credentials required.

## 2. Ads
- **Option:** No, my app does not contain ads.
- **Verification:** Verified zero ad SDKs in Gradle dependencies.

## 3. Content Rating (IARC Questionnaire)
- **Category:** Utility, Productivity, Communication, or Other.
- **Violence / Sexual content / Profanity / Controlled substances:** No.
- **User interaction:** No public chat or online user-to-user sharing.
- **Expected Rating:** Everyone / PEGI 3 / General.

## 4. Target Audience and Content
- **Target Age Groups:** 18 and over, 13–17.
- **Appeal to children:** No (app is a productivity/utility tool designed for students, journalists, professionals).

## 5. News App
- **Option:** No.

## 6. COVID-19 Contact Tracing & Status
- **Option:** Not applicable.

## 7. Data Safety
- Complete as documented in `docs/DATA_SAFETY_FINAL_CHECK.md`.

## 8. Government Apps
- **Option:** No.

## 9. Financial Features
- **Option:** Not a financial institution; standard Google Play In-App Purchase used for one-time Pro unlock.

## 10. Foreground Services (FGS) Declaration
- **Types used:**
  - `dataSync`: Background transcription job queue execution.
  - `mediaProcessing`: Audio extraction from video files before transcription.
- **User experience:** Notification displays active transcription progress with clean stop/pause controls.
