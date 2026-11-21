package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.ChatRoom
import com.example.data.UiState
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.compressUriToBase64
import com.example.ui.components.decodeBase64Bitmap
import com.example.ui.components.displayTitleFor
import com.example.ui.components.formatBengaliTime
import com.example.ui.components.readFileToBase64
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapPurple
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
  chat: ChatRoom?,
  myUid: String,
  messagesState: UiState<List<ChatMessage>>,
  onBack: () -> Unit,
  onVoiceCall: (ChatRoom) -> Unit,
  onVideoCall: (ChatRoom) -> Unit,
  onJoinGroupCall: (String) -> Unit,
  onSendMessage: (ChatRoom, String, String, String?, String?) -> Unit,
  onMarkRead: (String, List<ChatMessage>) -> Unit,
  onTypingChanged: (ChatRoom, Boolean) -> Unit,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null && chat != null) {
      val b64 = compressUriToBase64(context, uri)
      if (b64 != null) {
        onSendMessage(chat, "", "image", b64, "photo.jpg")
      } else {
        onShowToast("ছবিটি পড়া যায়নি")
      }
    }
  }

  val filePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null && chat != null) {
      val result = readFileToBase64(context, uri)
      when {
        result == null -> onShowToast("ফাইলটি পড়া যায়নি")
        result.first == "TOO_LARGE" -> onShowToast("৩৮০ কেবি-এর ছোট ফাইল নির্বাচন করুন")
        else -> onSendMessage(chat, "", "file", result.first, result.second)
      }
    }
  }

  val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

  // Mark delivered & seen when chat is open
  LaunchedEffect(chat?.id, messages.size) {
    if (chat != null && messages.isNotEmpty()) {
      onMarkRead(chat.id, messages)
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  // Typing status debounce
  LaunchedEffect(inputText) {
    if (chat == null) return@LaunchedEffect
    if (inputText.isNotBlank()) {
      onTypingChanged(chat, true)
      delay(2500L)
      onTypingChanged(chat, false)
    } else {
      onTypingChanged(chat, false)
    }
  }

  DisposableEffect(chat?.id) {
    onDispose {
      if (chat != null) onTypingChanged(chat, false)
    }
  }

  val title = chat?.displayTitleFor(myUid) ?: "চ্যাট"
  val someoneTyping = chat?.typingUids?.any { it != myUid } == true

  Scaffold(
    contentWindowInsets = WindowInsets(0),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "পেছনে")
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            AlaapAvatar(
              name = title,
              size = 38.dp,
              seed = chat?.id ?: title,
              overrideColor = if (chat?.groupChat == true) AlaapPurple else AlaapBlue,
            )
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
              )
              Text(
                text = when {
                  someoneTyping -> "টাইপ করছে..."
                  chat?.groupChat == true -> "${chat.memberUids.size} জন সদস্য"
                  else -> "অনলাইন / আলাপ ID"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (someoneTyping) AlaapBlue else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        actions = {
          if (chat != null) {
            IconButton(
              onClick = { onVoiceCall(chat) },
              modifier = Modifier.testTag("chat_voice_call"),
            ) {
              Icon(Icons.Rounded.Call, contentDescription = "ভয়েস কল", tint = AlaapGreen)
            }
            IconButton(
              onClick = { onVideoCall(chat) },
              modifier = Modifier.testTag("chat_video_call"),
            ) {
              Icon(Icons.Rounded.Videocam, contentDescription = "ভিডিও কল", tint = AlaapBlue)
            }
          }
        },
      )
    },
    bottomBar = {
      if (chat != null) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                photoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
            ) {
              Icon(Icons.Rounded.Image, contentDescription = "ছবি পাঠান", tint = AlaapBlue)
            }
            IconButton(
              onClick = { filePicker.launch(arrayOf("*/*")) },
            ) {
              Icon(Icons.Rounded.AttachFile, contentDescription = "ফাইল পাঠান", tint = AlaapPurple)
            }
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it },
              placeholder = { Text("মেসেজ লিখুন...") },
              singleLine = false,
              maxLines = 4,
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("chat_message_input"),
            )
            Spacer(Modifier.width(6.dp))
            Surface(
              shape = CircleShape,
              color = AlaapBlue,
              modifier = Modifier.size(48.dp),
            ) {
              IconButton(
                onClick = {
                  val textToSend = inputText.trim()
                  if (textToSend.isNotEmpty()) {
                    inputText = ""
                    onSendMessage(chat, textToSend, "text", null, null)
                  }
                },
                modifier = Modifier.testTag("chat_send_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Send,
                  contentDescription = "পাঠান",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Ongoing group call banner inside chat
      if (chat != null && chat.activeCallId.isNotBlank()) {
        Surface(
          color = AlaapGreen.copy(alpha = 0.14f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "গ্রুপ কল চলছে · যোগ দিন",
              color = AlaapGreen,
              fontWeight = FontWeight.SemiBold,
            )
            Button(
              onClick = { onJoinGroupCall(chat.activeCallId) },
              colors = ButtonDefaults.buttonColors(containerColor = AlaapGreen),
              shape = RoundedCornerShape(50),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
              Text("Join", color = Color.White)
            }
          }
        }
      }

      when (messagesState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlaapBlue)
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(messagesState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(messages, key = { it.id }) { msg ->
              MessageBubble(
                msg = msg,
                isMine = msg.senderId == myUid,
                isGroup = chat?.groupChat == true,
                memberCount = chat?.memberUids?.size ?: 2,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(
  msg: ChatMessage,
  isMine: Boolean,
  isGroup: Boolean,
  memberCount: Int,
) {
  val bubbleColor = if (isMine) AlaapBlue else MaterialTheme.colorScheme.surface
  val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
  val imageBitmap = remember(msg.mediaBase64) {
    if (msg.messageType == "image" && msg.mediaBase64.isNotBlank()) {
      decodeBase64Bitmap(msg.mediaBase64)
    } else null
  }

  val othersCount = (memberCount - 1).coerceAtLeast(1)
  val seenByOther = msg.seenBy.count { it != msg.senderId } >= 1
  val deliveredToOther = msg.deliveredTo.count { it != msg.senderId } >= 1

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      color = bubbleColor,
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp,
      ),
      shadowElevation = 1.dp,
      modifier = Modifier.widthIn(max = 290.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        if (!isMine && isGroup) {
          Text(
            text = msg.senderName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AlaapPurple,
          )
          Spacer(Modifier.height(2.dp))
        }

        if (imageBitmap != null) {
          Image(
            bitmap = imageBitmap.asImageBitmap(),
            contentDescription = "ছবি",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(12.dp)),
          )
          if (msg.text.isNotBlank()) Spacer(Modifier.height(6.dp))
        }

        if (msg.messageType == "file") {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isMine) Color.White.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant
              )
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Description,
              contentDescription = "ফাইল",
              tint = textColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = msg.fileName.ifBlank { "নথি" },
              color = textColor,
              fontWeight = FontWeight.Medium,
            )
          }
        }

        if (msg.text.isNotBlank()) {
          Text(
            text = msg.text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
          )
        }

        Spacer(Modifier.height(4.dp))

        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = formatBengaliTime(msg.createdAt),
            fontSize = 11.sp,
            color = if (isMine) Color.White.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (isMine) {
            Icon(
              imageVector = if (seenByOther || deliveredToOther || othersCount == 0) {
                Icons.Rounded.DoneAll
              } else {
                Icons.Rounded.Check
              },
              contentDescription = if (seenByOther) "সিন" else if (deliveredToOther) "ডেলিভার্ড" else "সেন্ট",
              tint = if (seenByOther) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }
    }
  }
}
