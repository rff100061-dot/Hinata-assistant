package com.example.model

enum class CallStatus {
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    ENDED
}

data class CallUiState(
    val status: CallStatus = CallStatus.CONNECTING,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val audioLevel: Float = 0f,
    val visemeMouthOpen: Float = 0f,
    val statusTextHindi: String = "जुड़ रहा है…",
    val userHindiQuery: String = "",
    val hinataHindiReply: String = "",
    val isMicPermissionGranted: Boolean = false,
    val audioFrequencies: List<Float> = listOf(0.4f, 0.6f, 0.8f, 0.5f, 0.7f, 0.6f, 0.9f, 0.4f)
)
