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
    
    fun startAdvertising(userId: String, userName: String) {
        if (isAdvertising) return
        
        this.userId = userId
        Log.d(TAG, "Starting advertising as: $userName")
        isAdvertising = true
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .build()
        
        connectionsClient.startAdvertising(userName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                Log.d(TAG, "Advertising started successfully")
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
        val payload = Payload.fromBytes(message.toByteArray())
        connectionsClient.sendPayload(endpointId, payload)
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
        
        // Convert base64 audio data back to bytes and play
        try {
            val audioBytes = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT)
            Log.d(TAG, "🔊 Decoded audio: ${audioBytes.size} bytes")
            playAudioData(audioBytes)
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
        
        // Create audio connection with the peer
        webRTCManager?.createPeerConnection(endpointId)
        
        // Enable audio streaming
        webRTCManager?.setMuted(false)
        
        // Send connection confirmation
        sendMessage(endpointId, "AUDIO_CONNECTED:${userId ?: "unknown"}")
        
        // Notify UI that audio is active
        Log.i(TAG, "✅ Ses iletişimi aktif: $endpointId")
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
