package com.intercomapp.communication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandManager @Inject constructor(
    private val context: Context
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private val _isListening = MutableStateFlow(false)
    val isListeningState: StateFlow<Boolean> = _isListening
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText
    
    private val _commandDetected = MutableStateFlow<VoiceCommand?>(null)
    val commandDetected: StateFlow<VoiceCommand?> = _commandDetected
    
    companion object {
        private const val TAG = "VoiceCommandManager"
        
        // Voice commands
        private val VOICE_COMMANDS = mapOf(
            "bağlan" to VoiceCommand.CONNECT,
            "kapat" to VoiceCommand.DISCONNECT,
            "müzik" to VoiceCommand.MUSIC,
            "müzik başlat" to VoiceCommand.START_MUSIC,
            "müziği durdur" to VoiceCommand.STOP_MUSIC,
            "ses aç" to VoiceCommand.VOLUME_UP,
            "ses kıs" to VoiceCommand.VOLUME_DOWN,
            "sustur" to VoiceCommand.MUTE,
            "susturmayı kaldır" to VoiceCommand.UNMUTE,
            "yardım" to VoiceCommand.HELP
        )
    }
    
    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupSpeechRecognizer()
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
        }
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _isListening.value = true
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Handle volume changes
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Handle audio buffer
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                _isListening.value = false
            }
            
            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                _isListening.value = false
                
                // Restart listening after error
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Restart listening
                        startListening()
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0].lowercase(Locale.getDefault())
                    _recognizedText.value = recognizedText
                    
                    Log.d(TAG, "Recognized text: $recognizedText")
                    
                    // Check for voice commands
                    val command = detectCommand(recognizedText)
                    if (command != null) {
                        _commandDetected.value = command
                        Log.d(TAG, "Voice command detected: $command")
                    }
                }
                
                // Restart listening for continuous recognition
                startListening()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial results
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle events
            }
        })
    }
    
    private fun detectCommand(text: String): VoiceCommand? {
        for ((commandText, command) in VOICE_COMMANDS) {
            if (text.contains(commandText, ignoreCase = true)) {
                return command
            }
        }
        return null
    }
    
    fun startListening() {
        if (speechRecognizer == null || isListening) return
        
        try {
            // API key kontrolü - şimdilik devre dışı
            Log.w(TAG, "Google Speech API devre dışı. Sesli komutlar kullanılamıyor.")
            return
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Turkish language
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }
    
    fun stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            _isListening.value = false
        }
    }
    
    fun clearCommand() {
        _commandDetected.value = null
    }
    
    fun release() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

enum class VoiceCommand {
    CONNECT,
    DISCONNECT,
    MUSIC,
    START_MUSIC,
    STOP_MUSIC,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    UNMUTE,
    HELP
}
