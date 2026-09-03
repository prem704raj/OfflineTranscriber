package app.offlinetranscriber.mobile.export

enum class ExportProgressState {
    IDLE,
    GENERATING,
    SAVING,
    SHARING,
    PRINTING,
    SUCCESS,
    ERROR
}
