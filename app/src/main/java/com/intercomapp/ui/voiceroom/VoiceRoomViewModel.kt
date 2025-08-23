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
            message = "Oda bekleniyor...",
            color = R.color.warning
        )
        
        // Initialize participant count with just me
        val currentUserId = currentUser?.uid ?: "unknown"
        connectedParticipants.add(currentUserId)
        _participantCount.value = 1 // Start with just me
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
    
    fun playTestAudio() {
        intercomService?.audioManager?.playTestAudio()
        _message.value = "Test sesi çalınıyor..."
    }
    
    fun setOtherUserId(userId: String) {
        otherUserId = userId
        
        // Update connection status
        _connectionStatus.value = ConnectionStatus(
            message = "Odaya bağlanılıyor...",
            color = R.color.warning
        )
        
        // Create room or join room based on whether we're the first user
        intercomService?.let { service ->
            val currentUserId = authRepository.currentUser?.uid ?: "unknown"
            val currentRoomId = _roomId.value ?: ""
            
            // For now, we'll create a room and wait for others to join
            // In a real implementation, you'd check if room exists first
            createRoom(currentRoomId, currentUserId)
        }
        
        _message.value = "Oda oluşturuldu, diğer kullanıcılar bekleniyor..."
        
        // Start checking for real connection
        startConnectionCheck()
    }
    
    private fun createRoom(roomId: String, userId: String) {
        intercomService?.let { service ->
            val intent = android.content.Intent(service, IntercomService::class.java).apply {
                action = "CREATE_ROOM"
                putExtra("room_id", roomId)
                putExtra("user_id", userId)
            }
            service.startService(intent)
        }
    }
    
    private fun startAudioConnection() {
        // Audio connection is handled automatically by SimpleAudioManager
        // when someone joins the room
        _connectionStatus.value = ConnectionStatus(
            message = "Ses bağlantısı hazırlanıyor...",
            color = R.color.warning
        )
        
        _message.value = "Ses sistemi başlatıldı"
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
        if (!connectedParticipants.contains(joinedUserId)) {
            connectedParticipants.add(joinedUserId)
            _participantCount.value = connectedParticipants.size
            
            // Set other user info when someone actually joins
            _otherUserInfo.value = UserInfo(
                id = joinedUserId,
                name = "Katılımcı ${joinedUserId.take(8)}...",
                isMuted = false
            )
            
            _connectionStatus.value = ConnectionStatus(
                message = "Katılımcı odaya girdi - Ses bağlantısı kuruldu!",
                color = R.color.success
            )
            
            _message.value = "Odaya katılım: $joinedUserId - Artık konuşabilirsiniz!"
            
            // Audio connection is automatically established by SimpleAudioManager
        }
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
            service.setMuted(isMuted)
        }
        
        // Microphone status is handled automatically by SimpleAudioManager
        // No need to send separate messages
        
        // Show message
        _message.value = if (isMuted) "Mikrofon susturuldu" else "Mikrofon açıldı"
    }
    
    fun disconnect() {
        intercomService?.let { service ->
            // Disconnect audio manager
            service.disconnect()
            
            // Update connection status
            _connectionStatus.value = ConnectionStatus(
                message = "Bağlantı kesildi",
                color = R.color.error
            )
            
            _message.value = "Ses odasından ayrıldınız"
        }
    }
    
    private fun updateConnectionStatus() {
        // Update connection status based on current state
        intercomService?.let { service ->
            val isConnected = service.getAudioConnectionStatus()
            
            _connectionStatus.value = ConnectionStatus(
                message = if (isConnected) "Ses bağlantısı kuruldu" else "Bağlantı bekleniyor...",
                color = if (isConnected) R.color.success else R.color.warning
            )
        }
    }
    
    private fun startConnectionCheck() {
        // Check connection status every 2 seconds
        CoroutineScope(Dispatchers.Main).launch {
            var shouldContinue = true
            while (shouldContinue) {
                delay(2000)
                
                intercomService?.let { service ->
                    val isConnected = service.getAudioConnectionStatus()
                    
                    if (isConnected) {
                        _connectionStatus.value = ConnectionStatus(
                            message = "Ses bağlantısı kuruldu - Konuşabilirsiniz!",
                            color = R.color.success
                        )
                        shouldContinue = false
                    } else {
                        _connectionStatus.value = ConnectionStatus(
                            message = "Karşı taraf bekleniyor...",
                            color = R.color.warning
                        )
                    }
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
