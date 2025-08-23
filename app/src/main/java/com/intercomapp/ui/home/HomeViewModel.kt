package com.intercomapp.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.intercomapp.service.IntercomService
import com.intercomapp.data.repository.AuthRepository
// import dagger.hilt.android.lifecycle.HiltViewModel
// import javax.inject.Inject

// @HiltViewModel
class HomeViewModel : ViewModel() {
    
    private val _connectionState = MutableLiveData<Boolean>()
    val connectionState: LiveData<Boolean> = _connectionState
    
    private val _muteState = MutableLiveData<Boolean>()
    val muteState: LiveData<Boolean> = _muteState
    
    private val _musicState = MutableLiveData<Boolean>()
    val musicState: LiveData<Boolean> = _musicState
    
    private val _voiceCommandState = MutableLiveData<Boolean>()
    val voiceCommandState: LiveData<Boolean> = _voiceCommandState
    
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    private var intercomService: IntercomService? = null
    private val authRepository = AuthRepository()
    private var context: Context? = null
    
    fun setIntercomService(service: IntercomService?) {
        intercomService = service
        if (service != null) {
            _message.value = "Servis bağlantısı kuruldu"
            updateStates()
        } else {
            _message.value = "Servis bağlantısı bulunamadı"
        }
    }
    
    fun setContext(context: Context) {
        this.context = context
    }
    
    fun getUserId(): String? {
        return authRepository.currentUser?.uid
    }
    
    fun copyUserIdToClipboard() {
        context?.let { ctx ->
            val userId = getUserId()
            if (userId != null) {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("User ID", userId)
                clipboard.setPrimaryClip(clip)
                _message.value = "ID kopyalandı: ${userId.take(8)}..."
            } else {
                _message.value = "ID alınamadı"
            }
        }
    }
    
    fun createRoom(): String {
        val currentUserId = getUserId() ?: "unknown"
        val roomId = "ROOM_${currentUserId}_${System.currentTimeMillis()}"
        
        _message.value = "Oda oluşturuldu: ${roomId.take(12)}..."
        
        // Copy room ID to clipboard
        context?.let { ctx ->
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Room ID", roomId)
            clipboard.setPrimaryClip(clip)
        }
        
        return roomId
    }
    
    fun joinRoom(roomId: String): Boolean {
        if (roomId.isEmpty()) {
            _message.value = "Lütfen geçerli bir oda ID'si girin"
            return false
        }
        
        if (!roomId.startsWith("ROOM_")) {
            _message.value = "Geçersiz oda ID formatı"
            return false
        }
        
        _message.value = "Odaya katılınıyor: ${roomId.take(12)}..."
        return true
    }
    
    private fun updateStates() {
        intercomService?.let { service ->
            _connectionState.value = service.getConnectionStatus()
            _muteState.value = service.getMuteStatus()
            _musicState.value = service.getMusicStatus()
        }
    }
    
    fun toggleConnection() {
        intercomService?.let { service ->
            if (service.getConnectionStatus()) {
                // Disconnect
                service.disconnect()
                _message.value = "Bağlantı kesildi"
                _connectionState.value = false
            } else {
                // Connect
                service.connect()
                _message.value = "Bağlantı aranıyor..."
                _connectionState.value = true
            }
            updateStates()
        } ?: run {
            _message.value = "Servis bağlantısı bulunamadı"
        }
    }
    
    fun toggleMute() {
        intercomService?.let { service ->
            service.onStartCommand(null, 0, 0)
            updateStates()
            _message.value = if (service.getMuteStatus()) "Susturuldu" else "Susturma kaldırıldı"
        } ?: run {
            _message.value = "Servis bağlantısı bulunamadı"
        }
    }
    
    fun startMusic() {
        intercomService?.let { service ->
            service.onStartCommand(null, 0, 0)
            updateStates()
            _message.value = "Müzik başlatıldı"
        } ?: run {
            _message.value = "Servis bağlantısı bulunamadı"
        }
    }
    
    fun stopMusic() {
        intercomService?.let { service ->
            service.onStartCommand(null, 0, 0)
            updateStates()
            _message.value = "Müzik durduruldu"
        } ?: run {
            _message.value = "Servis bağlantısı bulunamadı"
        }
    }
    
    fun adjustVolume(increase: Boolean) {
        _message.value = if (increase) "Ses artırıldı" else "Ses azaltıldı"
    }
    
    fun toggleVoiceCommands() {
        _voiceCommandState.value = !(_voiceCommandState.value ?: false)
        _message.value = if (_voiceCommandState.value == true) {
            "Sesli komutlar aktif"
        } else {
            "Sesli komutlar devre dışı"
        }
    }
}
