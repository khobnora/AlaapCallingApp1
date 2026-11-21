package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AlaapRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("ব্যবহারকারী লগইন করা নেই।")
  }

  private fun sanitizeShortId(raw: String): String {
    return raw.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").take(20)
  }

  fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
    if (uid.isBlank()) {
      emit(null)
      return@flow
    }
    val path = "users/$uid"
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { snap ->
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeContacts(uid: String): Flow<List<UserContact>> = flow {
    if (uid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "users/$uid/contacts"
    emitAll(
      db.collection("users").document(uid).collection("contacts")
        .snapshots()
        .map { snap ->
          snap.toObjects(UserContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayName.lowercase() }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeChats(uid: String): Flow<List<ChatRoom>> = flow {
    if (uid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "chats"
    emitAll(
      db.collection("chats")
        .whereArrayContains("memberUids", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = flow {
    if (uid.isBlank() || chatId.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "chats/$chatId/messages"
    emitAll(
      db.collection("chats").document(chatId).collection("messages")
        .whereArrayContains("memberUids", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeCalls(uid: String): Flow<List<CallSession>> = flow {
    if (uid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "calls"
    emitAll(
      db.collection("calls")
        .whereArrayContains("participantUids", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeCallSession(callId: String): Flow<CallSession?> = flow {
    if (callId.isBlank()) {
      emit(null)
      return@flow
    }
    val uid = auth.currentUser?.uid ?: return@flow
    val path = "calls/$callId"
    emitAll(
      db.collection("calls").document(callId)
        .snapshots()
        .map { snap ->
          snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeIceCandidates(callId: String, uid: String): Flow<List<IceCandidateRecord>> = flow {
    if (callId.isBlank() || uid.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "calls/$callId/candidates"
    emitAll(
      db.collection("calls").document(callId).collection("candidates")
        .whereArrayContains("participantUids", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(IceCandidateRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun ensureInitialProfile(user: FirebaseUser): Result<UserProfile> {
    val path = "users/${user.uid}"
    return try {
      val uid = requireUserId()
      val userDocRef = db.collection("users").document(uid)
      val snap = userDocRef.get().await()
      if (snap.exists()) {
        val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (existing != null) {
          // Ensure lookup doc is also synced
          val lookupRef = db.collection("short_id_lookup").document(existing.shortId)
          val lookupSnap = lookupRef.get().await()
          if (!lookupSnap.exists()) {
            lookupRef.set(
              mapOf(
                "userId" to uid,
                "shortId" to existing.shortId,
                "displayName" to existing.displayName,
                "photoUrl" to existing.photoUrl,
                "online" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            ).await()
          }
          return Result.success(existing)
        }
      }

      // Generate a unique 4-digit Short ID
      var chosenId = (1001 + Random.nextInt(8990)).toString()
      for (attempt in 0 until 8) {
        val candidate = if (attempt == 0) chosenId else (1001 + Random.nextInt(8990)).toString()
        val existsSnap = db.collection("short_id_lookup").document(candidate).get().await()
        if (!existsSnap.exists()) {
          chosenId = candidate
          break
        }
      }

      val cleanName = (user.displayName ?: "আলাপ ইউজার").trim().ifBlank { "আলাপ ইউজার" }.take(60)
      val email = (user.email ?: "").take(120)
      val photo = (user.photoUrl?.toString() ?: "").take(500)

      val profilePayload = mapOf(
        "uid" to uid,
        "shortId" to chosenId,
        "displayName" to cleanName,
        "email" to email,
        "phone" to (user.phoneNumber ?: "").take(30),
        "photoUrl" to photo,
        "notificationsEnabled" to true,
        "callRingtoneEnabled" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      userDocRef.set(profilePayload).await()

      val lookupPayload = mapOf(
        "userId" to uid,
        "shortId" to chosenId,
        "displayName" to cleanName,
        "photoUrl" to photo,
        "online" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("short_id_lookup").document(chosenId).set(lookupPayload).await()

      Result.success(
        UserProfile(
          uid = uid,
          shortId = chosenId,
          displayName = cleanName,
          email = email,
          photoUrl = photo,
          notificationsEnabled = true,
          callRingtoneEnabled = true,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun findUserByShortId(rawShortId: String): Result<ShortIdLookup?> {
    val cleanId = sanitizeShortId(rawShortId)
    if (cleanId.length < 3) {
      return Result.failure(IllegalArgumentException("কমপক্ষে ৩ অক্ষরের একটি সঠিক User ID দিন।"))
    }
    val path = "short_id_lookup/$cleanId"
    return try {
      requireUserId()
      val snap = db.collection("short_id_lookup").document(cleanId).get().await()
      if (!snap.exists()) {
        Result.success(null)
      } else {
        Result.success(
          snap.toObject(ShortIdLookup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun saveContact(myUid: String, peer: ShortIdLookup): Result<Unit> {
    val cleanShortId = sanitizeShortId(peer.shortId)
    if (cleanShortId.length < 3 || peer.userId.isBlank()) {
      return Result.success(Unit)
    }
    val path = "users/$myUid/contacts/${peer.userId}"
    return try {
      val uid = requireUserId()
      check(uid == myUid)
      val payload = mapOf(
        "contactUid" to peer.userId,
        "shortId" to cleanShortId,
        "displayName" to peer.displayName.trim().ifBlank { "আলাপ ইউজার" }.take(60),
        "photoUrl" to peer.photoUrl.take(300000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("users").document(uid).collection("contacts").document(peer.userId)
        .set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    uid: String,
    currentShortId: String,
    newName: String,
    newPhone: String,
    newPhotoUrl: String,
    notificationsEnabled: Boolean,
    callRingtoneEnabled: Boolean,
  ): Result<Unit> {
    val path = "users/$uid"
    return try {
      val authUid = requireUserId()
      check(authUid == uid)
      val safeName = newName.trim().ifBlank { "আলাপ ইউজার" }.take(60)
      val safePhone = newPhone.trim().take(30)
      val safePhoto = newPhotoUrl.take(300000)

      db.collection("users").document(uid).update(
        mapOf(
          "displayName" to safeName,
          "phone" to safePhone,
          "photoUrl" to safePhoto,
          "notificationsEnabled" to notificationsEnabled,
          "callRingtoneEnabled" to callRingtoneEnabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (currentShortId.isNotBlank()) {
        db.collection("short_id_lookup").document(currentShortId).update(
          mapOf(
            "displayName" to safeName,
            "photoUrl" to safePhoto,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun changeShortId(
    uid: String,
    oldShortId: String,
    desiredNewId: String,
    displayName: String,
    photoUrl: String,
  ): Result<String> {
    val cleanNew = sanitizeShortId(desiredNewId)
    if (cleanNew.length < 3) {
      return Result.failure(IllegalArgumentException("ইউনিক ID কমপক্ষে ৩ অক্ষরের (অক্ষর বা সংখ্যা) হতে হবে।"))
    }
    if (cleanNew == oldShortId) return Result.success(cleanNew)

    val path = "short_id_lookup/$cleanNew"
    return try {
      val authUid = requireUserId()
      check(authUid == uid)
      val existing = db.collection("short_id_lookup").document(cleanNew).get().await()
      if (existing.exists() && existing.getString("userId") != uid) {
        return Result.failure(IllegalStateException("এই ID ($cleanNew)-টি অন্য একজন ব্যবহার করছেন।"))
      }

      db.collection("short_id_lookup").document(cleanNew).set(
        mapOf(
          "userId" to uid,
          "shortId" to cleanNew,
          "displayName" to displayName.trim().ifBlank { "আলাপ ইউজার" }.take(60),
          "photoUrl" to photoUrl.take(300000),
          "online" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("users").document(uid).update(
        mapOf(
          "shortId" to cleanNew,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (oldShortId.isNotBlank() && oldShortId != cleanNew) {
        try {
          db.collection("short_id_lookup").document(oldShortId).delete().await()
        } catch (_: Exception) {
        }
      }
      Result.success(cleanNew)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun getOrCreateDirectChat(
    myProfile: UserProfile,
    peer: ShortIdLookup,
  ): Result<String> {
    val uid = requireUserId()
    val sorted = listOf(uid, peer.userId).sorted()
    val chatId = "direct_${sorted[0]}_${sorted[1]}"
    val path = "chats/$chatId"
    return try {
      val docRef = db.collection("chats").document(chatId)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val payload = mapOf(
          "groupChat" to false,
          "title" to peer.displayName.take(80),
          "memberUids" to sorted,
          "memberNames" to mapOf(
            myProfile.uid to myProfile.displayName.take(60),
            peer.userId to peer.displayName.take(60),
          ),
          "memberShortIds" to mapOf(
            myProfile.uid to myProfile.shortId,
            peer.userId to peer.shortId,
          ),
          "lastMessageText" to "চ্যাট শুরু হয়েছে",
          "lastMessageSenderId" to myProfile.uid,
          "lastMessageSenderName" to myProfile.displayName.take(60),
          "lastMessageType" to "system",
          "lastMessageAt" to FieldValue.serverTimestamp(),
          "typingUids" to emptyList<String>(),
          "activeCallId" to "",
          "activeCallVideo" to false,
          "activeCallHostId" to "",
          "createdBy" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
      // Also save contact quietly
      saveContact(uid, peer)
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun createGroupChat(
    myProfile: UserProfile,
    title: String,
    members: List<UserContact>,
  ): Result<String> {
    val cleanTitle = title.trim().take(80)
    if (cleanTitle.isBlank()) {
      return Result.failure(IllegalArgumentException("গ্রুপের একটি নাম লিখুন।"))
    }
    val path = "chats"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("chats").document()
      val allMembers = (listOf(uid) + members.map { it.contactUid }).distinct().take(20)
      val namesMap = mutableMapOf(uid to myProfile.displayName.take(60))
      val idsMap = mutableMapOf(uid to myProfile.shortId)
      for (m in members) {
        namesMap[m.contactUid] = m.displayName.take(60)
        idsMap[m.contactUid] = m.shortId
      }

      val payload = mapOf(
        "groupChat" to true,
        "title" to cleanTitle,
        "memberUids" to allMembers,
        "memberNames" to namesMap,
        "memberShortIds" to idsMap,
        "lastMessageText" to "${myProfile.displayName} গ্রুপ তৈরি করেছেন",
        "lastMessageSenderId" to uid,
        "lastMessageSenderName" to myProfile.displayName.take(60),
        "lastMessageType" to "system",
        "lastMessageAt" to FieldValue.serverTimestamp(),
        "typingUids" to emptyList<String>(),
        "activeCallId" to "",
        "activeCallVideo" to false,
        "activeCallHostId" to "",
        "createdBy" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun addMembersToGroup(
    chat: ChatRoom,
    newMembers: List<UserContact>,
  ): Result<Unit> {
    val path = "chats/${chat.id}"
    return try {
      requireUserId()
      val mergedUids = (chat.memberUids + newMembers.map { it.contactUid }).distinct().take(20)
      val namesMap = chat.memberNames.toMutableMap()
      val idsMap = chat.memberShortIds.toMutableMap()
      for (m in newMembers) {
        namesMap[m.contactUid] = m.displayName.take(60)
        idsMap[m.contactUid] = m.shortId
      }
      db.collection("chats").document(chat.id).update(
        mapOf(
          "title" to chat.title,
          "memberUids" to mergedUids,
          "memberNames" to namesMap,
          "memberShortIds" to idsMap,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun sendMessage(
    chat: ChatRoom,
    sender: UserProfile,
    text: String,
    messageType: String = "text",
    mediaBase64: String? = null,
    fileName: String? = null,
  ): Result<Unit> {
    val cleanText = text.trim().take(2000)
    if (cleanText.isBlank() && mediaBase64.isNullOrBlank()) {
      return Result.success(Unit)
    }
    val path = "chats/${chat.id}/messages"
    return try {
      val uid = requireUserId()
      val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
      val payload = mutableMapOf<String, Any>(
        "chatId" to chat.id,
        "senderId" to uid,
        "senderName" to sender.displayName.take(60),
        "memberUids" to chat.memberUids,
        "text" to cleanText,
        "messageType" to messageType,
        "deliveredTo" to listOf(uid),
        "seenBy" to listOf(uid),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (!mediaBase64.isNullOrBlank()) {
        payload["mediaBase64"] = mediaBase64.take(600000)
      }
      if (!fileName.isNullOrBlank()) {
        payload["fileName"] = fileName.take(150)
      }
      msgRef.set(payload).await()

      val summary = when (messageType) {
        "image" -> "📷 ছবি পাঠিয়েছেন"
        "file" -> "📎 ফাইল: ${fileName ?: "নথি"}"
        else -> cleanText
      }.take(500)

      val updatedTyping = chat.typingUids.filter { it != uid }
      db.collection("chats").document(chat.id).update(
        mapOf(
          "lastMessageText" to summary,
          "lastMessageSenderId" to uid,
          "lastMessageSenderName" to sender.displayName.take(60),
          "lastMessageType" to messageType,
          "lastMessageAt" to FieldValue.serverTimestamp(),
          "typingUids" to updatedTyping,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun markMessagesDeliveredAndSeen(
    chatId: String,
    messages: List<ChatMessage>,
    myUid: String,
    markSeen: Boolean,
  ): Result<Unit> {
    if (chatId.isBlank() || myUid.isBlank()) return Result.success(Unit)
    return try {
      requireUserId()
      for (msg in messages) {
        if (msg.senderId == myUid || msg.id.isBlank()) continue
        val needsDelivered = myUid !in msg.deliveredTo
        val needsSeen = markSeen && (myUid !in msg.seenBy)
        if (!needsDelivered && !needsSeen) continue
        val nextDelivered = (msg.deliveredTo + myUid).distinct().take(20)
        val nextSeen = if (markSeen) (msg.seenBy + myUid).distinct().take(20) else msg.seenBy
        db.collection("chats").document(chatId)
          .collection("messages").document(msg.id)
          .update(
            mapOf(
              "deliveredTo" to nextDelivered,
              "seenBy" to nextSeen,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages")
      Result.failure(e)
    }
  }

  suspend fun setTyping(chat: ChatRoom, myUid: String, isTyping: Boolean): Result<Unit> {
    val path = "chats/${chat.id}"
    return try {
      requireUserId()
      val next = if (isTyping) {
        (chat.typingUids + myUid).distinct().take(20)
      } else {
        chat.typingUids.filter { it != myUid }
      }
      if (next == chat.typingUids) return Result.success(Unit)
      db.collection("chats").document(chat.id).update(
        mapOf(
          "typingUids" to next,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun startCall(
    caller: UserProfile,
    participants: List<UserContact>,
    videoCall: Boolean,
    groupCall: Boolean,
    chatId: String = "",
    groupTitle: String = "",
  ): Result<String> {
    val path = "calls"
    return try {
      val uid = requireUserId()
      val callRef = db.collection("calls").document()
      val allUids = (listOf(uid) + participants.map { it.contactUid }).distinct().take(20)
      val namesMap = mutableMapOf(uid to caller.displayName.take(60))
      val shortIdsMap = mutableMapOf(uid to caller.shortId)
      for (p in participants) {
        namesMap[p.contactUid] = p.displayName.take(60)
        shortIdsMap[p.contactUid] = p.shortId
      }

      val payload = mutableMapOf<String, Any>(
        "callerId" to uid,
        "callerName" to caller.displayName.take(60),
        "callerShortId" to caller.shortId,
        "participantUids" to allUids,
        "joinedUids" to listOf(uid),
        "participantNames" to namesMap,
        "participantShortIds" to shortIdsMap,
        "videoCall" to videoCall,
        "groupCall" to groupCall,
        "status" to if (groupCall) "active" else "ringing",
        "durationSeconds" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (chatId.isNotBlank()) payload["chatId"] = chatId.take(128)
      if (groupTitle.isNotBlank()) payload["groupTitle"] = groupTitle.take(80)

      callRef.set(payload).await()

      if (groupCall && chatId.isNotBlank()) {
        val label = if (videoCall) "ভিডিও গ্রুপ কল চলছে · যোগ দিন" else "ভয়েস গ্রুপ কল চলছে · যোগ দিন"
        db.collection("chats").document(chatId).update(
          mapOf(
            "activeCallId" to callRef.id,
            "activeCallVideo" to videoCall,
            "activeCallHostId" to uid,
            "lastMessageText" to label,
            "lastMessageType" to "call",
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      Result.success(callRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun setOfferSdp(callId: String, offerSdp: String): Result<Unit> {
    val path = "calls/$callId"
    return try {
      requireUserId()
      db.collection("calls").document(callId).update(
        mapOf(
          "offerSdp" to offerSdp.take(50000),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun joinOrAcceptCall(
    call: CallSession,
    myUid: String,
    answerSdp: String? = null,
  ): Result<Unit> {
    val path = "calls/${call.id}"
    return try {
      requireUserId()
      val nextJoined = (call.joinedUids + myUid).distinct().take(20)
      val payload = mutableMapOf<String, Any>(
        "status" to "active",
        "joinedUids" to nextJoined,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (!answerSdp.isNullOrBlank()) {
        payload["answerSdp"] = answerSdp.take(50000)
      }
      db.collection("calls").document(call.id).update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun addIceCandidate(
    callId: String,
    participantUids: List<String>,
    sdpMid: String,
    sdpMLineIndex: Int,
    candidate: String,
  ): Result<Unit> {
    val path = "calls/$callId/candidates"
    return try {
      val uid = requireUserId()
      db.collection("calls").document(callId).collection("candidates").add(
        mapOf(
          "senderId" to uid,
          "participantUids" to participantUids,
          "sdpMid" to sdpMid.take(64),
          "sdpMLineIndex" to sdpMLineIndex,
          "candidate" to candidate.take(2000),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun endOrRejectCall(
    call: CallSession,
    myUid: String,
    newStatus: String,
    durationSeconds: Long,
  ): Result<Unit> {
    val path = "calls/${call.id}"
    return try {
      requireUserId()
      if (call.status in listOf("ended", "missed", "rejected")) {
        return Result.success(Unit)
      }
      val remainingJoined = call.joinedUids.filter { it != myUid }
      // For group calls, if others are still in the call and we are just leaving (not rejecting), keep call active
      val shouldKeepGroupAlive = call.groupCall &&
        newStatus == "ended" &&
        remainingJoined.isNotEmpty() &&
        myUid != call.callerId

      val finalStatus = if (shouldKeepGroupAlive) "active" else newStatus
      val finalJoined = if (shouldKeepGroupAlive) remainingJoined else call.joinedUids

      db.collection("calls").document(call.id).update(
        mapOf(
          "status" to finalStatus,
          "joinedUids" to finalJoined,
          "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (!shouldKeepGroupAlive && call.groupCall && call.chatId.isNotBlank()) {
        val summaryText = if (call.videoCall) "গ্রুপ ভিডিও কল শেষ হয়েছে" else "গ্রুপ ভয়েস কল শেষ হয়েছে"
        db.collection("chats").document(call.chatId).update(
          mapOf(
            "activeCallId" to "",
            "activeCallVideo" to false,
            "activeCallHostId" to "",
            "lastMessageText" to summaryText,
            "lastMessageType" to "call",
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }
}
