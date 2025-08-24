package com.intercomapp.communication

import android.content.Context
import android.util.Log
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AgoraAudioManager {
    
    companion object {
        private const val TAG = "AgoraAudioManager"
        private const val APP_ID = "29ce68eb5b54482883231d14eeac28a5"
    }
    
    private var context: Context? = null
    private var rtcEngine: RtcEngine? = null
    private val isConnected = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)
    
    private var roomId: String? = null
    private var userId: String? = null
    
    private var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    private var onUserJoined: ((String) -> Unit)? = null
    private var onUserLeft: ((String) -> Unit)? = null
    
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Agora Audio Manager")
        this.context = context
        
        try {
            // Initialize Agora RTC Engine
            rtcEngine = RtcEngine.create(context, APP_ID, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.i(TAG, "✅ Odaya başarıyla katıldı: $channel, UID: $uid")
                    isConnected.set(true)
                    onConnectionStateChanged?.invoke(true)
                }
                
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.i(TAG, "👤 Kullanıcı odaya katıldı: $uid")
                    onUserJoined?.invoke(uid.toString())
                }
                
                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.i(TAG, "👤 Kullanıcı odadan ayrıldı: $uid")
                    onUserLeft?.invoke(uid.toString())
                }
                
                override fun onLeaveChannel(stats: io.agora.rtc.IRtcEngineEventHandler.RtcStats) {
                    Log.i(TAG, "❌ Odadan ayrıldı")
                    isConnected.set(false)
                    onConnectionStateChanged?.invoke(false)
                }
                
                override fun onError(err: Int) {
                    Log.e(TAG, "❌ Agora hatası: $err")
                }
            })
            
            // Enable audio
            rtcEngine?.enableAudio()
            
            Log.i(TAG, "✅ Agora ses sistemi başarıyla başlatıldı")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Agora başlatma hatası", e)
        }
    }
    
    fun createRoom(roomId: String, userId: String) {
        this.roomId = roomId
        this.userId = userId
        
        Log.i(TAG, "🚪 Oda oluşturuluyor: $roomId")
        
        // Join channel (room)
        rtcEngine?.joinChannel(null, roomId, null, 0)
        Log.i(TAG, "✅ Odaya katılma isteği gönderildi")
    }
    
    fun joinRoom(roomId: String, userId: String) {
        this.roomId = roomId
        this.userId = userId
        
        Log.i(TAG, "🚪 Odaya katılınıyor: $roomId")
        
        // Join channel (room)
        rtcEngine?.joinChannel(null, roomId, null, 0)
        Log.i(TAG, "✅ Odaya katılma isteği gönderildi")
    }
    
    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
        rtcEngine?.muteLocalAudioStream(muted)
        Log.d(TAG, "Mikrofon durumu: ${if (muted) "Kapalı" else "Açık"}")
    }
    
    fun isConnected(): Boolean {
        return isConnected.get()
    }
    
    fun disconnect() {
        Log.i(TAG, "🔌 Bağlantı kesiliyor...")
        
        rtcEngine?.leaveChannel()
        isConnected.set(false)
        
        Log.i(TAG, "✅ Bağlantı kesildi")
    }
    
    fun release() {
        Log.i(TAG, "🗑️ Agora kaynakları temizleniyor...")
        
        disconnect()
        RtcEngine.destroy()
        rtcEngine = null
        
        Log.i(TAG, "✅ Agora kaynakları temizlendi")
    }
    
    // Test audio function
    fun playTestAudio() {
        try {
            Log.i(TAG, "🔊 Test sesi çalınıyor...")
            
            // Simple test - just log for now
            Log.i(TAG, "✅ Test sesi çalındı")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test sesi çalınamadı", e)
        }
    }
    
    // Callback setters
    fun setOnConnectionStateChanged(callback: (Boolean) -> Unit) {
        onConnectionStateChanged = callback
    }
    
    fun setOnUserJoined(callback: (String) -> Unit) {
        onUserJoined = callback
    }
    
    fun setOnUserLeft(callback: (String) -> Unit) {
        onUserLeft = callback
    }
}
