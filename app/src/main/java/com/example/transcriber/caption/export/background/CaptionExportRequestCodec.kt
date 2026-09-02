package com.example.transcriber.caption.export.background

import com.example.transcriber.caption.model.CaptionExportRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object CaptionExportRequestCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(request: CaptionExportRequest): String =
        json.encodeToString(request)

    fun decode(jsonString: String): CaptionExportRequest =
        json.decodeFromString(jsonString)

    fun write(file: File, request: CaptionExportRequest) {
        val encoded = encode(request)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(encoded, Charsets.UTF_8)
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    fun read(file: File): CaptionExportRequest {
        val text = file.readText(Charsets.UTF_8)
        return decode(text)
    }
}
