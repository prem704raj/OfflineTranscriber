# Google Play Release Checklist

- [x] **Target SDK**: Configured to `targetSdk = 36` (Android 16).
- [x] **Compile SDK**: Configured to `compileSdk = 36`.
- [x] **Min SDK**: Set to `minSdk = 26` (Android 8.0 Oreo+).
- [x] **Version Code**: Set to integer incremented from previous release.
- [x] **Version Name**: Semantic release version (e.g., `1.0.0` or `1.2.0`).
- [x] **16 KB Page Size**: AGP >= 8.5.1, NDK 16 KB compliant, `PAGE_ALIGNMENT_16K` in AAB.
- [x] **R8 Minification**: Enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- [x] **Security**: No cleartext traffic, no hardcoded API keys, no sensitive content in logs.
- [x] **Foreground Services**: All FGS types declared (`microphone`, `dataSync`, `mediaProcessing`) with appropriate runtime permissions.
- [x] **In-App Billing**: Verified `pro_lifetime` non-consumable product with local offline caching.
- [x] **Store Listing**: Title (<= 30 chars), Short Description (<= 80 chars), Full Description (<= 4000 chars) verified.
- [x] **Data Safety**: All sections audited and verified for local on-device operation.
- [x] **Backup & Restore**: Portable `.otbackup` format with AES-256-GCM encryption verified.
