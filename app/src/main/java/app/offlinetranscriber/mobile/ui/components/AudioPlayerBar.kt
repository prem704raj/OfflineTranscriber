package app.offlinetranscriber.mobile.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.theme.OtPlaybackTimeStyle
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.design.AppShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AudioPlayerState(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying by mutableStateOf(false)
        private set
    var currentPositionMs by mutableIntStateOf(0)
        private set
    var durationMs by mutableIntStateOf(0)
        private set
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set

    fun initialize(audioUriString: String?) {
        release()
        if (audioUriString.isNullOrBlank()) return

        try {
            val player = MediaPlayer()
            if (audioUriString.startsWith("asset://")) {
                val assetName = audioUriString.removePrefix("asset://")
                val afd = context.assets.openFd(assetName)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                player.setDataSource(context, Uri.parse(audioUriString))
            }
            player.prepare()
            durationMs = player.duration
            player.setOnCompletionListener {
                isPlaying = false
                currentPositionMs = 0
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioPlayerState", "Failed to initialize player for $audioUriString", e)
        }
    }

    fun togglePlay() {
        val player = mediaPlayer ?: return
        if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.start()
            isPlaying = true
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val pos = positionMs.toInt().coerceIn(0, durationMs.coerceAtLeast(1))
        player.seekTo(pos)
        currentPositionMs = pos
    }

    fun skip(deltaMs: Int) {
        val target = (currentPositionMs + deltaMs).coerceIn(0, durationMs.coerceAtLeast(1))
        seekTo(target.toLong())
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        mediaPlayer?.let { player ->
            try {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                Log.e("AudioPlayerState", "Failed to set playback speed", e)
            }
        }
    }

    fun updateProgress() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                currentPositionMs = player.currentPosition
            }
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayerState", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
            currentPositionMs = 0
            durationMs = 0
        }
    }
}

@Composable
fun rememberAudioPlayerState(audioUriString: String?): AudioPlayerState {
    val context = LocalContext.current
    val state = remember(audioUriString) { AudioPlayerState(context) }

    DisposableEffect(audioUriString) {
        state.initialize(audioUriString)
        onDispose {
            state.release()
        }
    }

    LaunchedEffect(state.isPlaying) {
        while (isActive && state.isPlaying) {
            state.updateProgress()
            delay(100)
        }
    }

    return state
}

@Composable
fun AudioPlayerBar(
    state: AudioPlayerState,
    modifier: Modifier = Modifier
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Slider & Time with Tabular Typography
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TranscriptSegment.formatTime(state.currentPositionMs.toLong()),
                        style = OtPlaybackTimeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = state.currentPositionMs.toFloat()
                            .coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { state.seekTo(it.toLong()) },
                        valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = TranscriptSegment.formatTime(state.durationMs.toLong()),
                        style = OtPlaybackTimeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback speed dropdown
                    Box {
                        TextButton(
                            onClick = { speedMenuExpanded = true },
                            shape = AppShapes.Control
                        ) {
                            Text(
                                text = "${state.playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        state.setSpeed(speed)
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Center playback controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { state.skip(-10000) },
                            modifier = Modifier.accessibleAction("Rewind 10 seconds")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledIconButton(
                            onClick = { state.togglePlay() },
                            modifier = Modifier
                                .size(48.dp)
                                .accessibleAction(if (state.isPlaying) "Pause" else "Play"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { state.skip(10000) },
                            modifier = Modifier.accessibleAction("Forward 10 seconds")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Placeholder for visual balance
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
