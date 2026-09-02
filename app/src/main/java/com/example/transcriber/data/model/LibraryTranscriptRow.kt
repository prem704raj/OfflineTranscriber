package com.example.transcriber.data.model

data class LibraryTranscriptRow(
    val id: Long,
    val title: String,
    val audioUri: String,
    val sourceUri: String,
    val mediaType: String,
    val durationMs: Long,
    val createdAt: Long,
    val bookmarkCount: Int
)
