package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Ready : VoiceState()
    data class Listening(val partialText: String = "", val rmsDb: Float = 0f) : VoiceState()
    data class Success(val recognizedText: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isAvailable = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                _isAvailable.value = true
            } else {
                _isAvailable.value = false
            }
        } catch (e: Exception) {
            _isAvailable.value = false
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.Ready
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.Listening("", 0f)
            }

            override fun onRmsChanged(rmsdB: Float) {
                val current = _voiceState.value
                if (current is VoiceState.Listening) {
                    _voiceState.value = current.copy(rmsDb = rmsdB.coerceAtLeast(0f))
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Speech ended, awaiting final result
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 녹음 에러"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 연결을 확인해주세요."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과"
                    SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다. 다시 말씀해 주세요."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 사용 중입니다."
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류가 발생했습니다."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말씀이 없어 인식을 종료했습니다."
                    else -> "음성 인식 오류 ($error)"
                }
                _voiceState.value = VoiceState.Error(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _voiceState.value = VoiceState.Success(text)
                } else {
                    _voiceState.value = VoiceState.Error("인식된 내용이 없습니다.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                val current = _voiceState.value
                val rms = if (current is VoiceState.Listening) current.rmsDb else 0f
                _voiceState.value = VoiceState.Listening(partialText = text, rmsDb = rms)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        try {
            if (speechRecognizer == null) {
                initRecognizer()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            _voiceState.value = VoiceState.Listening("", 0f)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("음성 인식을 시작할 수 없습니다: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _voiceState.value = VoiceState.Idle
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Idle
        }
    }

    fun reset() {
        _voiceState.value = VoiceState.Idle
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
