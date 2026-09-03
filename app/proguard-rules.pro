# ProGuard / R8 Rules for Offline Transcriber

# Keep all native method declarations across all classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Whisper JNI engine and native bridge
-keep class app.offlinetranscriber.mobile.transcription.WhisperNative { *; }
-keep class app.offlinetranscriber.mobile.transcription.TranscriptionListener { *; }
-keep class app.offlinetranscriber.mobile.whisper.** { *; }

# Keep Room generated and runtime classes
-keep class androidx.room.** { *; }

# Keep Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep Google Play Billing classes
-keep class com.android.billingclient.** { *; }

# Keep Google ML Kit GenAI Prompt API
-keep class com.google.mlkit.** { *; }

# Keep Media3 ExoPlayer components
-keep class androidx.media3.** { *; }

# Keep sherpa-onnx JNI and Kotlin bindings
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

