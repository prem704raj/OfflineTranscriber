package com.example.transcriber.recorder

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File

class LiveAudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    fun start(): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "recordings"
        ).apply { mkdirs() }

        val file = File(dir, "recording_${System.currentTimeMillis()}.m4a")
        currentFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mr
        isRecording = true
        return file
    }

    fun stop(): Uri? {
        if (!isRecording) return null
        return runCatching {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false

            currentFile?.let { Uri.fromFile(it) }
        }.getOrNull()
    }

    fun cancel() {
        if (!isRecording) return
        runCatching {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            currentFile?.delete()
            currentFile = null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
