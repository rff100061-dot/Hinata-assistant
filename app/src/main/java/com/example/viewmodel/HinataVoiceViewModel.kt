package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CallStatus
import com.example.model.CallUiState
import com.example.service.GeminiHindiService
import com.example.service.VoiceCallManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HinataVoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val geminiService = GeminiHindiService()
    private var voiceCallManager: VoiceCallManager? = null
    private var callTimerJob: Job? = null
    private var hasSpokenInitialGreeting = false

    fun initVoiceCall(hasMicPermission: Boolean) {
        _uiState.update { it.copy(isMicPermissionGranted = hasMicPermission) }
        if (!hasMicPermission) return

        if (voiceCallManager == null) {
            voiceCallManager = VoiceCallManager(
                context = getApplication(),
                scope = viewModelScope,
                onSpeechRecognized = { userHindi ->
                    handleUserHindiSpeech(userHindi)
                },
                onAudioAmplitudeChanged = { amp ->
                    _uiState.update { it.copy(audioLevel = amp) }
                },
                onSpeakingStateChanged = { speaking, mouthOpen ->
                    _uiState.update {
                        it.copy(
                            status = if (speaking) CallStatus.SPEAKING else CallStatus.LISTENING,
                            visemeMouthOpen = mouthOpen
                        )
                    }
                },
                onStatusChanged = { statusText ->
                    _uiState.update { it.copy(statusTextHindi = statusText) }
                }
            )
        }

        startCallSession()
    }

    private fun startCallSession() {
        startTimer()
        _uiState.update {
            it.copy(
                status = CallStatus.SPEAKING,
                statusTextHindi = "जुड़ रहा है…",
                durationSeconds = 0L
            )
        }

        // Hinata gives her sweet opening voice greeting in Hindi
        if (!hasSpokenInitialGreeting) {
            hasSpokenInitialGreeting = true
            viewModelScope.launch {
                delay(600)
                val initialGreeting = "नमस्ते… मुझे बहुत खुशी हुई कि आपने मुझे कॉल किया। आप कैसे हैं?"
                _uiState.update {
                    it.copy(
                        status = CallStatus.SPEAKING,
                        hinataHindiReply = initialGreeting,
                        statusTextHindi = "बोल रही हूँ…"
                    )
                }
                voiceCallManager?.speakHindi(initialGreeting)
            }
        } else {
            voiceCallManager?.startListening()
        }
    }

    private fun startTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update {
                    if (it.status != CallStatus.ENDED) {
                        it.copy(durationSeconds = it.durationSeconds + 1)
                    } else it
                }
            }
        }
    }

    private fun handleUserHindiSpeech(userSpeech: String) {
        if (_uiState.value.status == CallStatus.ENDED || _uiState.value.isMuted) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userHindiQuery = userSpeech,
                    status = CallStatus.THINKING,
                    statusTextHindi = "सोच रही हूँ…"
                )
            }

            val hinataResponse = geminiService.generateHinataHindiReply(userSpeech)

            if (_uiState.value.status != CallStatus.ENDED) {
                _uiState.update {
                    it.copy(
                        hinataHindiReply = hinataResponse,
                        status = CallStatus.SPEAKING,
                        statusTextHindi = "बोल रही हूँ…"
                    )
                }
                voiceCallManager?.speakHindi(hinataResponse)
            }
        }
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = newMuted) }
        voiceCallManager?.setMuted(newMuted)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_uiState.value.isSpeakerOn
        _uiState.update { it.copy(isSpeakerOn = newSpeaker) }
        voiceCallManager?.setSpeakerOn(newSpeaker)
    }

    fun endCall() {
        callTimerJob?.cancel()
        voiceCallManager?.endCall()
        _uiState.update {
            it.copy(
                status = CallStatus.ENDED,
                statusTextHindi = "कॉल समाप्त",
                visemeMouthOpen = 0f,
                audioLevel = 0f
            )
        }
    }

    fun callAgain() {
        geminiService.clearHistory()
        hasSpokenInitialGreeting = false
        _uiState.update {
            it.copy(
                status = CallStatus.CONNECTING,
                durationSeconds = 0L,
                isMuted = false,
                statusTextHindi = "जुड़ रहा है…"
            )
        }
        voiceCallManager?.restartCall()
        startCallSession()
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        voiceCallManager?.release()
    }
}
