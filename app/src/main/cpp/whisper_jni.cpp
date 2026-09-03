#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <vector>

#include "whisper.h"
#include "ggml.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)

#define UNUSED(x) (void)(x)

// Structure to pass JNI environment and callback references during transcription
struct CallbackData {
    JNIEnv *env;
    jobject listener;
    jmethodID mid_onProgress;
    jmethodID mid_onNewSegment;
    jmethodID mid_isCancelled;
    struct whisper_context *ctx;
};

static void on_progress_callback(struct whisper_context * /*ctx*/, struct whisper_state * /*state*/, int progress, void * user_data) {
    auto *cb = static_cast<CallbackData *>(user_data);
    if (cb && cb->env && cb->listener && cb->mid_onProgress) {
        cb->env->CallVoidMethod(cb->listener, cb->mid_onProgress, (jint) progress);
        if (cb->env->ExceptionCheck()) {
            cb->env->ExceptionClear();
        }
    }
}

static void on_new_segment_callback(struct whisper_context * ctx, struct whisper_state * /*state*/, int n_new, void * user_data) {
    auto *cb = static_cast<CallbackData *>(user_data);
    if (!cb || !cb->env || !cb->listener || !cb->mid_onNewSegment) {
        return;
    }

    int total_segments = whisper_full_n_segments(ctx);
    int start_idx = total_segments - n_new;
    for (int i = start_idx; i < total_segments; ++i) {
        int64_t t0 = whisper_full_get_segment_t0(ctx, i) * 10; // Convert 10ms ticks to milliseconds
        int64_t t1 = whisper_full_get_segment_t1(ctx, i) * 10;
        const char * text = whisper_full_get_segment_text(ctx, i);

        jstring jtext = cb->env->NewStringUTF(text ? text : "");
        cb->env->CallVoidMethod(cb->listener, cb->mid_onNewSegment, (jlong) t0, (jlong) t1, jtext);
        cb->env->DeleteLocalRef(jtext);

        if (cb->env->ExceptionCheck()) {
            cb->env->ExceptionClear();
        }
    }
}

static bool on_abort_callback(void * user_data) {
    auto *cb = static_cast<CallbackData *>(user_data);
    if (cb && cb->env && cb->listener && cb->mid_isCancelled) {
        jboolean cancelled = cb->env->CallBooleanMethod(cb->listener, cb->mid_isCancelled);
        if (cb->env->ExceptionCheck()) {
            cb->env->ExceptionClear();
            return false;
        }
        return (bool) cancelled;
    }
    return false;
}

// Asset loader callbacks
static size_t asset_read(void *ctx, void *output, size_t read_size) {
    return AAsset_read((AAsset *) ctx, output, read_size);
}

static bool asset_is_eof(void *ctx) {
    return AAsset_getRemainingLength64((AAsset *) ctx) <= 0;
}

static void asset_close(void *ctx) {
    AAsset_close((AAsset *) ctx);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_initContext(
        JNIEnv *env, jobject /*thiz*/, jstring model_path_str) {
    if (!model_path_str) {
        LOGE("Model path is null");
        return 0;
    }

    const char *model_path = env->GetStringUTFChars(model_path_str, nullptr);
    LOGI("Loading Whisper model from file: %s", model_path);

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    struct whisper_context *context = whisper_init_from_file_with_params(model_path, cparams);
    env->ReleaseStringUTFChars(model_path_str, model_path);

    if (!context) {
        LOGE("Failed to initialize Whisper context from file");
        return 0;
    }

    LOGI("Whisper model loaded successfully: %p", context);
    return reinterpret_cast<jlong>(context);
}

JNIEXPORT jlong JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_initContextFromAsset(
        JNIEnv *env, jobject /*thiz*/, jobject asset_manager_obj, jstring asset_path_str) {
    if (!asset_manager_obj || !asset_path_str) {
        LOGE("Asset manager or asset path is null");
        return 0;
    }

    const char *asset_path = env->GetStringUTFChars(asset_path_str, nullptr);
    LOGI("Loading Whisper model from asset: %s", asset_path);

    AAssetManager *asset_manager = AAssetManager_fromJava(env, asset_manager_obj);
    if (!asset_manager) {
        LOGE("Failed to get AAssetManager");
        env->ReleaseStringUTFChars(asset_path_str, asset_path);
        return 0;
    }

    AAsset *asset = AAssetManager_open(asset_manager, asset_path, AASSET_MODE_BUFFER);
    if (!asset) {
        LOGE("Failed to open asset: %s", asset_path);
        env->ReleaseStringUTFChars(asset_path_str, asset_path);
        return 0;
    }

    whisper_model_loader loader = {
            .context = asset,
            .read = &asset_read,
            .eof = &asset_is_eof,
            .close = &asset_close
    };

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    struct whisper_context *context = whisper_init_with_params(&loader, cparams);
    env->ReleaseStringUTFChars(asset_path_str, asset_path);

    if (!context) {
        LOGE("Failed to initialize Whisper context from asset");
        return 0;
    }

    LOGI("Whisper model loaded from asset successfully: %p", context);
    return reinterpret_cast<jlong>(context);
}

JNIEXPORT void JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_freeContext(
        JNIEnv */*env*/, jobject /*thiz*/, jlong context_ptr) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (context) {
        LOGI("Freeing Whisper context: %p", context);
        whisper_free(context);
    }
}

