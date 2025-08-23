package com.intercomapp.communication

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class SimpleAudioManager {
    
    companion object {
        private const val TAG = "SimpleAudioManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        private const val PORT = 8080
    }
    
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    
    private var context: Context? = null
    private var audioManager: AudioManager? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    
    private var isRecording = false
    private var isPlaying = false
    private var isMuted = false
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    
    private val isConnected = AtomicBoolean(false)
    private val isServer = AtomicBoolean(false)
    
    private var recordingJob: Job? = null
    private var playingJob: Job? = null
    private var connectionJob: Job? = null
    
    private var roomId: String? = null
    private var userId: String? = null
    
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Simple Audio Manager")
        this.context = context
        
        // Setup audio manager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = true
        audioManager?.isMicrophoneMute = false
        
        // Set volume to maximum
        audioManager?.let { am ->
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        }
        
        // Initialize audio components
        initializeAudioComponents()
        
        Log.i(TAG, "✅ Basit ses sistemi başarıyla başlatıldı")
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
    
    fun createRoom(roomId: String, userId: String) {
        this.roomId = roomId
        this.userId = userId
        isServer.set(true)
        
        Log.i(TAG, "🚪 Oda oluşturuluyor: $roomId")
        
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start server
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "✅ Sunucu başlatıldı, port: $PORT")
                
                // Wait for client connection
                val client = serverSocket?.accept()
                if (client != null) {
                    clientSocket = client
                    inputStream = DataInputStream(client.getInputStream())
                    outputStream = DataOutputStream(client.getOutputStream())
                    
                    isConnected.set(true)
                    Log.i(TAG, "✅ İstemci bağlandı!")
                    
                    // Start audio streaming
                    startAudioStreaming()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sunucu hatası", e)
            }
        }
    }
    
    fun joinRoom(roomId: String, userId: String, serverIp: String) {
        this.roomId = roomId
        this.userId = userId
        isServer.set(false)
        
        Log.i(TAG, "🚪 Odaya katılınıyor: $roomId")
        
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Connect to server
                clientSocket = Socket(serverIp, PORT)
                inputStream = DataInputStream(clientSocket?.getInputStream())
                outputStream = DataOutputStream(clientSocket?.getOutputStream())
                
                isConnected.set(true)
                Log.i(TAG, "✅ Sunucuya bağlandı!")
                
                // Start audio streaming
                startAudioStreaming()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Bağlantı hatası", e)
            }
        }
    }
    
    private fun startAudioStreaming() {
        if (isRecording || isPlaying) return
        
        Log.i(TAG, "🎵 Ses akışı başlatılıyor...")
        
        // Start recording
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                isRecording = true
                audioRecord?.startRecording()
                
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (isRecording && isConnected.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0 && !isMuted) {
                        // Send audio data
                        sendAudioData(buffer.copyOf(bytesRead))
                    }
                    
                    delay(10) // 10ms delay for real-time performance
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Kayıt hatası", e)
            } finally {
                isRecording = false
                audioRecord?.stop()
            }
        }
        
        // Start playing
        playingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                isPlaying = true
                audioTrack?.play()
                
                while (isPlaying && isConnected.get()) {
                    // Receive audio data
                    val audioData = receiveAudioData()
                    if (audioData != null) {
                        playAudioData(audioData)
                    }
                    
                    delay(10) // 10ms delay for real-time performance
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Oynatma hatası", e)
            } finally {
                isPlaying = false
                audioTrack?.stop()
            }
        }
    }
    
    private fun sendAudioData(audioData: ByteArray) {
        try {
            outputStream?.let { out ->
                out.writeInt(audioData.size)
                out.write(audioData)
                out.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ses gönderme hatası", e)
            isConnected.set(false)
        }
    }
    
    private fun receiveAudioData(): ByteArray? {
        return try {
            inputStream?.let { input ->
                val size = input.readInt()
                val data = ByteArray(size)
                input.readFully(data)
                data
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ses alma hatası", e)
            isConnected.set(false)
            null
        }
    }
    
    private fun playAudioData(audioData: ByteArray) {
        try {
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
            
            val bytesWritten = audioTrack?.write(audioData, 0, audioData.size) ?: 0
            Log.v(TAG, "🎵 Ses oynatıldı: $bytesWritten bytes")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ses oynatma hatası", e)
        }
    }
    
    fun setMuted(muted: Boolean) {
        isMuted = muted
        audioManager?.isMicrophoneMute = muted
        Log.d(TAG, "Mikrofon durumu: ${if (muted) "Kapalı" else "Açık"}")
    }
    
    fun isConnected(): Boolean {
        return isConnected.get()
    }
    
    fun disconnect() {
        Log.i(TAG, "🔌 Bağlantı kesiliyor...")
        
        isConnected.set(false)
        isRecording = false
        isPlaying = false
        
        recordingJob?.cancel()
        playingJob?.cancel()
        connectionJob?.cancel()
        
        try {
            clientSocket?.close()
            serverSocket?.close()
            inputStream?.close()
            outputStream?.close()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bağlantı kapatma hatası", e)
        }
        
        audioRecord?.stop()
        audioTrack?.stop()
        
        Log.i(TAG, "✅ Bağlantı kesildi")
    }
    
    fun release() {
        disconnect()
        
        audioRecord?.release()
        audioTrack?.release()
        
        audioRecord = null
        audioTrack = null
    }
    
    // Test audio function
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
            
            playAudioData(audioData)
            Log.i(TAG, "✅ Test sesi çalındı")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test sesi çalınamadı", e)
        }
    }
}
