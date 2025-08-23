package com.intercomapp.ui.voiceroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.intercomapp.R
import com.intercomapp.data.repository.AuthRepository
import com.intercomapp.service.IntercomService
import com.intercomapp.communication.WebRTCManager

class VoiceRoomViewModel : ViewModel() {
    
    private var intercomService: IntercomService? = null
    private val authRepository = AuthRepository()
    
    // LiveData for UI updates
    private val _roomId = MutableLiveData<String>()
    val roomId: LiveData<String> = _roomId
    
    private val _myUserInfo = MutableLiveData<UserInfo>()
    val myUserInfo: LiveData<UserInfo> = _myUserInfo
    
    private val _otherUserInfo = MutableLiveData<UserInfo>()
    val otherUserInfo: LiveData<UserInfo> = _otherUserInfo
    
    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus
    
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    // State variables
    private var isMuted = false
    private var otherUserId: String? = null

    
    init {
        // Initialize with current user info
        val currentUser = authRepository.currentUser
        _myUserInfo.value = UserInfo(
            id = currentUser?.uid ?: "unknown",
            name = currentUser?.displayName ?: "Ben",
            isMuted = false
        )
        
        // Set initial connection status
        _connectionStatus.value = ConnectionStatus(
            message = "Arama bekleniyor...",
            color = R.color.warning
        )
    }
    
    fun setIntercomService(service: IntercomService?) {
        intercomService = service
        if (service != null) {
            _message.value = "Ses odası servisi bağlandı"
            updateConnectionStatus()
        } else {
            _message.value = "Ses odası servisi bulunamadı"
        }
    }
    
    fun setRoomId(roomId: String) {
        _roomId.value = roomId
    }
    
    fun setOtherUserId(userId: String) {
        otherUserId = userId
        _otherUserInfo.value = UserInfo(
            id = userId,
            name = "Kullanıcı ${userId.take(8)}...",
            isMuted = false
        )
        
        // Update connection status
        _connectionStatus.value = ConnectionStatus(
            message = "Ses odasına bağlanılıyor...",
            color = R.color.warning
        )
        
        // Start audio connection immediately
        startAudioConnection()
        
        _message.value = "Ses odasına bağlandı"
    }
    
    private fun startAudioConnection() {
        otherUserId?.let { userId ->
            intercomService?.let { service ->
                // Create WebRTC connection
                service.webRTCManager?.createPeerConnection(userId)
                
                // Update connection status to waiting
                _connectionStatus.value = ConnectionStatus(
                    message = "Karşı taraf bekleniyor...",
                    color = R.color.warning
                )
                
                _message.value = "Ses bağlantısı başlatıldı"
            }
        }
    }
    
    fun onAudioConnected() {
        // Called when the other party connects
        _connectionStatus.value = ConnectionStatus(
            message = "Ses bağlantısı kuruldu",
            color = R.color.success
        )
        
        _message.value = "Ses iletişimi aktif"
    }
    

    
    fun toggleMute() {
        isMuted = !isMuted
        
        // Update my user info
        _myUserInfo.value = _myUserInfo.value?.copy(isMuted = isMuted)
        
        // Update service
        intercomService?.let { service ->
            service.webRTCManager?.setMuted(isMuted)
            service.setMuted(isMuted)
        }
        
        // Show message
        _message.value = if (isMuted) "Mikrofon susturuldu" else "Mikrofon açıldı"
    }
    
    fun disconnect() {
        otherUserId?.let { userId ->
            intercomService?.let { service ->
                // Disconnect WebRTC
                service.webRTCManager?.disconnect(userId)
                
                // Update connection status
                _connectionStatus.value = ConnectionStatus(
                    message = "Bağlantı kesildi",
                    color = R.color.error
                )
                
                _message.value = "Ses odasından ayrıldınız"
            }
        }
    }
    
    private fun updateConnectionStatus() {
        _connectionStatus.value = ConnectionStatus(
            message = "Bağlantı kuruldu",
            color = R.color.success
        )
    }
    
    // Update other user's mute status (called when receiving status updates)
    fun updateOtherUserMuteStatus(isMuted: Boolean) {
        _otherUserInfo.value = _otherUserInfo.value?.copy(isMuted = isMuted)
    }
    
    // Data classes
    data class UserInfo(
        val id: String,
        val name: String,
        val isMuted: Boolean
    )
    
    data class ConnectionStatus(
        val message: String,
        val color: Int
    )
}
