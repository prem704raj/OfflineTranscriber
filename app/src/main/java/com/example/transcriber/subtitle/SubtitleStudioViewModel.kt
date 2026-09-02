package com.example.transcriber.subtitle

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.caption.edit.CaptionTimingAnalyzer
import com.example.transcriber.caption.edit.CaptionTimingIssue
import com.example.transcriber.caption.export.CaptionExportRequestFactory
import com.example.transcriber.caption.export.background.CaptionExportJobState
import com.example.transcriber.caption.export.background.CaptionExportJobStatus
import com.example.transcriber.caption.export.background.CaptionExportRequestCodec
import com.example.transcriber.caption.export.background.CaptionExportStarter
import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionExportResolution
import com.example.transcriber.caption.model.CaptionPreset
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.style.CaptionPresetFactory
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.repository.TranscriptRepository
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.playback.PlayerPositionTicker
import com.example.transcriber.playback.SegmentTimelineIndex
import com.example.transcriber.transcription.AudioProcessor
import com.example.transcriber.transcription.ModelManager
import com.example.transcriber.transcription.WhisperEngine
import com.example.transcriber.ui.caption.CaptionToolPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class SubtitleStudioUiState(
    val transcript: TranscriptEntity? = null,
    val segments: List<TranscriptSegment> = emptyList(),
    val cues: List<CaptionCue> = emptyList(),
    val cueRevision: Long = 0L,
    val style: CaptionStyle = CaptionPresetFactory.style(CaptionPreset.CLASSIC),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val activeIndex: Int = -1,
    val timingIssues: List<CaptionTimingIssue> = emptyList(),
    val hasSpeakerData: Boolean = false,
    val selectedPanel: CaptionToolPanel = CaptionToolPanel.NONE,
    val exportResolution: CaptionExportResolution = CaptionExportResolution.ORIGINAL,
    val exportJob: CaptionExportJobState? = null,
    val entitlement: Entitlement = Entitlement.FREE,
    val errorMessage: String? = null,
    val message: String? = null
)

class SubtitleStudioViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val transcriptId: Long = checkNotNull(savedStateHandle["transcriptId"])
    private val initialSeekMs: Long = savedStateHandle["seekMs"] ?: 0L
    private var initialSeekConsumed = false

    private val repository = TranscriptRepository(
        context = application,
        modelManager = ModelManager(application),
        whisperEngine = WhisperEngine(application),
        audioProcessor = AudioProcessor(application)
    )

    private val knowledgeRepository = app.knowledgeRepository
    private val projectProvider = app.captionProjectProvider
    private val jobStore = app.captionExportJobStore
    private val workspace = app.captionExportWorkspace
    private val splitUseCase = app.splitCaptionUseCase
    private val mergeUseCase = app.safeMergeCaptionUseCase

    val bookmarkedSegmentIds: StateFlow<List<Long>> = knowledgeRepository
        .observeBookmarkedSegmentIds(transcriptId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(
        SubtitleStudioUiState(
            entitlement = app.billingRepository.state.value.entitlement
        )
    )
    val uiState = _uiState.asStateFlow()

    private var loadedSource: String? = null
    private var timelineIndex = SegmentTimelineIndex.fromSegments(emptyList())

    private val positionTicker = PlayerPositionTicker(
        scope = viewModelScope,
        player = player,
        intervalMs = 250L
    ) { position, duration ->
        val active = timelineIndex.activeIndex(position)
        _uiState.value = _uiState.value.copy(
            positionMs = position,
            durationMs = duration,
            activeIndex = active,
            isPlaying = player.isPlaying
        )
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) positionTicker.start() else positionTicker.stop()
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            positionTicker.publishNow()
        }
    }

    init {
        player.addListener(listener)

        // Observe billing entitlement
        viewModelScope.launch {
            app.entitlementRepository.entitlement.collect { entitlement ->
                _uiState.value = _uiState.value.copy(entitlement = entitlement)
            }
        }

        // Observe export jobs
        viewModelScope.launch {
            jobStore.jobs.collect {
                val latest = jobStore.latestForTranscript(transcriptId)
                _uiState.value = _uiState.value.copy(exportJob = latest)
            }
        }

        // Observe transcript and segments
        viewModelScope.launch {
            combine(
                repository.observeTranscript(transcriptId),
                repository.observeSegments(transcriptId)
            ) { transcript, segments -> transcript to segments }
                .collect { (transcript, segments) ->
                    timelineIndex = SegmentTimelineIndex.fromSegments(segments)

                    if (transcript != null) {
                        try {
                            val snapshot = projectProvider.load(
                                transcriptId = transcriptId,
                                includeSpeakers = true
                            )
                            val issues = CaptionTimingAnalyzer.analyze(snapshot.cues)
                            val hasSpeakers = snapshot.cues.any { !it.speakerLabel.isNullOrBlank() }

                            _uiState.value = _uiState.value.copy(
                                transcript = transcript,
                                segments = segments,
                                cues = snapshot.cues,
                                cueRevision = _uiState.value.cueRevision + 1L,
                                timingIssues = issues,
                                hasSpeakerData = hasSpeakers,
                                activeIndex = timelineIndex.activeIndex(player.currentPosition.coerceAtLeast(0L))
                            )
                        } catch (_: Exception) {
                            _uiState.value = _uiState.value.copy(
                                transcript = transcript,
                                segments = segments,
                                activeIndex = timelineIndex.activeIndex(player.currentPosition.coerceAtLeast(0L))
                            )
                        }
                    }

                    val source = transcript?.sourceUri?.takeIf { it.isNotBlank() }
                    if (source != null && source != loadedSource) {
                        loadedSource = source
                        player.setMediaItem(MediaItem.fromUri(source))
                        player.prepare()
                        if (!initialSeekConsumed && initialSeekMs > 0L) {
                            initialSeekConsumed = true
                            player.seekTo(initialSeekMs)
                        }
                    }
                    positionTicker.publishNow()
                }
        }
    }

    fun togglePlayback() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0L))
        positionTicker.publishNow()
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    fun toggleBookmark(segmentId: Long) {
        viewModelScope.launch {
            knowledgeRepository.toggleBookmark(
                transcriptId = transcriptId,
                segmentId = segmentId
            )
        }
    }

    fun saveSegment(
        segmentId: Long,
        text: String,
        startMs: Long,
        endMs: Long
    ) {
        val segments = _uiState.value.segments
        val index = segments.indexOfFirst { it.id == segmentId }
        if (index < 0) {
            showError("Subtitle no longer exists.")
            return
        }

        val validation = SubtitleTimingValidator.validate(
            startMs,
            endMs,
            segments.getOrNull(index - 1)?.endMs,
            segments.getOrNull(index + 1)?.startMs
        )
        if (!validation.valid) {
            showError(validation.message ?: "Invalid subtitle timing.")
            return
        }
        if (text.isBlank()) {
            showError("Subtitle text can't be empty.")
            return
        }

        viewModelScope.launch {
            repository.updateSegment(
                transcriptId = transcriptId,
                segmentId = segmentId,
                text = text.trim(),
                startMs = startMs,
                endMs = endMs
            )
        }
    }

    fun selectPreset(preset: CaptionPreset) {
        _uiState.value = _uiState.value.copy(
            style = CaptionPresetFactory.style(preset)
        )
    }

    fun setStyle(style: CaptionStyle) {
        _uiState.value = _uiState.value.copy(style = style)
    }

    fun updateStyle(transform: (CaptionStyle) -> CaptionStyle) {
        _uiState.value = _uiState.value.copy(
            style = transform(_uiState.value.style)
        )
    }

    fun setSelectedPanel(panel: CaptionToolPanel) {
        _uiState.value = _uiState.value.copy(selectedPanel = panel)
    }

    fun setExportResolution(resolution: CaptionExportResolution) {
        _uiState.value = _uiState.value.copy(exportResolution = resolution)
    }

    fun splitSegment(segmentId: Long, splitIndex: Int) {
        viewModelScope.launch {
            val result = splitUseCase.split(
                transcriptId = transcriptId,
                segmentId = segmentId,
                splitIndex = splitIndex
            )
            if (result.isFailure) {
                showError(result.exceptionOrNull()?.message ?: "Failed to split caption segment.")
            } else {
                _uiState.value = _uiState.value.copy(message = "Caption split into 2 segments.")
            }
        }
    }

    fun mergeWithNext(currentSegmentId: Long, nextSegmentId: Long) {
        viewModelScope.launch {
            val result = mergeUseCase.merge(
                transcriptId = transcriptId,
                currentSegmentId = currentSegmentId,
                nextSegmentId = nextSegmentId
            )
            if (result.isFailure) {
                showError(result.exceptionOrNull()?.message ?: "Failed to merge caption segments.")
            } else {
                _uiState.value = _uiState.value.copy(message = "Caption merged with next.")
            }
        }
    }

    fun startExport() {
        val state = _uiState.value
        val transcript = state.transcript ?: return
        if (state.cues.isEmpty()) {
            showError("No valid captions available to export.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = projectProvider.load(transcriptId, state.style.includeSpeakerLabel)
                val request = CaptionExportRequestFactory.create(
                    project = snapshot,
                    style = state.style,
                    resolution = state.exportResolution,
                    title = transcript.title
                )

                val jobId = UUID.randomUUID().toString().replace("-", "")
                val workDir = workspace.workDir(jobId)
                val requestFile = File(workDir, "request.json")
                CaptionExportRequestCodec.write(requestFile, request)

                val jobState = CaptionExportJobState(
                    jobId = jobId,
                    transcriptId = transcriptId,
                    status = CaptionExportJobStatus.QUEUED,
                    progress = 0,
                    updatedAt = System.currentTimeMillis()
                )
                jobStore.put(jobState)

                CaptionExportStarter.start(app, jobId)
            } catch (e: Exception) {
                showError(e.message ?: "Failed to start export.")
            }
        }
    }

    fun cancelExport(jobId: String) {
        CaptionExportStarter.cancel(app, jobId)
    }

    fun deleteTempExport(jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            workspace.cleanupReady(jobId)
            jobStore.remove(jobId)
        }
    }

    fun saveExportedVideo(context: Context, destUri: Uri, jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val job = jobStore.get(jobId) ?: error("Export job not found.")
                val readyPath = job.readyFilePath ?: error("Exported file not found.")
                val readyFile = File(readyPath)
                if (!readyFile.exists()) error("Exported video file no longer exists.")

                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    readyFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                _uiState.value = _uiState.value.copy(message = "Video saved to device.")
            } catch (e: Exception) {
                showError("Failed to save video: ${e.message}")
            }
        }
    }

    fun shareExportedVideo(context: Context, jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val job = jobStore.get(jobId) ?: error("Export job not found.")
                val readyPath = job.readyFilePath ?: error("Exported file not found.")
                val readyFile = File(readyPath)
                if (!readyFile.exists()) error("Exported video file no longer exists.")

                val shareDir = workspace.shareDir()
                val sharedFile = File(shareDir, readyFile.name)
                readyFile.copyTo(sharedFile, overwrite = true)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    sharedFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share Captioned Video").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                showError("Failed to share video: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    override fun onCleared() {
        positionTicker.stop()
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }
}
