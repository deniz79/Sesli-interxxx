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
            discoveredPeers[endpointId] = Endpoint(endpointId, info.endpointName)
        }
        
        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Lost endpoint: $endpointId")
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
    
    private fun handleMessage(endpointId: String, message: String) {
        Log.d(TAG, "Received message from $endpointId: $message")
        // Handle different message types
        when {
            message.startsWith("AUDIO:") -> {
                // Handle audio data
                val audioData = message.substring(6)
                // Process audio data
            }
            message.startsWith("TEXT:") -> {
                // Handle text message
                val text = message.substring(5)
                // Process text message
            }
            else -> {
                // Handle other message types
            }
        }
    }
    
    private fun handleAudioStream(endpointId: String, stream: Payload.Stream) {
        Log.d(TAG, "Received audio stream from: $endpointId")
        // Process audio stream
    }
    
    fun release() {
        stopDiscovery()
        stopAdvertising()
        disconnectFromAllPeers()
        connectionsClient.stopAllEndpoints()
    }
    
    data class Endpoint(val id: String, val name: String)
}
