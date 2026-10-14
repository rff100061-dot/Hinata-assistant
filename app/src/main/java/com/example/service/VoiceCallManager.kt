package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class VoiceCallManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeechRecognized: (String) -> Unit,
    private val onAudioAmplitudeChanged: (Float) -> Unit,
    private val onSpeakingStateChanged: (Boolean, Float) -> Unit, // isSpeaking, mouthOpenRatio
    private val onStatusChanged: (String) -> Unit
) {
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isTtsReady = false
    private var isCallActive = true
    private var isMuted = false
    private var isSpeaking = false
    private var mouthAnimationJob: Job? = null

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale.forLanguageTag("hi-IN")
                val langResult = textToSpeech?.setLanguage(hindiLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Hindi not supported on default TTS, trying generic Hindi")
                    textToSpeech?.setLanguage(Locale.forLanguageTag("hi"))
                }
                // Hinata sweet, gentle anime feminine voice styling
                textToSpeech?.setPitch(1.18f)
                textToSpeech?.setSpeechRate(0.92f)

                // Select female Hindi voice if available
                try {
                    val voices = textToSpeech?.voices
                    val femaleHindiVoice = voices?.firstOrNull {
                        it.locale.language == "hi" &&
                                (it.name.contains("female", ignoreCase = true) || it.name.contains("f0", ignoreCase = true))
                    } ?: voices?.firstOrNull { it.locale.language == "hi" }
                    if (femaleHindiVoice != null) {
                        textToSpeech?.voice = femaleHindiVoice
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Voice selection fallback", e)
                }

                isTtsReady = true
                Log.d(TAG, "TTS initialized successfully for Hindi")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                startVisemeLipSyncAnimation()
                mainHandler.post {
                    onStatusChanged("बोल रही हूँ…")
                }
            }

            override fun onDone(utteranceId: String?) {
                stopVisemeLipSyncAnimation()
                isSpeaking = false
                mainHandler.post {
                    onSpeakingStateChanged(false, 0f)
                    if (isCallActive && !isMuted) {
                        startListening()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                stopVisemeLipSyncAnimation()
                isSpeaking = false
                mainHandler.post {
                    onSpeakingStateChanged(false, 0f)
                    if (isCallActive && !isMuted) {
                        startListening()
                    }
                }
            }
        })
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on device")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isCallActive && !isSpeaking && !isMuted) {
                        onStatusChanged("सुन रही हूँ…")
                    }
                }

                override fun onBeginningOfSpeech() {
                    onStatusChanged("सुन रही हूँ…")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize RMS to 0.0 - 1.0
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    onAudioAmplitudeChanged(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onStatusChanged("सोच रही हूँ…")
                }

                override fun onError(error: Int) {
                    Log.d(TAG, "SpeechRecognizer error: $error")
                    // If error is timeout or no match, resume listening smoothly if call is active
                    if (isCallActive && !isSpeaking && !isMuted) {
                        mainHandler.postDelayed({
                            if (isCallActive && !isSpeaking && !isMuted) {
                                startListening()
                            }
                        }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim()
                    if (!spokenText.isNullOrBlank()) {
                        Log.d(TAG, "Recognized user Hindi speech: $spokenText")
                        onSpeechRecognized(spokenText)
                    } else {
                        if (isCallActive && !isSpeaking && !isMuted) {
                            startListening()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (!isCallActive || isSpeaking || isMuted) return
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
                onStatusChanged("सुन रही हूँ…")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    fun speakHindi(text: String) {
        if (!isCallActive) return
        stopListening()
        mainHandler.post {
            val utteranceId = UUID.randomUUID().toString()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun startVisemeLipSyncAnimation() {
        mouthAnimationJob?.cancel()
        mouthAnimationJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && isSpeaking) {
                // Syllabic speech cadence modulation
                // Vowels and consonants naturally pulse between 0.15 and 0.95
                val oscillation = (Math.sin(step * 0.45) * 0.5 + 0.5).toFloat()
                val flutter = (Math.sin(step * 1.2) * 0.25).toFloat()
                val mouthOpenRatio = (oscillation * 0.75f + flutter).coerceIn(0.1f, 0.95f)

                // Also generate audio visualizer amplitude
                val amplitude = (mouthOpenRatio * 0.85f + 0.15f).coerceIn(0.1f, 1f)

                withContext(Dispatchers.Main) {
                    onSpeakingStateChanged(true, mouthOpenRatio)
                    onAudioAmplitudeChanged(amplitude)
                }
                delay(65)
                step++
            }
        }
    }

    private fun stopVisemeLipSyncAnimation() {
        mouthAnimationJob?.cancel()
        mouthAnimationJob = null
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopListening()
            onStatusChanged("माइक बंद")
        } else {
            if (isCallActive && !isSpeaking) {
                startListening()
            }
        }
    }

    fun setSpeakerOn(speakerOn: Boolean) {
        try {
            audioManager?.isSpeakerphoneOn = speakerOn
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle speakerphone", e)
        }
    }

    fun endCall() {
        isCallActive = false
        stopListening()
        textToSpeech?.stop()
        stopVisemeLipSyncAnimation()
    }

    fun restartCall() {
        isCallActive = true
        isMuted = false
        isSpeaking = false
        startListening()
    }

    fun release() {
        isCallActive = false
        stopListening()
        mouthAnimationJob?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        private const val TAG = "VoiceCallManager"
    }
}
