cmake_minimum_required(VERSION 3.22.1)

project("whisper-android" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

set(WHISPER_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR})

include_directories(${WHISPER_SRC_DIR}/include)
include_directories(${WHISPER_SRC_DIR}/src)
include_directories(${WHISPER_SRC_DIR}/ggml/include)
include_directories(${WHISPER_SRC_DIR}/ggml/src)
include_directories(${WHISPER_SRC_DIR}/ggml/src/ggml-cpu)

find_library(LOG_LIB log)
find_library(ANDROID_LIB android)

# Include GGML subdirectory
set(GGML_STANDALONE OFF)
set(GGML_BUILD_EXAMPLES OFF)
set(GGML_BUILD_TESTS OFF)
set(BUILD_SHARED_LIBS OFF)

add_subdirectory(${WHISPER_SRC_DIR}/ggml ${CMAKE_CURRENT_BINARY_DIR}/ggml)

# Main JNI library
add_library(
    whisper_jni
    SHARED
    ${WHISPER_SRC_DIR}/src/whisper.cpp
    ${WHISPER_SRC_DIR}/whisper_jni.cpp
)

target_compile_definitions(whisper_jni PUBLIC GGML_USE_CPU WHISPER_VERSION="1.7.4")

# 16 KB memory-page alignment for Android 15/16 and Google Play requirements
target_link_options(
    whisper_jni
    PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)

# ABI-specific compiler optimization flags
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_options(whisper_jni PRIVATE -O3 -fPIC)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_options(whisper_jni PRIVATE -O3 -fPIC -mfpu=neon-vfpv4)
elseif (${ANDROID_ABI} STREQUAL "x86_64")
    target_compile_options(whisper_jni PRIVATE -O3 -fPIC -mavx -mavx2 -mfma -mf16c)
else()
    target_compile_options(whisper_jni PRIVATE -O3 -fPIC)
endif()

target_link_libraries(
    whisper_jni
    PRIVATE
    ${LOG_LIB}
    ${ANDROID_LIB}
    ggml
    ggml-cpu
    ggml-base
)
