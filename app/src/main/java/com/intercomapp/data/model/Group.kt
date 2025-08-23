package com.intercomapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Group(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val members: List<String> = emptyList(),
    val admins: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val settings: GroupSettings = GroupSettings()
)

data class GroupSettings(
    val maxMembers: Int = 10,
    val allowInvites: Boolean = true,
    val requireApproval: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.MEDIUM,
    val allowMusicSharing: Boolean = true
)

data class GroupMember(
    val userId: String = "",
    val joinedAt: Timestamp = Timestamp.now(),
    val isAdmin: Boolean = false,
    val isMuted: Boolean = false,
    val isConnected: Boolean = false
)
