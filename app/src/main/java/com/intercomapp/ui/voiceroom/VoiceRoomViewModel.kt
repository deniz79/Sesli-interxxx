package com.intercomapp.ui.voiceroom

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.intercomapp.R
import com.intercomapp.data.repository.AuthRepository
import com.intercomapp.service.IntercomService
import com.intercomapp.communication.WebRTCManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VoiceRoomViewModel : ViewModel() {
    
    private var intercomService: IntercomService? = null
    private val authRepository = AuthRepository()
    private var context: Context? = null
    
    // LiveData for UI updates
    private val _roomId = MutableLiveData<String>()
    val roomId: LiveData<String> = _roomId
    
    private val _myUserInfo = MutableLiveData<UserInfo>()
    val myUserInfo: LiveData<UserInfo> = _myUserInfo
    
    private val _otherUserInfo = MutableLiveData<UserInfo?>()
    val otherUserInfo: LiveData<UserInfo?> = _otherUserInfo
    
    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus
    
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    // State variables
    private var isMuted = false
    private var otherUserId: String? = null
    private var connectedParticipants = mutableSetOf<String>()
    
    private val _participantCount = MutableLiveData<Int>()
    val participantCount: LiveData<Int> = _participantCount

    
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
        
        // Initialize participant count with just me
        val currentUserId = currentUser?.uid ?: "unknown"
        connectedParticipants.add(currentUserId)
        _participantCount.value = connectedParticipants.size
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
    
    fun setContext(context: Context) {
        this.context = context
    }
    
    fun copyRoomIdToClipboard() {
        context?.let { ctx ->
            val roomId = _roomId.value
            if (roomId != null) {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Room ID", roomId)
                clipboard.setPrimaryClip(clip)
                _message.value = "Oda ID'si kopyalandı: ${roomId.take(15)}..."
            } else {
                _message.value = "Oda ID'si bulunamadı"
            }
        }
    }
    
    fun setOtherUserId(userId: String) {
        otherUserId = userId
        // Don't set other user info until someone actually joins
        // _otherUserInfo.value will remain null initially
        
        // Update connection status
        _connectionStatus.value = ConnectionStatus(
            message = "Ses odasına bağlanılıyor...",
            color = R.color.warning
        )
        
        // Start audio connection immediately
        startAudioConnection()
        
        // Send room join notification to other users in the room
        intercomService?.let { service ->
            val currentUserId = authRepository.currentUser?.uid ?: "unknown"
            val currentRoomId = _roomId.value ?: ""
            service.connectionManager?.sendMessage(currentRoomId, "ROOM_JOINED:$currentUserId")
        }
        
        _message.value = "Ses odasına bağlandı"
        
        // Start checking for real connection
        startConnectionCheck()
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
    
    fun onRoomJoined(peerId: String, joinedUserId: String) {
        // Called when someone joins the room
        connectedParticipants.add(joinedUserId)
        _participantCount.value = connectedParticipants.size
        
        // Set other user info when someone actually joins
        _otherUserInfo.value = UserInfo(
            id = joinedUserId,
            name = "Katılımcı ${joinedUserId.take(8)}...",
            isMuted = false
        )
        
        _connectionStatus.value = ConnectionStatus(
            message = "Katılımcı odaya girdi",
            color = R.color.success
        )
        
        _message.value = "Odaya katılım: $joinedUserId"
    }
    
    fun onParticipantLeft(userId: String) {
        // Called when someone leaves the room
        connectedParticipants.remove(userId)
        _participantCount.value = connectedParticipants.size
        
        // Hide other user if they left
        if (_otherUserInfo.value?.id == userId) {
            _otherUserInfo.value = null
        }
        
        _message.value = "Katılımcı ayrıldı: $userId"
    }
    
    fun onMicrophoneStatusChanged(peerId: String, isMuted: Boolean) {
        // Called when microphone status changes
        updateOtherUserMuteStatus(isMuted)
        _message.value = if (isMuted) "Karşı taraf mikrofonu kapattı" else "Karşı taraf mikrofonu açtı"
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
        
        // Send microphone status to other user
        otherUserId?.let { userId ->
            intercomService?.let { service ->
                val micStatus = if (isMuted) "MUTED" else "UNMUTED"
                service.connectionManager?.sendMessage(userId, "MIC_STATUS:$micStatus")
            }
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
        // Update connection status based on current state
        val isConnected = intercomService?.webRTCManager?.isPeerConnected(otherUserId ?: "") == true
        
        _connectionStatus.value = ConnectionStatus(
            message = if (isConnected) "Ses bağlantısı kuruldu" else "Bağlantı bekleniyor...",
            color = if (isConnected) R.color.success else R.color.warning
        )
    }
    
    private fun startConnectionCheck() {
        // Check connection status every 2 seconds
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(2000)
                
                val isConnected = intercomService?.webRTCManager?.isPeerConnected(otherUserId ?: "") == true
                
                if (isConnected) {
                    _connectionStatus.value = ConnectionStatus(
                        message = "Ses bağlantısı kuruldu",
                        color = R.color.success
                    )
                    break
                } else {
                    _connectionStatus.value = ConnectionStatus(
                        message = "Karşı taraf bekleniyor...",
                        color = R.color.warning
                    )
                }
            }
        }
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
