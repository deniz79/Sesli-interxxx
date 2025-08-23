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
        
        // Initialize audio components
        initializeAudioComponents()
        
        Log.i(TAG, "✅ Basit ses iletişimi başarıyla başlatıldı")
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
                    
                    delay(20) // Increased delay for better performance
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
            Log.v(TAG, "Sending audio data: ${audioData.size} bytes to ${audioStreams.size} peers")
            
            // Convert audio data to base64 for transmission
            val audioDataBase64 = android.util.Base64.encodeToString(audioData, android.util.Base64.DEFAULT)
            val audioMessage = "AUDIO:$audioDataBase64"
            
            // Send audio data to each connected peer
            audioStreams.keys.forEach { peerId ->
                // Send through connection manager
                sendAudioMessage(peerId, audioMessage)
                Log.d(TAG, "Sending audio to peer: $peerId")
            }
        }
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
        Log.d(TAG, "Playing received audio: ${audioBytes.size} bytes")
        
        // Make sure AudioTrack is playing
        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack?.play()
        }
        
        // Play the received audio through AudioTrack
        val bytesWritten = audioTrack?.write(audioBytes, 0, audioBytes.size) ?: 0
        Log.i(TAG, "🎵 Alınan ses oynatılıyor: ${audioBytes.size} bytes, yazılan: $bytesWritten")
    }
}
