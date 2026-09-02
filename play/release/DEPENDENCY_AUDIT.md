# Production Dependency Audit

All dependencies are pinned to exact versions without dynamic (`+`) or snapshot ranges.

| Artifact | Version | Purpose |
| :--- | :--- | :--- |
| `com.android.application` | `9.0.1` | Android Gradle Plugin (16 KB page-size compatible) |
| `org.jetbrains.kotlin.android` | `2.3.20` | Kotlin Multiplatform / JVM Compiler |
| `com.google.devtools.ksp` | `2.3.11` | Kotlin Symbol Processing for Room & Navigation |
| `androidx.compose.bom` | `2026.03.01` | Jetpack Compose Bill of Materials |
| `androidx.core:core-ktx` | `1.18.0` | Core Android KTX extensions |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.10.0` | AndroidX Lifecycle |
| `androidx.room:room-runtime` | `2.7.1` | SQLite Object Mapping (Room Database) |
| `androidx.room:room-ktx` | `2.7.1` | Room Coroutines support |
| `androidx.media3:media3-exoplayer` | `1.5.1` | Media3 Audio/Video playback |
| `androidx.media3:media3-transformer` | `1.5.1` | Media3 Subtitle & Video Export |
| `androidx.media3:media3-effect` | `1.5.1` | Media3 Visual Canvas Overlays |
| `androidx.navigation3:navigation3-runtime` | `1.0.1` | Jetpack Compose Navigation 3 |
| `com.android.billingclient:billing` | `9.1.0` | Google Play Billing Client |
| `com.android.billingclient:billing-ktx` | `9.1.0` | Google Play Billing KTX |
| `com.google.mlkit:genai-prompt` | `1.0.0-beta4`| ML Kit On-Device GenAI Prompt API |
| `org.apache.commons:commons-compress` | `1.26.2` | Zip / Tar model archive validation |
