package com.intercomapp.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.intercomapp.IntercomApplication
import com.intercomapp.R
import com.intercomapp.communication.VoiceCommand
import com.intercomapp.communication.VoiceCommandManager
import com.intercomapp.communication.WebRTCManager
import com.intercomapp.communication.ConnectionManager
import com.intercomapp.data.repository.AuthRepository
import kotlinx.coroutines.*

class IntercomService : Service() {
    
    lateinit var webRTCManager: WebRTCManager
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var authRepository: AuthRepository
    private lateinit var connectionManager: ConnectionManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val binder = IntercomBinder()
    
    private var isConnected = false
    private var isMuted = false
    private var isMusicPlaying = false
    
    companion object {
        private const val TAG = "IntercomService"
        private const val NOTIFICATION_ID = 1001
        private const val FOREGROUND_SERVICE_ID = 1002
    }
    
    inner class IntercomBinder : Binder() {
        fun getService(): IntercomService = this@IntercomService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IntercomService created")
        
        // Initialize dependencies manually (Hilt disabled)
        webRTCManager = WebRTCManager()
        voiceCommandManager = VoiceCommandManager(this)
        authRepository = AuthRepository()
        connectionManager = ConnectionManager(this)
        
        // Initialize managers
        webRTCManager.initialize(this)
        voiceCommandManager.initialize()
        
        // Connect to signaling server
        webRTCManager.connectToSignalingServer()
        
        Log.i(TAG, "✅ Ses iletişimi için WebRTC aktif")
        
        // Start voice command listening
        startVoiceCommandListening()
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "IntercomService started")
        
