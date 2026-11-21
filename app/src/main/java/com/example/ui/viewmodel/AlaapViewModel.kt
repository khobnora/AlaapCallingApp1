package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlaapRepository
import com.example.data.CallSession
import com.example.data.ChatMessage
import com.example.data.ChatRoom
import com.example.data.IceCandidateRecord
import com.example.data.ShortIdLookup
import com.example.data.UiState
import com.example.data.UserContact
import com.example.data.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlaapViewModel(
  private val repository: AlaapRepository,
  val currentUserId: String,
) : ViewModel() {

  companion object {
    private const val TAG = "AlaapVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }

  private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val toastMessage = _toastMessage.asSharedFlow()

  private val _activeChatId = MutableStateFlow("")
  val activeChatId = _activeChatId.asStateFlow()

  private val _activeCallId = MutableStateFlow("")
  val activeCallId = _activeCallId.asStateFlow()

  private val _callDurationSeconds = MutableStateFlow(0L)
  val callDurationSeconds = _callDurationSeconds.asStateFlow()

  private val _peerLookupLoading = MutableStateFlow(false)
  val peerLookupLoading = _peerLookupLoading.asStateFlow()

  private val _peerLookupResult = MutableStateFlow<ShortIdLookup?>(null)
  val peerLookupResult = _peerLookupResult.asStateFlow()

  private val _peerLookupError = MutableStateFlow<String?>(null)
  val peerLookupError = _peerLookupError.asStateFlow()

  private var callTimerJob: Job? = null

  val profileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile(currentUserId)
    .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Error observing profile", root)
      emit(UiState.Error(root.message ?: "প্রোফাইল লোড করতে সমস্যা হয়েছে"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val contactsState: StateFlow<UiState<List<UserContact>>> = repository.observeContacts(currentUserId)
    .map<List<UserContact>, UiState<List<UserContact>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Error observing contacts", root)
      emit(UiState.Error(root.message ?: "কন্টাক্ট লোড করতে সমস্যা হয়েছে"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val chatsState: StateFlow<UiState<List<ChatRoom>>> = repository.observeChats(currentUserId)
    .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Error observing chats", root)
      emit(UiState.Error(root.message ?: "চ্যাট তালিকা লোড করতে সমস্যা হয়েছে"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val callsState: StateFlow<UiState<List<CallSession>>> = repository.observeCalls(currentUserId)
    .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Error observing calls", root)
      emit(UiState.Error(root.message ?: "কল হিস্ট্রি লোড করতে সমস্যা হয়েছে"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val messagesState: StateFlow<UiState<List<ChatMessage>>> = _activeChatId
    .flatMapLatest { chatId ->
      if (chatId.isBlank()) {
        flowOf(UiState.Success(emptyList()))
      } else {
        repository.observeMessages(chatId, currentUserId)
          .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing messages", root)
            emit(UiState.Error(root.message ?: "মেসেজ লোড করতে সমস্যা হয়েছে"))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeCallSession: StateFlow<CallSession?> = _activeCallId
    .flatMapLatest { callId ->
      if (callId.isBlank()) flowOf(null) else repository.observeCallSession(callId)
        .catch { error ->
          if (error is CancellationException) throw error
          emit(null)
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeIceCandidates: StateFlow<List<IceCandidateRecord>> = _activeCallId
    .flatMapLatest { callId ->
      if (callId.isBlank()) flowOf(emptyList())
      else repository.observeIceCandidates(callId, currentUserId)
        .catch { error ->
          if (error is CancellationException) throw error
          emit(emptyList())
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  init {
    ensureProfile()
  }

  private fun ensureProfile() {
    val user = Firebase.auth.currentUser ?: return
    viewModelScope.launch {
      repository.ensureInitialProfile(user).onFailure { err ->
        _toastMessage.tryEmit(err.localizedMessage ?: "প্রোফাইল তৈরি করতে সমস্যা হয়েছে")
      }
    }
  }

  fun setOpenChat(chatId: String) {
    _activeChatId.value = chatId
  }

  fun setActiveCallId(callId: String) {
    _activeCallId.value = callId
    if (callId.isBlank()) {
      stopCallTimer()
    }
  }

  fun startCallTimerIfNeeded() {
    if (callTimerJob?.isActive == true) return
    _callDurationSeconds.value = 0L
    callTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        _callDurationSeconds.value += 1L
      }
    }
  }

  fun stopCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = null
    _callDurationSeconds.value = 0L
  }

  fun clearPeerLookup() {
    _peerLookupResult.value = null
    _peerLookupError.value = null
    _peerLookupLoading.value = false
  }

  fun lookupUserByShortId(shortId: String) {
    viewModelScope.launch {
      _peerLookupLoading.value = true
      _peerLookupError.value = null
      _peerLookupResult.value = null
      repository.findUserByShortId(shortId)
        .onSuccess { peer ->
          if (peer == null) {
            _peerLookupError.value = "এই ID ($shortId) দিয়ে কোনো ব্যবহারকারী পাওয়া যায়নি।"
          } else if (peer.userId == currentUserId) {
            _peerLookupError.value = "এটি আপনার নিজের ID।"
          } else {
            _peerLookupResult.value = peer
          }
        }
        .onFailure { err ->
          _peerLookupError.value = err.localizedMessage ?: "খুঁজতে সমস্যা হয়েছে।"
        }
      _peerLookupLoading.value = false
    }
  }

  fun saveContact(peer: ShortIdLookup) {
    viewModelScope.launch {
      repository.saveContact(currentUserId, peer)
        .onSuccess { _toastMessage.tryEmit("${peer.displayName} কন্টাক্টে যুক্ত হয়েছেন") }
        .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "কন্টাক্ট সেভ হয়নি") }
    }
  }

  fun openDirectChat(peer: ShortIdLookup, onChatReady: (String) -> Unit) {
    val myProfile = (profileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.getOrCreateDirectChat(myProfile, peer)
        .onSuccess { chatId -> onChatReady(chatId) }
        .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "চ্যাট খোলা যায়নি") }
    }
  }

  fun createGroup(title: String, members: List<UserContact>, onCreated: (String) -> Unit) {
    val myProfile = (profileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.createGroupChat(myProfile, title, members)
        .onSuccess { chatId ->
          _toastMessage.tryEmit("গ্রুপ '$title' তৈরি হয়েছে")
          onCreated(chatId)
        }
        .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "গ্রুপ তৈরি হয়নি") }
    }
  }

  fun addMembersToGroup(chat: ChatRoom, newMembers: List<UserContact>) {
    if (newMembers.isEmpty()) return
    viewModelScope.launch {
      repository.addMembersToGroup(chat, newMembers)
        .onSuccess { _toastMessage.tryEmit("নতুন সদস্য যোগ করা হয়েছে") }
        .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "সদস্য যোগ করা যায়নি") }
    }
  }

  fun sendMessage(
    chat: ChatRoom,
    text: String,
    messageType: String = "text",
    mediaBase64: String? = null,
    fileName: String? = null,
  ) {
    val sender = (profileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.sendMessage(chat, sender, text, messageType, mediaBase64, fileName)
        .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "মেসেজ পাঠানো যায়নি") }
    }
  }

  fun markMessagesRead(chatId: String, messages: List<ChatMessage>, markSeen: Boolean) {
    viewModelScope.launch {
      repository.markMessagesDeliveredAndSeen(chatId, messages, currentUserId, markSeen)
    }
  }

  fun setTyping(chat: ChatRoom, isTyping: Boolean) {
    viewModelScope.launch {
      repository.setTyping(chat, currentUserId, isTyping)
    }
  }

  fun updateProfile(
    newName: String,
    newPhone: String,
    newPhotoUrl: String,
    desiredShortId: String,
    notificationsEnabled: Boolean,
    callRingtoneEnabled: Boolean,
    onDone: () -> Unit = {},
  ) {
    val current = (profileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      var activeShortId = current.shortId
      val trimmedDesired = desiredShortId.trim().lowercase()
      if (trimmedDesired.isNotBlank() && trimmedDesired != current.shortId) {
        val changeRes = repository.changeShortId(
          uid = currentUserId,
          oldShortId = current.shortId,
          desiredNewId = trimmedDesired,
          displayName = newName.ifBlank { current.displayName },
          photoUrl = newPhotoUrl,
        )
        changeRes.onFailure { err ->
          _toastMessage.tryEmit(err.localizedMessage ?: "ID পরিবর্তন করা যায়নি")
          return@launch
        }.onSuccess { updatedId ->
          activeShortId = updatedId
        }
      }

      repository.updateProfile(
        uid = currentUserId,
        currentShortId = activeShortId,
        newName = newName,
        newPhone = newPhone,
        newPhotoUrl = newPhotoUrl,
        notificationsEnabled = notificationsEnabled,
        callRingtoneEnabled = callRingtoneEnabled,
      ).onSuccess {
        _toastMessage.tryEmit("প্রোফাইল আপডেট হয়েছে")
        onDone()
      }.onFailure { err ->
        _toastMessage.tryEmit(err.localizedMessage ?: "প্রোফাইল সেভ হয়নি")
      }
    }
  }

  fun startDirectCall(peer: ShortIdLookup, videoCall: Boolean, onStarted: (String) -> Unit) {
    val myProfile = (profileState.value as? UiState.Success)?.data ?: return
    val contact = UserContact(
      contactUid = peer.userId,
      shortId = peer.shortId,
      displayName = peer.displayName,
      photoUrl = peer.photoUrl,
    )
    viewModelScope.launch {
      repository.saveContact(currentUserId, peer)
      repository.startCall(
        caller = myProfile,
        participants = listOf(contact),
        videoCall = videoCall,
        groupCall = false,
      ).onSuccess { callId ->
        _activeCallId.value = callId
        onStarted(callId)
      }.onFailure { err ->
        _toastMessage.tryEmit(err.localizedMessage ?: "কল শুরু করা যায়নি")
      }
    }
  }

  fun startGroupCall(chat: ChatRoom, videoCall: Boolean, onStarted: (String) -> Unit) {
    val myProfile = (profileState.value as? UiState.Success)?.data ?: return
    val participants = chat.memberUids
      .filter { it != currentUserId }
      .map { uid ->
        UserContact(
          contactUid = uid,
          shortId = chat.memberShortIds[uid] ?: "",
          displayName = chat.memberNames[uid] ?: "সদস্য",
        )
      }
    viewModelScope.launch {
      repository.startCall(
        caller = myProfile,
        participants = participants,
        videoCall = videoCall,
        groupCall = true,
        chatId = chat.id,
        groupTitle = chat.title,
      ).onSuccess { callId ->
        _activeCallId.value = callId
        onStarted(callId)
      }.onFailure { err ->
        _toastMessage.tryEmit(err.localizedMessage ?: "গ্রুপ কল শুরু করা যায়নি")
      }
    }
  }

  fun joinOrAnswerCall(call: CallSession, answerSdp: String? = null, onJoined: () -> Unit = {}) {
    viewModelScope.launch {
      _activeCallId.value = call.id
      repository.joinOrAcceptCall(call, currentUserId, answerSdp)
        .onSuccess {
          startCallTimerIfNeeded()
          onJoined()
        }
        .onFailure { err ->
          _toastMessage.tryEmit(err.localizedMessage ?: "কলে যুক্ত হওয়া যায়নি")
        }
    }
  }

  fun setOfferSdp(callId: String, offerSdp: String) {
    viewModelScope.launch {
      repository.setOfferSdp(callId, offerSdp)
    }
  }

  fun sendIceCandidate(
    callId: String,
    participantUids: List<String>,
    sdpMid: String,
    sdpMLineIndex: Int,
    candidate: String,
  ) {
    viewModelScope.launch {
      repository.addIceCandidate(callId, participantUids, sdpMid, sdpMLineIndex, candidate)
    }
  }

  fun endOrRejectCall(call: CallSession, status: String) {
    val duration = _callDurationSeconds.value
    stopCallTimer()
    _activeCallId.value = ""
    viewModelScope.launch {
      repository.endOrRejectCall(call, currentUserId, status, duration)
    }
  }
}
