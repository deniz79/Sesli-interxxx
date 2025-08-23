package com.intercomapp.communication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingClient @Inject constructor() {
    
    private var webSocket: WebSocket? = null
    private var clientId: String? = null
    private var currentRoomId: String? = null
    private var isConnected = false
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Callbacks
    var onConnectionEstablished: ((String) -> Unit)? = null
    var onUserJoined: ((String, String) -> Unit)? = null
    var onUserLeft: ((String, String) -> Unit)? = null
    var onOffer: ((String, String, String) -> Unit)? = null
    var onAnswer: ((String, String, String) -> Unit)? = null
    var onIceCandidate: ((String, String, String) -> Unit)? = null
    var onRoomInfo: ((String, List<String>) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    
    companion object {
        private const val TAG = "SignalingClient"
        private const val SERVER_URL = "ws://10.0.2.2:3000" // Android emulator için
        // private const val SERVER_URL = "ws://192.168.1.100:3000" // Gerçek cihaz için
    }
    
    fun connect() {
        if (isConnected) return
        
        val request = Request.Builder()
            .url(SERVER_URL)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                handleMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Binary message received")
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                isConnected = false
                onDisconnected?.invoke()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                isConnected = false
                onDisconnected?.invoke()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isConnected = false
                onDisconnected?.invoke()
                
                // Yeniden bağlanma denemesi
                scope.launch {
                    kotlinx.coroutines.delay(5000)
                    connect()
                }
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        clientId = null
        currentRoomId = null
    }
    
    fun joinRoom(roomId: String) {
        if (!isConnected) {
            Log.w(TAG, "Not connected to signaling server")
            return
        }
        
        currentRoomId = roomId
        sendMessage(JSONObject().apply {
            put("type", "join-room")
            put("roomId", roomId)
        })
        
        Log.d(TAG, "Joined room: $roomId")
    }
    
    fun leaveRoom() {
        currentRoomId?.let { roomId ->
            sendMessage(JSONObject().apply {
                put("type", "leave-room")
                put("roomId", roomId)
            })
            currentRoomId = null
            Log.d(TAG, "Left room: $roomId")
        }
    }
    
    fun sendOffer(target: String, offer: String) {
        currentRoomId?.let { roomId ->
            sendMessage(JSONObject().apply {
                put("type", "offer")
                put("target", target)
                put("offer", offer)
                put("roomId", roomId)
            })
            Log.d(TAG, "Sent offer to: $target")
        }
    }
    
    fun sendAnswer(target: String, answer: String) {
        currentRoomId?.let { roomId ->
            sendMessage(JSONObject().apply {
                put("type", "answer")
                put("target", target)
                put("answer", answer)
                put("roomId", roomId)
            })
            Log.d(TAG, "Sent answer to: $target")
        }
    }
    
    fun sendIceCandidate(target: String, candidate: String) {
        currentRoomId?.let { roomId ->
            sendMessage(JSONObject().apply {
                put("type", "ice-candidate")
                put("target", target)
                put("candidate", candidate)
                put("roomId", roomId)
            })
            Log.d(TAG, "Sent ICE candidate to: $target")
        }
    }
    
    fun ping() {
        sendMessage(JSONObject().apply {
            put("type", "ping")
        })
    }
    
    private fun sendMessage(json: JSONObject) {
        if (!isConnected) {
            Log.w(TAG, "Not connected to signaling server")
            return
        }
        
        val message = json.toString()
        webSocket?.send(message)
        Log.d(TAG, "Sent message: $message")
    }
    
    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")
            
            when (type) {
                "connection" -> {
                    clientId = json.getString("clientId")
                    Log.d(TAG, "Connection established with client ID: $clientId")
                    onConnectionEstablished?.invoke(clientId!!)
                }
                
                "user-joined" -> {
                    val userId = json.getString("clientId")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "User joined: $userId in room: $roomId")
                    onUserJoined?.invoke(userId, roomId)
                }
                
                "user-left" -> {
                    val userId = json.getString("clientId")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "User left: $userId from room: $roomId")
                    onUserLeft?.invoke(userId, roomId)
                }
                
                "user-disconnected" -> {
                    val userId = json.getString("clientId")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "User disconnected: $userId from room: $roomId")
                    onUserLeft?.invoke(userId, roomId)
                }
                
                "room-info" -> {
                    val roomId = json.getString("roomId")
                    val membersArray = json.getJSONArray("members")
                    val members = mutableListOf<String>()
                    for (i in 0 until membersArray.length()) {
                        members.add(membersArray.getString(i))
                    }
                    Log.d(TAG, "Room info: $roomId with ${members.size} members")
                    onRoomInfo?.invoke(roomId, members)
                }
                
                "offer" -> {
                    val from = json.getString("from")
                    val offer = json.getString("offer")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "Received offer from: $from")
                    onOffer?.invoke(from, offer, roomId)
                }
                
                "answer" -> {
                    val from = json.getString("from")
                    val answer = json.getString("answer")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "Received answer from: $from")
                    onAnswer?.invoke(from, answer, roomId)
                }
                
                "ice-candidate" -> {
                    val from = json.getString("from")
                    val candidate = json.getString("candidate")
                    val roomId = json.getString("roomId")
                    Log.d(TAG, "Received ICE candidate from: $from")
                    onIceCandidate?.invoke(from, candidate, roomId)
                }
                
                "pong" -> {
                    Log.d(TAG, "Received pong")
                }
                
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    fun getClientId(): String? = clientId
    
    fun isConnected(): Boolean = isConnected
    
    fun getCurrentRoomId(): String? = currentRoomId
}
