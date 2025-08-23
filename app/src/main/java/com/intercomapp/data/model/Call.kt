package com.intercomapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Call(
    @DocumentId
    val id: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val groupId: String? = null,
    val type: CallType = CallType.ONE_ON_ONE,
    val status: CallStatus = CallStatus.INITIATED,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val duration: Long = 0,
    val isMuted: Boolean = false,
    val participants: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)

enum class CallType {
    ONE_ON_ONE,
    GROUP
}

enum class CallStatus {
    INITIATED,
    RINGING,
    CONNECTED,
    ENDED,
    MISSED,
    REJECTED
}

data class CallParticipant(
    val userId: String = "",
    val joinedAt: Timestamp = Timestamp.now(),
    val leftAt: Timestamp? = null,
    val isMuted: Boolean = false,
    val isConnected: Boolean = false
)
