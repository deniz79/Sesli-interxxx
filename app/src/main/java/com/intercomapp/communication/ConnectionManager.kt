package com.intercomapp.communication

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ConnectionManager"
        private const val SERVICE_ID = "com.intercomapp.intercom"
    }
    
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val discoveredPeers = ConcurrentHashMap<String, Endpoint>()
    private val connectedPeers = ConcurrentHashMap<String, Endpoint>()
    
    // Add mapping for room ID to endpoint ID
    private val roomToEndpointMapping = ConcurrentHashMap<String, String>()
    private val endpointToRoomMapping = ConcurrentHashMap<String, String>()
    
    private var isDiscovering = false
    private var isAdvertising = false
    private var userId: String? = null
    private var webRTCManager: WebRTCManager? = null
    private var callCallback: ((String, String) -> Unit)? = null
    
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: $endpointId")
            
            // Accept the connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        when (result.status.statusCode) {
            ConnectionsStatusCodes.STATUS_OK -> {
                Log.d(TAG, "Connection successful with: $endpointId")
                connectedPeers[endpointId] = discoveredPeers[endpointId] ?: return
                
                // Map endpoint ID to room ID (assuming room ID is in endpoint name)
                val endpoint = discoveredPeers[endpointId]
                val roomId = endpoint?.name?.substringAfter("ROOM_") ?: ""
                if (roomId.isNotEmpty()) {
                    roomToEndpointMapping[roomId] = endpointId
                    endpointToRoomMapping[endpointId] = roomId
                    Log.d(TAG, "Mapped room $roomId to endpoint $endpointId")
                }
                
                // Notify that connection is established
                Log.i(TAG, "✅ Bağlantı başarıyla kuruldu: $endpointId")
                
                // Start WebRTC audio connection
                startWebRTCAudioConnection(endpointId)
            }
            ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                Log.w(TAG, "Connection rejected by: $endpointId")
                Log.w(TAG, "❌ Bağlantı reddedildi: $endpointId")
            }
            ConnectionsStatusCodes.STATUS_ERROR -> {
                Log.e(TAG, "Connection failed with: $endpointId")
                Log.e(TAG, "❌ Bağlantı hatası: $endpointId")
            }
        }
    }
        
        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            connectedPeers.remove(endpointId)
            // Notify that peer disconnected
        }
    }
    
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Found endpoint: $endpointId")
            Log.i(TAG, "🔍 Cihaz bulundu: $endpointId (${info.endpointName})")
            discoveredPeers[endpointId] = Endpoint(endpointId, info.endpointName)
        }
        
        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Lost endpoint: $endpointId")
            Log.w(TAG, "❌ Cihaz kayboldu: $endpointId")
            discoveredPeers.remove(endpointId)
        }
    }
    
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = payload.asBytes()
                    data?.let { handleMessage(endpointId, String(it)) }
                }
                Payload.Type.STREAM -> {
                    // Handle audio stream
                    val stream = payload.asStream()
                    stream?.let { handleAudioStream(endpointId, it) }
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handle transfer updates
        }
    }
    
    fun startDiscovery() {
        if (isDiscovering) return
        
        Log.d(TAG, "Starting discovery")
        isDiscovering = true
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .build()
        
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Discovery started successfully")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to start discovery", exception)
                isDiscovering = false
            }
    }
    
    fun stopDiscovery() {
        if (!isDiscovering) return
        
        Log.d(TAG, "Stopping discovery")
        connectionsClient.stopDiscovery()
        isDiscovering = false
        discoveredPeers.clear()
    }
    
    fun startAdvertising(userId: String, userName: String, roomId: String? = null) {
        if (isAdvertising) return
        
        this.userId = userId
        val advertisingName = if (roomId != null) "${userName}ROOM_${roomId}" else userName
        Log.d(TAG, "Starting advertising as: $advertisingName")
        isAdvertising = true
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .build()
        
        connectionsClient.startAdvertising(advertisingName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Advertising started successfully as: $advertisingName")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to start advertising", exception)
                isAdvertising = false
            }
    }
    
    fun stopAdvertising() {
        if (!isAdvertising) return
        
        Log.d(TAG, "Stopping advertising")
        connectionsClient.stopAdvertising()
        isAdvertising = false
    }
    
    fun connectToPeer(endpointId: String) {
        Log.d(TAG, "Connecting to peer: $endpointId")
        
        val connectionOptions = ConnectionOptions.Builder()
            .build()
        
        connectionsClient.requestConnection(userId ?: "Unknown", endpointId, connectionLifecycleCallback, connectionOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Connection request sent to: $endpointId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to request connection to: $endpointId", exception)
            }
    }
    
    fun disconnectFromPeer(endpointId: String) {
        Log.d(TAG, "Disconnecting from peer: $endpointId")
        connectionsClient.disconnectFromEndpoint(endpointId)
        connectedPeers.remove(endpointId)
    }
    
    fun disconnectFromAllPeers() {
        Log.d(TAG, "Disconnecting from all peers")
        connectedPeers.keys.forEach { endpointId ->
            connectionsClient.disconnectFromEndpoint(endpointId)
        }
        connectedPeers.clear()
    }
    
    fun sendMessage(endpointId: String, message: String) {
        try {
            val payload = Payload.fromBytes(message.toByteArray())
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "📤 Message sent to $endpointId: $message")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send message to $endpointId", e)
        }
    }
    
    // Add method to send message by room ID
    fun sendMessageByRoomId(roomId: String, message: String) {
        val endpointId = roomToEndpointMapping[roomId]
        if (endpointId != null) {
            sendMessage(endpointId, message)
            Log.d(TAG, "📤 Message sent to room $roomId via endpoint $endpointId: $message")
        } else {
            Log.w(TAG, "⚠️ No endpoint found for room $roomId")
        }
    }
    
    // Add method to get endpoint ID from room ID
    fun getEndpointIdForRoom(roomId: String): String? {
        return roomToEndpointMapping[roomId]
    }
    
    fun sendAudioStream(endpointId: String, audioData: ByteArray) {
        val payload = Payload.fromBytes(audioData)
        connectionsClient.sendPayload(endpointId, payload)
    }
    
    fun getDiscoveredPeers(): List<Endpoint> {
        return discoveredPeers.values.toList()
    }
    
    fun getConnectedPeers(): List<Endpoint> {
        return connectedPeers.values.toList()
    }
    
    fun setWebRTCManager(webRTCManager: WebRTCManager) {
        this.webRTCManager = webRTCManager
        Log.d(TAG, "WebRTC manager set for audio communication")
    }
    
    fun setCallCallback(callback: (String, String) -> Unit) {
        this.callCallback = callback
        Log.d(TAG, "Call callback set")
    }
    
    private fun handleMessage(endpointId: String, message: String) {
        Log.d(TAG, "Received message from $endpointId: $message")
        // Handle different message types
        when {
            message.startsWith("AUDIO:") -> {
                // Handle audio data
                val audioData = message.substring(6)
                handleAudioData(endpointId, audioData)
            }
            message.startsWith("TEXT:") -> {
                // Handle text message
                val text = message.substring(5)
                // Process text message
            }
            message.startsWith("WEBRTC_") -> {
                // Handle WebRTC messages
                handleWebRTCMessage(endpointId, message)
            }
            message.startsWith("CALL_") -> {
                // Handle call messages
                handleCallMessage(endpointId, message)
            }
            message.startsWith("AUDIO_CONNECTED:") -> {
                // Handle audio connection confirmation
                val connectedUserId = message.substringAfter("AUDIO_CONNECTED:")
                Log.i(TAG, "✅ Ses bağlantısı onaylandı: $connectedUserId")
                // Notify callback about audio connection
                callCallback?.invoke("AUDIO_CONNECTED", endpointId)
            }
            message.startsWith("ROOM_JOINED:") -> {
                // Handle room join notification
                val joinedUserId = message.substringAfter("ROOM_JOINED:")
                Log.i(TAG, "🚪 Odaya katılım: $joinedUserId")
                // Notify callback about room join
                callCallback?.invoke("ROOM_JOINED", "$endpointId:$joinedUserId")
            }
            message.startsWith("MIC_STATUS:") -> {
                // Handle microphone status update
                val micStatus = message.substringAfter("MIC_STATUS:")
                val isMuted = micStatus == "MUTED"
                Log.i(TAG, "🎤 Mikrofon durumu güncellendi: ${if (isMuted) "Kapalı" else "Açık"}")
                // Notify callback about microphone status
                callCallback?.invoke("MIC_STATUS", "$endpointId:$isMuted")
            }
            else -> {
                // Handle other message types
            }
        }
    }
    
    private fun handleAudioData(endpointId: String, audioData: String) {
        Log.d(TAG, "📥 Received audio data from $endpointId: ${audioData.length} chars")
        
        // Check if this audio is from ourselves to avoid echo
        val currentUserId = userId ?: ""
        val senderEndpoint = connectedPeers[endpointId]
        val senderUserId = senderEndpoint?.name?.substringBefore("ROOM_") ?: ""
        
        if (senderUserId == currentUserId) {
            Log.d(TAG, "🔄 Ignoring own audio to prevent echo")
            return
        }
        
        // Convert base64 audio data back to bytes and play
        try {
            val audioBytes = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
            Log.d(TAG, "🔊 Decoded audio: ${audioBytes.size} bytes from $senderUserId")
            
            // Play audio immediately in a separate coroutine to avoid blocking
            CoroutineScope(Dispatchers.IO).launch {
                playAudioData(audioBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to decode audio data", e)
        }
    }
    
    private fun playAudioData(audioBytes: ByteArray) {
        // Play received audio data
        Log.d(TAG, "Playing audio data: ${audioBytes.size} bytes")
        
        // Play the audio through the WebRTC manager
        webRTCManager?.playReceivedAudio(audioBytes)
        Log.i(TAG, "🎵 Ses verisi oynatılıyor: ${audioBytes.size} bytes")
    }
    
    private fun handleAudioStream(endpointId: String, stream: Payload.Stream) {
        Log.d(TAG, "Received audio stream from: $endpointId")
        // Process audio stream
    }
    
    private fun startWebRTCAudioConnection(endpointId: String) {
        Log.d(TAG, "Starting WebRTC audio connection with: $endpointId")
        Log.i(TAG, "🎵 WebRTC ses bağlantısı başlatılıyor: $endpointId")
        
        // Send WebRTC ready message
        sendWebRTCOffer(endpointId)
    }
    
    private fun sendWebRTCOffer(endpointId: String) {
        Log.d(TAG, "Sending WebRTC offer to: $endpointId")
        
        // Send a simple message indicating WebRTC is ready
        val message = "WEBRTC_READY:${userId ?: "unknown"}"
        sendMessage(endpointId, message)
    }
    
    private fun handleWebRTCMessage(endpointId: String, message: String) {
        Log.d(TAG, "Handling WebRTC message from $endpointId: $message")
        
        when {
            message.startsWith("WEBRTC_READY:") -> {
                Log.i(TAG, "✅ WebRTC hazır mesajı alındı: $endpointId")
                // Both peers are ready for WebRTC audio
                startAudioStreaming(endpointId)
            }
            message.startsWith("SDP_OFFER:") -> {
                // Handle SDP offer
                val sdpData = message.substringAfter("SDP_OFFER:")
                Log.d(TAG, "Received SDP offer from $endpointId")
            }
            message.startsWith("SDP_ANSWER:") -> {
                // Handle SDP answer
                val sdpData = message.substringAfter("SDP_ANSWER:")
                Log.d(TAG, "Received SDP answer from $endpointId")
            }
            message.startsWith("ICE_CANDIDATE:") -> {
                // Handle ICE candidate
                val candidateData = message.substringAfter("ICE_CANDIDATE:")
                Log.d(TAG, "Received ICE candidate from $endpointId")
            }
        }
    }
    
    private fun startAudioStreaming(endpointId: String) {
        Log.i(TAG, "🎵 Ses akışı başlatılıyor: $endpointId")
        
        // Get room ID for this endpoint
        val roomId = endpointToRoomMapping[endpointId] ?: ""
        
        // Create audio connection with the peer
        if (roomId.isNotEmpty()) {
            webRTCManager?.createPeerConnectionByRoomId(roomId, endpointId)
        } else {
            webRTCManager?.createPeerConnection(endpointId)
        }
        
        // Enable audio streaming
        webRTCManager?.setMuted(false)
        
        // Send connection confirmation
        sendMessage(endpointId, "AUDIO_CONNECTED:${userId ?: "unknown"}")
        
        // Notify UI that audio is active
        Log.i(TAG, "✅ Ses iletişimi aktif: $endpointId (Room: $roomId)")
    }
    
    private fun handleCallMessage(endpointId: String, message: String) {
        Log.d(TAG, "Handling call message from $endpointId: $message")
        
        when {
            message.startsWith("CALL_REQUEST:") -> {
                val callerId = message.substringAfter("CALL_REQUEST:")
                Log.i(TAG, "📞 Arama isteği alındı: $callerId")
                // Show incoming call notification
                showIncomingCallNotification(endpointId, callerId)
            }
            message.startsWith("CALL_ACCEPTED:") -> {
                val accepterId = message.substringAfter("CALL_ACCEPTED:")
                Log.i(TAG, "✅ Arama kabul edildi: $accepterId")
                
                // Notify callback
                callCallback?.invoke("CALL_ACCEPTED", endpointId)
                
                // Send acceptance confirmation
                sendMessage(endpointId, "CALL_CONFIRMED:${userId ?: "unknown"}")
            }
            message.startsWith("CALL_REJECTED:") -> {
                val rejecterId = message.substringAfter("CALL_REJECTED:")
                Log.i(TAG, "❌ Arama reddedildi: $rejecterId")
                // Handle call rejection
            }
            message.startsWith("CALL_CONFIRMED:") -> {
                val confirmerId = message.substringAfter("CALL_CONFIRMED:")
                Log.i(TAG, "✅ Arama onaylandı: $confirmerId")
                
                // Notify callback
                callCallback?.invoke("CALL_CONFIRMED", endpointId)
                
                // Both parties confirmed, start audio connection
                startWebRTCAudioConnection(endpointId)
            }
        }
    }
    
    private fun showIncomingCallNotification(endpointId: String, callerId: String) {
        // This would show an incoming call notification
        Log.i(TAG, "📞 Gelen arama: $callerId")
        // In a real implementation, this would show a notification or dialog
    }
    
    fun release() {
        stopDiscovery()
        stopAdvertising()
        disconnectFromAllPeers()
        connectionsClient.stopAllEndpoints()
    }
    
    data class Endpoint(val id: String, val name: String)
}
