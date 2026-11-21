package com.example.ui.screens

import android.Manifest
import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddIcCall
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.call.NotificationHelper
import com.example.data.AlaapRepository
import com.example.data.CallSession
import com.example.data.ShortIdLookup
import com.example.data.UiState
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.viewmodel.AlaapViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object Routes {
  const val HOME = "home"
  const val CHAT = "chat/{chatId}"
  const val CREATE_GROUP = "create_group"
  const val ACTIVE_CALL = "active_call/{callId}"

  fun chat(chatId: String) = "chat/$chatId"
  fun activeCall(callId: String) = "active_call/$callId"
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
  pendingCallId: String? = null,
  pendingAction: String? = null,
  pendingChatId: String? = null,
  onClearPendingIntents: () -> Unit = {},
) {
  val context = LocalContext.current
  val firebaseInitialized = remember(context) {
    if (FirebaseApp.getApps(context).isNotEmpty()) {
      true
    } else {
      runCatching { FirebaseApp.initializeApp(context) }.getOrNull() != null ||
        FirebaseApp.getApps(context).isNotEmpty()
    }
  }

  if (!firebaseInitialized) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
      ) {
        androidx.compose.material3.Card(
          shape = RoundedCornerShape(20.dp),
          colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Firebase কনফিগারেশন পাওয়া যায়নি",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.error,
            )
            Text(
              text = "এই APK-টি app/google-services.json ফাইল ছাড়া বিল্ড হয়েছে। GitHub বা AI Studio-তে app ফোল্ডারের ভেতরে google-services.json ফাইল রেখে নতুন করে APK তৈরি করুন।",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
    return
  }

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow()
    .collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      AuthenticatedApp(
        currentUserId = user.uid,
        pendingCallId = pendingCallId,
        pendingAction = pendingAction,
        pendingChatId = pendingChatId,
        onClearPendingIntents = onClearPendingIntents,
      )
    }
  }
}

