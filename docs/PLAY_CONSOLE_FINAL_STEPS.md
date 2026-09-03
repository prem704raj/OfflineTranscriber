# Google Play Console Launch Steps

1. **Create App in Google Play Console**
   - Application ID: `app.offlinetranscriber.mobile`
   - App name: `Offline Transcriber`
   - Default language: English (United States)
   - App or Game: App
   - Free or Paid: Free (with In-App Purchases)

2. **Set up Store Presence**
   - Enter Main store listing details from `docs/STORE_LISTING_FINAL.md`.
   - Upload App Icon (512x512) and Feature Graphic (1024x500) from `docs/PLAY_ASSET_PLAN.md`.
   - Upload Phone & Tablet screenshots.

3. **Complete App Content Declarations**
   - **Privacy policy:** Provide your hosted public URL of `docs/PRIVACY_POLICY_FINAL.md`.
   - **App access:** All functionality available without restrictions.
   - **Ads:** No ads.
   - **Content rating:** Complete IARC questionnaire (Productivity / Utility).
   - **Target audience:** Ages 13 and up.
   - **Data safety:** Complete form using `docs/DATA_SAFETY_FINAL_CHECK.md`.
   - **Foreground Services (FGS):** Declare `dataSync` and `mediaProcessing`.

4. **Set up In-App Products (Google Play Billing)**
   - In-app product ID: `pro_lifetime`
   - Name: `Offline Transcriber Pro`
   - Description: `Lifetime access to advanced offline transcription features.`
   - Pricing: Set local base price (e.g. ₹349 in India / $4.99 in US) and convert for other regions.
   - Status: Set to **Active**.

5. **Upload Production Bundle (AAB)**
   - Build bundle: `.\gradlew.bat bundleRelease`
   - Artifact location: `app/build/outputs/bundle/release/app-release.aab`
   - Upload to **Internal Testing** track first.

6. **Internal & Closed Testing**
   - Add license testers in Play Console (Settings > License testing) to test Pro purchase flows with zero-cost test cards.
   - Verify: Model downloads, audio transcription, video Subtitle Studio, Study Mode, purchase restoration, and offline operations.
   - For personal developer accounts created after Nov 13, 2023: run Closed Test with $\ge 12$ opted-in testers for 14 continuous days.

7. **Production Release**
   - Promote verified release from Testing to **Production**.
   - Paste release notes from `docs/RELEASE_NOTES_1.0.0.md`.
   - Submit for Google Play review.
