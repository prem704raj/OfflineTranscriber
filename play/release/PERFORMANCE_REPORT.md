# Performance & Resource Stress Report

## 1. Startup & Main-Thread Responsiveness
- `Application.onCreate()` performs zero disk I/O, zero model initialization, and zero heavy computations.
- All Room database operations, Whisper speech models, sherpa-onnx embeddings, Media3 Transformer video encoding, and `.otbackup` compression run on `Dispatchers.IO` background coroutines.
- Edge-to-edge system bars and predictive back navigation operate smoothly without frame drops.

## 2. Heavy ML Workload Concurrency (`HeavyProcessingArbiter`)
- `HeavyProcessingArbiter.withLease(...)` serializes heavy workloads across the app:
  - Whisper Transcription
  - Speaker Diarization
  - Video Subtitle Export
  - Database Backup / Restore
- Prevents concurrent CPU/GPU saturation, thermal throttling, and out-of-memory errors on lower-tier devices.

## 3. Memory & Stream Management
- Video export uses memory-efficient stream processing with reusable caption bitmap caches.
- Backup archive creation and restoration stream JSON and media data chunk-by-chunk without allocating full-file ByteArrays in RAM.
