package com.Mohammad.Elahi.terpsichore.ui.preview

import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PREVIEW_CAP_MS = 30_000
private const val POLL_INTERVAL_MS = 250L

data class PreviewState(
    val key: String? = null,
    val loading: Boolean = false,
    val playing: Boolean = false,
    val progress: Float = 0f
)

class PreviewPlayer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var pollJob: Job? = null
    private var prepareJob: Job? = null
    private var preparedDurationMs: Int = 0

    fun toggle(key: String, urlProvider: suspend () -> String?) {
        val current = _state.value
        if (current.key == key && (current.loading || current.playing)) {
            stopPlayback()
            return
        }
        stopPlayback()
        _state.value = PreviewState(key = key, loading = true)
        prepareJob = scope.launch {
            val url = try {
                urlProvider()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (url == null) {
                resetStateIf(key)
                return@launch
            }
            if (_state.value.key != key) return@launch
            prepare(key, url)
        }
    }

    fun release() {
        stopPlayback()
    }

    private fun prepare(key: String, url: String) {
        val player = MediaPlayer().apply {
            setOnErrorListener { _, _, _ ->
                resetStateIf(key)
                true
            }
            setOnCompletionListener {
                resetStateIf(key)
            }
            setOnPreparedListener { mp ->
                preparedDurationMs = mp.duration
                _state.update { state ->
                    if (state.key == key) state.copy(loading = false, playing = true) else state
                }
                mp.start()
                startPolling(key)
            }
        }
        try {
            player.setDataSource(url)
            player.prepareAsync()
            mediaPlayer = player
        } catch (_: Exception) {
            player.release()
            mediaPlayer = null
            resetStateIf(key)
        }
    }

    private fun startPolling(key: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val player = mediaPlayer ?: break
                val position = try {
                    player.currentPosition
                } catch (_: IllegalStateException) {
                    break
                }
                if (position >= PREVIEW_CAP_MS) {
                    stopPlayback()
                    break
                }
                val denominator =
                    if (preparedDurationMs > 0) minOf(
                        preparedDurationMs,
                        PREVIEW_CAP_MS
                    ) else PREVIEW_CAP_MS
                _state.update { state ->
                    if (state.key == key) {
                        state.copy(progress = (position.toFloat() / denominator).coerceIn(0f, 1f))
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun stopPlayback() {
        prepareJob?.cancel()
        prepareJob = null
        pollJob?.cancel()
        pollJob = null
        mediaPlayer?.release()
        mediaPlayer = null
        preparedDurationMs = 0
        _state.value = PreviewState()
    }

    private fun resetStateIf(key: String) {
        if (_state.value.key == key) stopPlayback()
    }
}
