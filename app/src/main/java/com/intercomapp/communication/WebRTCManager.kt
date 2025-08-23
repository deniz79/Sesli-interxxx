package com.intercomapp.communication

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class WebRTCManager {
    
    companion object {
        private const val TAG = "WebRTCManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
    }
    
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    
    private var context: Context? = null
    private var audioManager: AudioManager? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false
    private var isMuted = false
    private var audioJob: Job? = null
    private val audioStreams = ConcurrentHashMap<String, AudioStream>()
    private var connectionManager: ConnectionManager? = null
    
    data class AudioStream(
        val peerId: String,
        val isActive: Boolean = false
    )
    
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Simple Audio Manager")
        this.context = context
        
        // Setup audio manager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = true
        audioManager?.isMicrophoneMute = false
        
        // Set volume to maximum for testing
        audioManager?.let { am ->
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        }
        
        // Initialize audio components
        initializeAudioComponents()
        
        Log.i(TAG, "✅ Basit ses iletişimi başarıyla başlatıldı")
        Log.i(TAG, "🔊 Ses seviyesi maksimuma ayarlandı (test için)")
    }
    
    fun setConnectionManager(connectionManager: ConnectionManager) {
        this.connectionManager = connectionManager
        Log.d(TAG, "Connection manager set for audio communication")
    }
    
    private fun initializeAudioComponents() {
        try {
            // Initialize AudioRecord for recording
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            
            // Initialize AudioTrack for playback
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(android.media.AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            Log.d(TAG, "Audio components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio components", e)
        }
    }
    
    fun createPeerConnection(peerId: String): Any? {
        Log.d(TAG, "Creating audio connection for: $peerId")
        
        // Create audio stream for this peer
        audioStreams[peerId] = AudioStream(peerId, true)
        
        // Start audio streaming if not already started
        if (!isRecording) {
            startAudioStreaming()
        }
        
        Log.i(TAG, "✅ Ses bağlantısı oluşturuldu: $peerId")
        return peerId
    }
    
    private fun startAudioStreaming() {
        if (isRecording) return
        
        audioJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                isRecording = true
                isPlaying = true
                
                val buffer = ByteArray(BUFFER_SIZE)
                
                // Start recording and playing
                audioRecord?.startRecording()
                audioTrack?.play()
                
                Log.i(TAG, "🎵 Ses akışı başlatıldı - Recording: ${audioRecord?.state}, Playing: ${audioTrack?.playState}")
                
                while (isRecording && isActive) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0 && !isMuted) {
                        // Send audio data to connected peers
                        sendAudioToPeers(buffer.copyOf(bytesRead))
                        Log.v(TAG, "Ses gönderildi: $bytesRead bytes")
                    }
                    
                    delay(10) // Reduced delay for better real-time performance
                }
                
                audioRecord?.stop()
                audioTrack?.stop()
                
                Log.d(TAG, "Audio streaming stopped")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio streaming", e)
            } finally {
                isRecording = false
                isPlaying = false
            }
        }
    }
    
    private fun sendAudioToPeers(audioData: ByteArray) {
        // Send audio data to all connected peers via ConnectionManager
        if (audioStreams.isNotEmpty()) {
            Log.d(TAG, "🎤 Sending audio data: ${audioData.size} bytes to ${audioStreams.size} peers")
            
            // Convert audio data to base64 for transmission
            val audioDataBase64 = android.util.Base64.encodeToString(audioData, android.util.Base64.DEFAULT)
            val audioMessage = "AUDIO:$audioDataBase64"
            
            // Send audio data to each connected peer
            audioStreams.keys.forEach { peerId ->
                // Send through connection manager
                sendAudioMessage(peerId, audioMessage)
                Log.d(TAG, "📤 Audio sent to peer: $peerId")
            }
        } else {
            Log.w(TAG, "⚠️ No audio streams available to send audio")
        }
    }
    
    // Add method to check if peer is connected
    fun isPeerConnected(peerId: String): Boolean {
        return audioStreams.containsKey(peerId)
    }
    
    private fun sendAudioMessage(peerId: String, audioMessage: String) {
        // Send audio message through connection manager
        connectionManager?.sendMessage(peerId, audioMessage)
        Log.d(TAG, "Audio message sent to $peerId: ${audioMessage.length} chars")
    }
    
    fun disconnect(peerId: String) {
        Log.d(TAG, "Disconnecting audio from peer: $peerId")
        audioStreams.remove(peerId)
        
        // If no more peers, stop audio streaming
        if (audioStreams.isEmpty()) {
            stopAudioStreaming()
        }
    }
    
    fun disconnectAll() {
        Log.d(TAG, "Disconnecting from all audio peers")
        audioStreams.clear()
        stopAudioStreaming()
    }
    
    private fun stopAudioStreaming() {
        isRecording = false
        isPlaying = false
        audioJob?.cancel()
        audioJob = null
        
        audioRecord?.stop()
        audioTrack?.stop()
        
        Log.d(TAG, "Audio streaming stopped")
    }
    
    fun release() {
        Log.d(TAG, "Releasing audio resources")
        disconnectAll()
        
        audioRecord?.release()
        audioTrack?.release()
        
        audioRecord = null
        audioTrack = null
    }
    
    fun setMuted(muted: Boolean) {
        Log.d(TAG, "Setting muted: $muted")
        isMuted = muted
        audioManager?.isMicrophoneMute = muted
    }
    
    fun setUserId(userId: String) {
        Log.d(TAG, "Setting user ID: $userId")
        
        // Save user ID for echo prevention
        context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("user_id", userId)
            apply()
        }
    }
    
    // Test audio function for debugging
    fun playTestAudio() {
        try {
            Log.i(TAG, "🔊 Test sesi çalınıyor...")
            
            // Create a simple test tone (440 Hz sine wave)
            val sampleRate = 44100
            val duration = 1.0 // 1 second
            val frequency = 440.0 // A4 note
            val numSamples = (sampleRate * duration).toInt()
            val audioData = ByteArray(numSamples * 2) // 16-bit samples
            
            for (i in 0 until numSamples) {
                val sample = (Math.sin(2 * Math.PI * frequency * i / sampleRate) * 32767).toInt().toShort()
                audioData[i * 2] = (sample.toInt() and 0xFF).toByte()
                audioData[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
            }
            
            // Play the test audio
            playReceivedAudio(audioData)
            Log.i(TAG, "✅ Test sesi çalındı")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test sesi çalınamadı", e)
        }
    }
    
    fun connectToSignalingServer() {
        Log.d(TAG, "Audio system ready for communication")
    }
    
    fun joinRoom(roomId: String) {
        Log.d(TAG, "Joining audio room: $roomId")
    }
    
    fun leaveRoom() {
        Log.d(TAG, "Leaving audio room")
    }
    
    fun handleRemoteDescription(peerId: String, sdp: String) {
        Log.d(TAG, "Handling remote description for: $peerId")
        // For simple audio, we just create the connection
        createPeerConnection(peerId)
    }
    
    fun handleIceCandidate(peerId: String, candidate: String) {
        Log.d(TAG, "Handling ICE candidate for: $peerId")
        // For simple audio, we don't need ICE candidates
    }
    
    fun playReceivedAudio(audioBytes: ByteArray) {
        try {
            Log.d(TAG, "Playing received audio: ${audioBytes.size} bytes")
            
            // Make sure AudioTrack is playing
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
                Log.d(TAG, "🎵 Audio track started playing")
            }
            
            // Play the received audio through AudioTrack
            val bytesWritten = audioTrack?.write(audioBytes, 0, audioBytes.size) ?: 0
            Log.i(TAG, "🎵 Alınan ses oynatılıyor: ${audioBytes.size} bytes, yazılan: $bytesWritten")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing received audio", e)
        }
    }
    
    // Add method to check if we should play audio (avoid echo)
    private fun shouldPlayAudio(fromPeerId: String): Boolean {
        // Don't play audio from ourselves to avoid echo
        val currentUserId = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)?.getString("user_id", "") ?: ""
        return fromPeerId != currentUserId
    }
}