@Composable
private fun AuthenticatedApp(
  currentUserId: String,
  pendingCallId: String?,
  pendingAction: String?,
  pendingChatId: String?,
  onClearPendingIntents: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: AlaapViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) as Application
        val dbId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        AlaapViewModel(AlaapRepository(db), currentUserId)
      }
    },
  )

  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
  val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
  val callsState by viewModel.callsState.collectAsStateWithLifecycle()
  val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
  val activeCall by viewModel.activeCallSession.collectAsStateWithLifecycle()
  val activeCallId by viewModel.activeCallId.collectAsStateWithLifecycle()
  val callDuration by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
  val iceCandidates by viewModel.activeIceCandidates.collectAsStateWithLifecycle()

  val peerLookupLoading by viewModel.peerLookupLoading.collectAsStateWithLifecycle()
  val peerLookupResult by viewModel.peerLookupResult.collectAsStateWithLifecycle()
  val peerLookupError by viewModel.peerLookupError.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val contacts = (contactsState as? UiState.Success)?.data ?: emptyList()
  val chats = (chatsState as? UiState.Success)?.data ?: emptyList()
  val calls = (callsState as? UiState.Success)?.data ?: emptyList()

  // Ask notification permission on Android 13+ once signed in
  val notifPermLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {}
  LaunchedEffect(Unit) {
    NotificationHelper.ensureChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  // Toast messages
  LaunchedEffect(Unit) {
    viewModel.toastMessage.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  // Detect incoming ringing call
  val incomingCall: CallSession? = remember(calls, currentUserId, activeCallId) {
    calls.firstOrNull {
      it.status == "ringing" && it.callerId != currentUserId && it.id != activeCallId
    }
  }

  // Play ringtone + post full-screen call notification when incoming call rings
  var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }
  LaunchedEffect(incomingCall?.id) {
    if (incomingCall != null) {
      if (profile?.notificationsEnabled != false) {
        NotificationHelper.showIncomingCallNotification(context, incomingCall)
      }
      if (profile?.callRingtoneEnabled != false) {
        try {
          val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          val ringtone = RingtoneManager.getRingtone(context, uri)
          ringtone?.play()
          activeRingtone = ringtone
        } catch (_: Exception) {
        }
      }
    } else {
      NotificationHelper.cancelIncomingCallNotification(context)
      try {
        activeRingtone?.stop()
      } catch (_: Exception) {
      }
      activeRingtone = null
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      try {
        activeRingtone?.stop()
      } catch (_: Exception) {
      }
      NotificationHelper.cancelIncomingCallNotification(context)
    }
  }

  // Handle notification action clicks (Accept / Reject / Open Chat)
  LaunchedEffect(pendingCallId, pendingAction, calls) {
    if (!pendingCallId.isNullOrBlank()) {
      val target = calls.firstOrNull { it.id == pendingCallId }
      if (target != null) {
        when (pendingAction) {
          NotificationHelper.ACTION_ACCEPT_CALL -> {
            viewModel.joinOrAnswerCall(target) {
              navController.navigate(Routes.activeCall(target.id)) { launchSingleTop = true }
            }
          }
          NotificationHelper.ACTION_REJECT_CALL -> {
            viewModel.endOrRejectCall(target, "rejected")
          }
        }
        onClearPendingIntents()
      }
    }
  }

  LaunchedEffect(pendingChatId) {
    if (!pendingChatId.isNullOrBlank()) {
      viewModel.setOpenChat(pendingChatId)
      navController.navigate(Routes.chat(pendingChatId)) { launchSingleTop = true }
      onClearPendingIntents()
    }
  }

  // Auto-close active call screen if remote ends/rejects the call
  LaunchedEffect(activeCall?.status, activeCallId) {
    val status = activeCall?.status
    if (activeCallId.isNotBlank() && status in listOf("ended", "missed", "rejected")) {
      viewModel.setActiveCallId("")
      navController.popBackStack(Routes.HOME, inclusive = false)
    } else if (activeCallId.isNotBlank() && status == "active") {
      viewModel.startCallTimerIfNeeded()
    }
  }

  // Dialog states
  var peerDialogMode by remember { mutableStateOf<PeerActionMode?>(null) }
  var peerDialogInitialQuery by remember { mutableStateOf("") }
  var showEditProfileDialog by remember { mutableStateOf(false) }

  var selectedTab by remember { mutableIntStateOf(0) }
  val openChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
  val lastNotifiedChatTimes = remember { mutableMapOf<String, Long>() }

  LaunchedEffect(chats, openChatId, profile?.notificationsEnabled) {
    val enabled = profile?.notificationsEnabled ?: true
    for (chat in chats) {
      val timeMs = chat.lastMessageAt?.toDate()?.time ?: 0L
      val prev = lastNotifiedChatTimes[chat.id]
      if (prev == null) {
        lastNotifiedChatTimes[chat.id] = timeMs
      } else if (timeMs > prev) {
        lastNotifiedChatTimes[chat.id] = timeMs
        if (enabled && chat.id != openChatId && chat.lastMessageSenderId.isNotBlank() && chat.lastMessageSenderId != currentUserId && chat.lastMessageType != "system") {
          NotificationHelper.showMessageNotification(
            context = context,
            senderName = chat.lastMessageSenderName.ifBlank { chat.title.ifBlank { "নতুন মেসেজ" } },
            messageText = chat.lastMessageText,
            chatId = chat.id,
          )
        }
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = Routes.HOME,
    ) {
      composable(Routes.HOME) {
        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          floatingActionButton = {
            FloatingActionButton(
              onClick = {
                viewModel.clearPeerLookup()
                peerDialogInitialQuery = ""
                peerDialogMode = PeerActionMode.VOICE_CALL
              },
              shape = CircleShape,
              containerColor = AlaapGreen,
              contentColor = Color.White,
              modifier = Modifier.testTag("home_call_fab"),
            ) {
              Icon(Icons.Rounded.AddIcCall, contentDescription = "নতুন কল")
            }
          },
          bottomBar = {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
            ) {
              val navItems = listOf(
                Triple("চ্যাট", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
                Triple("কল", Icons.Rounded.Call, Icons.Outlined.Call),
                Triple("গ্রুপ", Icons.Rounded.Groups, Icons.Outlined.Groups),
                Triple("সেটিংস", Icons.Rounded.Settings, Icons.Outlined.Settings),
              )
              navItems.forEachIndexed { index, (label, filled, outlined) ->
                NavigationBarItem(
                  selected = selectedTab == index,
                  onClick = { selectedTab = index },
                  icon = {
                    Icon(
                      imageVector = if (selectedTab == index) filled else outlined,
                      contentDescription = label,
                    )
                  },
                  label = { Text(label) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AlaapBlue,
                    selectedTextColor = AlaapBlue,
                    indicatorColor = AlaapBlue.copy(alpha = 0.12f),
                  ),
                  modifier = Modifier.testTag("bottom_tab_$index"),
                )
              }
            }
          },
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            when (selectedTab) {
              0 -> HomeChatTab(
                myUid = currentUserId,
                profile = profile,
                chatsState = chatsState,
                callsState = callsState,
                onCopyIdFeedback = { msg ->
                  scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onQuickVoiceCall = {
                  viewModel.clearPeerLookup()
                  peerDialogInitialQuery = ""
                  peerDialogMode = PeerActionMode.VOICE_CALL
                },
                onQuickVideoCall = {
                  viewModel.clearPeerLookup()
                  peerDialogInitialQuery = ""
                  peerDialogMode = PeerActionMode.VIDEO_CALL
                },
                onQuickGroupCall = {
                  selectedTab = 2
                },
                onQuickNewMessage = {
                  viewModel.clearPeerLookup()
                  peerDialogInitialQuery = ""
                  peerDialogMode = PeerActionMode.MESSAGE
                },
                onOpenChat = { chatId ->
                  viewModel.setOpenChat(chatId)
                  navController.navigate(Routes.chat(chatId))
                },
                onJoinGroupCall = { callId ->
                  val targetCall = calls.firstOrNull { it.id == callId }
                    ?: CallSession(id = callId, groupCall = true, status = "active")
                  viewModel.joinOrAnswerCall(targetCall) {
                    navController.navigate(Routes.activeCall(callId))
                  }
                },
                onCallbackMissed = { missedCall ->
                  val peer = ShortIdLookup(
                    userId = missedCall.callerId,
                    shortId = missedCall.callerShortId,
                    displayName = missedCall.callerName,
                  )
                  viewModel.startDirectCall(peer, missedCall.videoCall) { newCallId ->
                    navController.navigate(Routes.activeCall(newCallId))
                  }
                },
                onFindByIdRequested = { query ->
                  viewModel.clearPeerLookup()
                  peerDialogInitialQuery = query
                  peerDialogMode = PeerActionMode.ANY
                  viewModel.lookupUserByShortId(query)
                },
              )

              1 -> CallsHistoryTab(
                myUid = currentUserId,
                callsState = callsState,
                onRedialCall = { oldCall, isVideo ->
                  if (oldCall.groupCall && oldCall.chatId.isNotBlank()) {
                    val chat = chats.firstOrNull { it.id == oldCall.chatId }
                    if (chat != null) {
                      viewModel.startGroupCall(chat, isVideo) { newCallId ->
                        navController.navigate(Routes.activeCall(newCallId))
                      }
                    }
                  } else {
                    val otherUid = oldCall.participantUids.firstOrNull { it != currentUserId }
                      ?: oldCall.callerId
                    val peer = ShortIdLookup(
                      userId = otherUid,
                      shortId = oldCall.participantShortIds[otherUid] ?: oldCall.callerShortId,
                      displayName = oldCall.participantNames[otherUid] ?: oldCall.callerName,
                    )
                    viewModel.startDirectCall(peer, isVideo) { newCallId ->
                      navController.navigate(Routes.activeCall(newCallId))
                    }
                  }
                },
              )

              2 -> GroupsTab(
                chatsState = chatsState,
                onCreateGroupClick = {
                  viewModel.clearPeerLookup()
                  navController.navigate(Routes.CREATE_GROUP)
                },
                onOpenGroupChat = { chatId ->
                  viewModel.setOpenChat(chatId)
                  navController.navigate(Routes.chat(chatId))
                },
                onStartGroupCall = { chat, isVideo ->
                  viewModel.startGroupCall(chat, isVideo) { newCallId ->
                    navController.navigate(Routes.activeCall(newCallId))
                  }
                },
                onJoinActiveGroupCall = { callId ->
                  val targetCall = calls.firstOrNull { it.id == callId }
                    ?: CallSession(id = callId, groupCall = true, status = "active")
                  viewModel.joinOrAnswerCall(targetCall) {
                    navController.navigate(Routes.activeCall(callId))
                  }
                },
              )

              3 -> SettingsTab(
                profile = profile,
                onEditProfileClick = { showEditProfileDialog = true },
                onToggleNotifications = { enabled ->
                  profile?.let {
                    viewModel.updateProfile(
                      newName = it.displayName,
                      newPhone = it.phone,
                      newPhotoUrl = it.photoUrl,
                      desiredShortId = it.shortId,
                      notificationsEnabled = enabled,
                      callRingtoneEnabled = it.callRingtoneEnabled,
                    )
                  }
                },
                onToggleRingtone = { enabled ->
                  profile?.let {
                    viewModel.updateProfile(
                      newName = it.displayName,
                      newPhone = it.phone,
                      newPhotoUrl = it.photoUrl,
                      desiredShortId = it.shortId,
                      notificationsEnabled = it.notificationsEnabled,
                      callRingtoneEnabled = enabled,
                    )
                  }
                },
                onCopyFeedback = { msg ->
                  scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onSignOut = {
                  GoogleAuthHelper.signOut(credentialManager, scope)
                },
              )
            }
          }
        }
      }

      composable(Routes.CHAT) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()
        val chat = chats.firstOrNull { it.id == chatId }
        BackHandler {
          viewModel.setOpenChat("")
          navController.popBackStack()
        }
        ChatRoomScreen(
          chat = chat,
          myUid = currentUserId,
          messagesState = messagesState,
          onBack = {
            viewModel.setOpenChat("")
            navController.popBackStack()
          },
          onVoiceCall = { room ->
            if (room.groupChat) {
              viewModel.startGroupCall(room, false) { callId ->
                navController.navigate(Routes.activeCall(callId))
              }
            } else {
              val otherUid = room.memberUids.firstOrNull { it != currentUserId } ?: return@ChatRoomScreen
              val peer = ShortIdLookup(
                userId = otherUid,
                shortId = room.memberShortIds[otherUid] ?: "",
                displayName = room.memberNames[otherUid] ?: "আলাপ ইউজার",
              )
              viewModel.startDirectCall(peer, false) { callId ->
                navController.navigate(Routes.activeCall(callId))
              }
            }
          },
          onVideoCall = { room ->
            if (room.groupChat) {
              viewModel.startGroupCall(room, true) { callId ->
                navController.navigate(Routes.activeCall(callId))
              }
            } else {
              val otherUid = room.memberUids.firstOrNull { it != currentUserId } ?: return@ChatRoomScreen
              val peer = ShortIdLookup(
                userId = otherUid,
                shortId = room.memberShortIds[otherUid] ?: "",
                displayName = room.memberNames[otherUid] ?: "আলাপ ইউজার",
              )
              viewModel.startDirectCall(peer, true) { callId ->
                navController.navigate(Routes.activeCall(callId))
              }
            }
          },
          onJoinGroupCall = { callId ->
            val targetCall = calls.firstOrNull { it.id == callId }
              ?: CallSession(id = callId, groupCall = true, status = "active")
            viewModel.joinOrAnswerCall(targetCall) {
              navController.navigate(Routes.activeCall(callId))
            }
          },
          onSendMessage = { room, text, type, b64, fileName ->
            viewModel.sendMessage(room, text, type, b64, fileName)
          },
          onMarkRead = { id, msgs ->
            viewModel.markMessagesRead(id, msgs, markSeen = true)
          },
          onTypingChanged = { room, typing ->
            viewModel.setTyping(room, typing)
          },
          onShowToast = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
          },
        )
      }

      composable(Routes.CREATE_GROUP) {
        BackHandler { navController.popBackStack() }
        CreateGroupScreen(
          contacts = contacts,
          foundPeer = peerLookupResult,
          lookupError = peerLookupError,
          onLookupId = { viewModel.lookupUserByShortId(it) },
          onSavePeerToContacts = { viewModel.saveContact(it) },
          onCreateGroup = { title, members ->
            viewModel.createGroup(title, members) { newChatId ->
              viewModel.setOpenChat(newChatId)
              navController.navigate(Routes.chat(newChatId)) {
                popUpTo(Routes.HOME)
              }
            }
          },
          onBack = { navController.popBackStack() },
        )
      }

      composable(Routes.ACTIVE_CALL) { backStackEntry ->
        val callId = backStackEntry.arguments?.getString("callId").orEmpty()
        val call = activeCall ?: calls.firstOrNull { it.id == callId }
        if (call != null) {
          BackHandler {
            val endStatus = if (call.status == "ringing" && call.callerId == currentUserId) "missed" else "ended"
            viewModel.endOrRejectCall(call, endStatus)
            navController.popBackStack(Routes.HOME, inclusive = false)
          }
          ActiveCallScreen(
            call = call,
            myUid = currentUserId,
            durationSeconds = callDuration,
            iceCandidates = iceCandidates,
            onOfferCreated = { sdp -> viewModel.setOfferSdp(call.id, sdp) },
            onAnswerCreated = { sdp -> viewModel.joinOrAnswerCall(call, answerSdp = sdp) },
            onIceCandidate = { mid, idx, cand ->
              viewModel.sendIceCandidate(call.id, call.participantUids, mid, idx, cand)
            },
            onEndCall = {
              val endStatus = if (call.status == "ringing" && call.callerId == currentUserId) "missed" else "ended"
              viewModel.endOrRejectCall(call, endStatus)
              navController.popBackStack(Routes.HOME, inclusive = false)
            },
          )
        }
      }
    }

    // Full-screen Incoming Call Overlay (works over any screen!)
    if (incomingCall != null) {
      IncomingCallScreen(
        call = incomingCall,
        onAccept = {
          viewModel.joinOrAnswerCall(incomingCall) {
            navController.navigate(Routes.activeCall(incomingCall.id)) {
              launchSingleTop = true
            }
          }
        },
        onReject = {
          viewModel.endOrRejectCall(incomingCall, "rejected")
        },
      )
    }

    // Find Peer / Dialer Dialog
    if (peerDialogMode != null) {
      FindPeerDialog(
        mode = peerDialogMode!!,
        initialQuery = peerDialogInitialQuery,
        contacts = contacts,
        isLoading = peerLookupLoading,
        foundPeer = peerLookupResult,
        errorText = peerLookupError,
        onSearchId = { viewModel.lookupUserByShortId(it) },
        onVoiceCallPeer = { peer ->
          peerDialogMode = null
          viewModel.startDirectCall(peer, false) { callId ->
            navController.navigate(Routes.activeCall(callId))
          }
        },
        onVideoCallPeer = { peer ->
          peerDialogMode = null
          viewModel.startDirectCall(peer, true) { callId ->
            navController.navigate(Routes.activeCall(callId))
          }
        },
        onMessagePeer = { peer ->
          peerDialogMode = null
          viewModel.openDirectChat(peer) { chatId ->
            viewModel.setOpenChat(chatId)
            navController.navigate(Routes.chat(chatId))
          }
        },
        onDismiss = { peerDialogMode = null },
      )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog && profile != null) {
      EditProfileDialog(
        profile = profile,
        onSave = { newName, newShortId, newPhone, newPhotoUrl ->
          viewModel.updateProfile(
            newName = newName,
            newPhone = newPhone,
            newPhotoUrl = newPhotoUrl,
            desiredShortId = newShortId,
            notificationsEnabled = profile.notificationsEnabled,
            callRingtoneEnabled = profile.callRingtoneEnabled,
            onDone = { showEditProfileDialog = false },
          )
        },
        onDismiss = { showEditProfileDialog = false },
      )
    }
  }
}
