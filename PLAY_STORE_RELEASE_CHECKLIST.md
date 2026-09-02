# Play Store Release Checklist

## 1. Application Build & Signing
- [x] **Package Name / Application ID**: `com.example.transcriber`
- [x] **Version Code**: `1` (Unique positive integer for release)
- [x] **Version Name**: `1.0.0`
- [x] **Target SDK**: `36` (Android 16 compatibility)
- [x] **Min SDK**: `26` (Android 8.0 Oreo)
- [x] **Compilation Mode**: Minified and resource-shrunk (`isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`)
- [x] **Native Architecture Support**: `arm64-v8a`, `armeabi-v7a`, `x86_64` supported by CMake + whisper.cpp
- [ ] **Release Keystore**: Create release keystore and configure Play App Signing in Google Play Console
- [ ] **App Bundle (AAB)**: Generate release bundle via `./gradlew bundleRelease`

## 2. In-App Monetization (Google Play Billing)
- [x] **Billing Library**: Google Play Billing 9.1.0 (`com.android.billingclient:billing:9.1.0`)
- [x] **Product ID**: `pro_lifetime` (Single non-consumable one-time purchase)
- [x] **Zero Hardcoded Prices**: Dynamic localized price query via `BillingClient.queryProductDetails()`
- [x] **Pending Purchases**: Handled gracefully (`enablePendingPurchases`)
- [x] **Offline Cache**: DataStore cache with automatic background re-verification on network connect
- [x] **Restore Purchases**: Dedicated "Restore purchase" button in Settings and Privacy Center
- [ ] **Play Console In-App Product Setup**: Create in-app product `pro_lifetime` in Play Console and activate pricing

## 3. Privacy, Security & Data Safety
- [x] **Local Processing**: Whisper speech recognition runs 100% locally via NDK/C++
- [x] **No Cloud Audio Uploads**: No third-party transcription servers or cloud audio storage
- [x] **No Remote Telemetry**: Zero Firebase, Crashlytics, AdMob, or tracking SDKs
- [x] **Backup Exclusion**: `android:allowBackup="false"` and `data_extraction_rules.xml` exclude private transcripts and models
- [x] **FileProvider Tightening**: `file_paths.xml` restricted strictly to `shared_subtitles/` and `diagnostics/`
- [x] **Exported Components Audit**: Only `MainActivity` is exported with launcher intent; all services and providers `exported="false"`
- [x] **Safe Deletion**: Privacy Center provides transactional deletion of transcripts, models, queue, and local data
- [x] **Sanitized Diagnostics**: Shared diagnostics strip transcripts, file paths, URIs, and purchase tokens
- [ ] **Privacy Policy URL**: Host `PRIVACY_POLICY_DRAFT.md` on a public HTTPS URL and link in Play Console

## 4. Store Listing Assets
- [x] **App Title**: Offline Transcriber
- [x] **Short Description**: Private audio and video transcription that runs on your phone.
- [x] **Full Description**: Completed in `STORE_LISTING_DRAFT.md`
- [ ] **App Icon**: 512x512 PNG (32-bit color, up to 1024KB)
- [ ] **Feature Graphic**: 1024x500 PNG/JPEG (no transparency)
- [ ] **Screenshots**: At least 4 phone screenshots (16:9 or 18:9 aspect ratio)
- [x] **Category**: Productivity / Tools
- [x] **Content Rating**: Complete IARC questionnaire (PEGI 3 / Everyone)

## 5. Rollout Strategy
1. **Internal Testing Track**: Test with internal team for in-app purchase verification and model download.
2. **Closed Testing Track**: Verify across diverse physical hardware (RAM < 4GB, 6GB, 8GB+, Android 10–15).
3. **Staged Production Rollout**:
   - Day 1: 10%
   - Day 2: 25%
   - Day 3: 50%
   - Day 4: 100%
