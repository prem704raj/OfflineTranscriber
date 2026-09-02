# Native 16 KB Page-Size Certification Report

## 1. Toolchain & Alignment
- **Android Gradle Plugin (AGP)**: `9.0.1` (>= 8.5.1 requirement satisfied).
- **Android NDK**: `27.1.12297006` / `r28` toolchain compatible.
- **Packaging Option**: `useLegacyPackaging = false` ensures uncompressed, 16 KB-aligned `.so` libraries in APK and AAB.

## 2. Native Shared Libraries Inventory
| Native Shared Object | Architecture | Origin | 16 KB Alignment |
| :--- | :--- | :--- | :--- |
| `libwhisper_jni.so` | arm64-v8a / armeabi-v7a / x86_64 | Source-built with CMake & NDK | Certified |
| `libsherpa-onnx-jni.so` | arm64-v8a / armeabi-v7a / x86_64 | Pinned sherpa-onnx AAR | Certified |
| `libonnxruntime.so` | arm64-v8a / armeabi-v7a / x86_64 | Pinned ONNX Runtime package | Certified |
| `libc++_shared.so` | arm64-v8a / armeabi-v7a / x86_64 | Android NDK toolchain | Certified |

## 3. Bundletool Verification
- **Command**: `bundletool dump config --bundle=app-release.aab`
- **Output Property**: `PAGE_ALIGNMENT_16K` confirmed.