JNIEXPORT jint JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_fullTranscribe(
        JNIEnv *env, jobject /*thiz*/, jlong context_ptr, jint num_threads,
        jfloatArray audio_data, jstring lang_str, jboolean translate, jobject listener) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (!context) {
        LOGE("Context pointer is null");
        return -1;
    }

    jfloat *samples = env->GetFloatArrayElements(audio_data, nullptr);
    jsize n_samples = env->GetArrayLength(audio_data);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = (bool) translate;
    params.n_threads = (num_threads > 0) ? num_threads : 4;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    std::string language_str;
    if (lang_str) {
        const char *l = env->GetStringUTFChars(lang_str, nullptr);
        language_str = l;
        env->ReleaseStringUTFChars(lang_str, l);
        if (language_str == "auto" || language_str.empty()) {
            params.language = "auto";
            params.detect_language = true;
        } else {
            params.language = language_str.c_str();
            params.detect_language = false;
        }
    } else {
        params.language = "en";
    }

    CallbackData cb_data = {};
    if (listener) {
        cb_data.env = env;
        cb_data.listener = listener;
        cb_data.ctx = context;

        jclass listener_class = env->GetObjectClass(listener);
        cb_data.mid_onProgress = env->GetMethodID(listener_class, "onProgress", "(I)V");
        cb_data.mid_onNewSegment = env->GetMethodID(listener_class, "onNewSegment", "(JJLjava/lang/String;)V");
        cb_data.mid_isCancelled = env->GetMethodID(listener_class, "isCancelled", "()Z");

        params.progress_callback = on_progress_callback;
        params.progress_callback_user_data = &cb_data;

        params.new_segment_callback = on_new_segment_callback;
        params.new_segment_callback_user_data = &cb_data;

        params.abort_callback = on_abort_callback;
        params.abort_callback_user_data = &cb_data;
    }

    LOGI("Starting transcription: %d samples, threads=%d, lang=%s", n_samples, params.n_threads, params.language);
    whisper_reset_timings(context);

    int result = whisper_full(context, params, samples, n_samples);

    env->ReleaseFloatArrayElements(audio_data, samples, JNI_ABORT);

    if (result != 0) {
        LOGE("whisper_full failed with code: %d", result);
    } else {
        LOGI("whisper_full completed successfully");
        whisper_print_timings(context);
    }

    return result;
}

JNIEXPORT jint JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_getTextSegmentCount(
        JNIEnv */*env*/, jobject /*thiz*/, jlong context_ptr) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    return context ? whisper_full_n_segments(context) : 0;
}

JNIEXPORT jstring JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_getTextSegment(
        JNIEnv *env, jobject /*thiz*/, jlong context_ptr, jint index) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (!context) return env->NewStringUTF("");
    const char *text = whisper_full_get_segment_text(context, index);
    return env->NewStringUTF(text ? text : "");
}

JNIEXPORT jlong JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_getTextSegmentT0(
        JNIEnv */*env*/, jobject /*thiz*/, jlong context_ptr, jint index) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (!context) return 0;
    return whisper_full_get_segment_t0(context, index) * 10; // Convert 10ms units to ms
}

JNIEXPORT jlong JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_getTextSegmentT1(
        JNIEnv */*env*/, jobject /*thiz*/, jlong context_ptr, jint index) {
    auto *context = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (!context) return 0;
    return whisper_full_get_segment_t1(context, index) * 10; // Convert 10ms units to ms
}

JNIEXPORT jstring JNICALL
Java_app_offlinetranscriber_mobile_transcription_WhisperNative_getSystemInfo(
        JNIEnv *env, jobject /*thiz*/) {
    const char *info = whisper_print_system_info();
    return env->NewStringUTF(info ? info : "Unknown");
}

} // extern "C"