        when (intent?.action) {
            "START_FOREGROUND" -> startForegroundService()
            "STOP_FOREGROUND" -> stopForegroundService()
            "CONNECT" -> connect()
            "DISCONNECT" -> disconnect()
            "MUTE" -> toggleMute()
            "START_MUSIC" -> startMusic()
            "STOP_MUSIC" -> stopMusic()
        }
        
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(FOREGROUND_SERVICE_ID, notification)
        Log.d(TAG, "Started foreground service")
    }
    
    private fun stopForegroundService() {
        stopForeground(true)
        Log.d(TAG, "Stopped foreground service")
    }
    
    private fun createNotification(): Notification {
        val channelId = IntercomApplication.CHANNEL_INTERCOM
        
        val intent = Intent(this, IntercomService::class.java).apply {
            action = "STOP_FOREGROUND"
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val status = when {
            isConnected -> "Bağlandı"
            isMuted -> "Susturuldu"
            else -> "Hazır"
        }
        
        return NotificationCompat.Builder(this, IntercomApplication.CHANNEL_INTERCOM)
            .setContentTitle("Intercom Servisi")
            .setContentText("Durum: $status")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Durdur", pendingIntent)
            .build()
    }
    
    private fun startVoiceCommandListening() {
        serviceScope.launch {
            voiceCommandManager.commandDetected.collect { command ->
                command?.let { handleVoiceCommand(it) }
            }
        }
        
        voiceCommandManager.startListening()
    }
    
    private fun handleVoiceCommand(command: VoiceCommand) {
        Log.d(TAG, "Handling voice command: $command")
        
        when (command) {
            VoiceCommand.CONNECT -> connect()
            VoiceCommand.DISCONNECT -> disconnect()
            VoiceCommand.MUTE -> toggleMute()
            VoiceCommand.UNMUTE -> toggleMute()
            VoiceCommand.START_MUSIC -> startMusic()
            VoiceCommand.STOP_MUSIC -> stopMusic()
            VoiceCommand.MUSIC -> startMusic()
            VoiceCommand.VOLUME_UP -> adjustVolume(true)
            VoiceCommand.VOLUME_DOWN -> adjustVolume(false)
            VoiceCommand.HELP -> showHelp()
        }
        
        // Clear the command after handling
        voiceCommandManager.clearCommand()
    }
    
                    fun connect() {
                    if (isConnected) {
                        Log.d(TAG, "Already connected")
                        return
                    }

                    serviceScope.launch {
                        try {
                            // Start discovery and advertising
                            connectionManager.startDiscovery()
                            connectionManager.startAdvertising(
                                authRepository.currentUser?.uid ?: "unknown",
                                authRepository.currentUser?.displayName ?: "Unknown User"
                            )

                            // Update user status
                            val currentUser = authRepository.currentUser
                            if (currentUser != null) {
                                authRepository.updateUserStatus(
                                    currentUser.uid,
                                    "ONLINE",
                                    true
                                )
                            }

                            isConnected = true
                            updateNotification()

                            Log.d(TAG, "Connected successfully")
                            Log.i(TAG, "✅ Bağlantı başarıyla kuruldu")

                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to connect", e)
                            Log.e(TAG, "❌ Bağlantı hatası: ${e.message}")
                        }
                    }
                }
    
    fun disconnect() {
        if (!isConnected) {
            Log.d(TAG, "Not connected")
            return
        }
        
        serviceScope.launch {
            try {
                // Stop discovery and advertising
                connectionManager.stopDiscovery()
                connectionManager.stopAdvertising()
                connectionManager.disconnectFromAllPeers()
                
                // Disconnect all WebRTC connections
                webRTCManager.disconnectAll()
                
                // Update user status
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    authRepository.updateUserStatus(
                        currentUser.uid,
                        "OFFLINE",
                        false
                    )
                }
                
                isConnected = false
                updateNotification()
                
                Log.d(TAG, "Disconnected successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
            }
        }
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        webRTCManager.setMuted(isMuted)
        updateNotification()
        
        Log.d(TAG, if (isMuted) "Muted" else "Unmuted")
    }
    
    private fun startMusic() {
        if (isMusicPlaying) {
            Log.d(TAG, "Music already playing")
            return
        }
        
        // Implement music sharing functionality
        isMusicPlaying = true
        updateNotification()
        
        Log.d(TAG, "Music started")
    }
    
    private fun stopMusic() {
        if (!isMusicPlaying) {
            Log.d(TAG, "Music not playing")
            return
        }
        
        isMusicPlaying = false
        updateNotification()
        
        Log.d(TAG, "Music stopped")
    }
    
    private fun adjustVolume(increase: Boolean) {
        // Implement volume adjustment
        Log.d(TAG, if (increase) "Volume increased" else "Volume decreased")
    }
    
    private fun showHelp() {
        // Show available voice commands
        Log.d(TAG, "Available commands: bağlan, kapat, sustur, müzik başlat, müziği durdur, ses aç, ses kıs")
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_SERVICE_ID, notification)
    }
    
    fun connectToSpecificUser(targetUserId: String) {
        Log.d(TAG, "Attempting to connect to specific user: $targetUserId")
        
        serviceScope.launch {
            try {
                // Start advertising our own ID
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    connectionManager.startAdvertising(
                        currentUser.uid,
                        currentUser.displayName ?: "Unknown User"
                    )
                    
                    // Start discovery to find target user
                    connectionManager.startDiscovery()
                    
                    // Set connection status to true when attempting to connect
                    isConnected = true
                    updateNotification()
                    
                    // Store target user ID for connection
                    // In a real implementation, you'd use a signaling server
                    // For now, we'll use Nearby Connections discovery
                    
                    Log.d(TAG, "Searching for user: ${targetUserId.take(8)}...")
                    Log.d(TAG, "Started discovery for user: $targetUserId")
                } else {
                    Log.e(TAG, "No current user found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to specific user", e)
            }
        }
    }
    
    // Public methods for activity to call
    fun getConnectionStatus(): Boolean = isConnected
    fun getMuteStatus(): Boolean = isMuted
    fun getMusicStatus(): Boolean = isMusicPlaying
    
    fun setMuted(muted: Boolean) {
        isMuted = muted
        webRTCManager.setMuted(muted)
        updateNotification()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IntercomService destroyed")
        
        serviceScope.cancel()
        voiceCommandManager.release()
        webRTCManager.release()
    }
}
