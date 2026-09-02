package com.example.transcriber.share

sealed interface ShareImportUiState {
    data object Importing : ShareImportUiState
    data class Done(val result: ShareImportResult) : ShareImportUiState
    data class Error(val message: String) : ShareImportUiState
}
