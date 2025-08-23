package com.intercomapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    val isOnline: Boolean = false,
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val deviceToken: String = "",
    val settings: UserSettings = UserSettings()
)

enum class UserStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    AWAY
}

data class UserSettings(
    val audioQuality: AudioQuality = AudioQuality.MEDIUM,
    val noiseReduction: Boolean = true,
    val autoConnect: Boolean = false,
    val voiceCommands: Boolean = true,
    val notifications: Boolean = true,
    val volume: Int = 50
)

enum class AudioQuality {
    LOW,
    MEDIUM,
    HIGH
}
