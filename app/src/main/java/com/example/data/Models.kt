package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  val uid: String = "",
  val shortId: String = "",
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val photoUrl: String = "",
  val notificationsEnabled: Boolean = true,
  val callRingtoneEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ShortIdLookup(
  val userId: String = "",
  val shortId: String = "",
  val displayName: String = "",
  val photoUrl: String = "",
  val online: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserContact(
  @DocumentId val id: String = "",
  val contactUid: String = "",
  val shortId: String = "",
  val displayName: String = "",
  val photoUrl: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatRoom(
  @DocumentId val id: String = "",
  val groupChat: Boolean = false,
  val title: String = "",
  val memberUids: List<String> = emptyList(),
  val memberNames: Map<String, String> = emptyMap(),
  val memberShortIds: Map<String, String> = emptyMap(),
  val lastMessageText: String = "",
  val lastMessageSenderId: String = "",
  val lastMessageSenderName: String = "",
  val lastMessageType: String = "text",
  val lastMessageAt: Timestamp? = null,
  val typingUids: List<String> = emptyList(),
  val activeCallId: String = "",
  val activeCallVideo: Boolean = false,
  val activeCallHostId: String = "",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val memberUids: List<String> = emptyList(),
  val text: String = "",
  val messageType: String = "text",
  val mediaBase64: String = "",
  val fileName: String = "",
  val deliveredTo: List<String> = emptyList(),
  val seenBy: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CallSession(
  @DocumentId val id: String = "",
  val callerId: String = "",
  val callerName: String = "",
  val callerShortId: String = "",
  val chatId: String = "",
  val groupTitle: String = "",
  val participantUids: List<String> = emptyList(),
  val joinedUids: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val participantShortIds: Map<String, String> = emptyMap(),
  val videoCall: Boolean = false,
  val groupCall: Boolean = false,
  val status: String = "ringing",
  val offerSdp: String = "",
  val answerSdp: String = "",
  val durationSeconds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class IceCandidateRecord(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val participantUids: List<String> = emptyList(),
  val sdpMid: String = "",
  val sdpMLineIndex: Int = 0,
  val candidate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
