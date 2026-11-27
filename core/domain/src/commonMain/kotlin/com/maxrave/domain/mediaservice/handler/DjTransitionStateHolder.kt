package com.maxrave.domain.mediaservice.handler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DjTransitionState(
    val isTransitioning: Boolean = false,
    val progress: Float = 0f,
    val outgoingMediaId: String = "",
    val incomingMediaId: String = "",
    val djMode: Boolean = false,
)

/**
 * Publishes real-time DJ crossfade transition progress (0f..1f) and track IDs
 * from the media player adapter to the Compose UI.
 */
object DjTransitionStateHolder {
    private val _state = MutableStateFlow(DjTransitionState())
    val state: StateFlow<DjTransitionState> = _state.asStateFlow()

    fun start(outgoingMediaId: String, incomingMediaId: String, djMode: Boolean) {
        _state.value = DjTransitionState(
            isTransitioning = true,
            progress = 0f,
            outgoingMediaId = outgoingMediaId,
            incomingMediaId = incomingMediaId,
            djMode = djMode,
        )
    }

    fun updateProgress(progress: Float) {
        val current = _state.value
        if (current.isTransitioning) {
            _state.value = current.copy(progress = progress.coerceIn(0f, 1f))
        }
    }

    fun reset() {
        _state.value = DjTransitionState()
    }
}
