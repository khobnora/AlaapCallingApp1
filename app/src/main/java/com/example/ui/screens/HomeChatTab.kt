package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallSession
import com.example.data.ChatRoom
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.displayTitleFor
import com.example.ui.components.formatBengaliTime
import com.example.ui.components.otherShortIdFor
import com.example.ui.theme.AlaapAmber
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapPurple
import com.example.ui.theme.AlaapRed

sealed interface RecentFeedItem {
  val sortTimeMs: Long
  data class ChatEntry(val chat: ChatRoom, override val sortTimeMs: Long) : RecentFeedItem
  data class MissedCallEntry(val call: CallSession, override val sortTimeMs: Long) : RecentFeedItem
}

@Composable
fun HomeChatTab(
  myUid: String,
  profile: UserProfile?,
  chatsState: UiState<List<ChatRoom>>,
  callsState: UiState<List<CallSession>>,
  onCopyIdFeedback: (String) -> Unit,
  onQuickVoiceCall: () -> Unit,
  onQuickVideoCall: () -> Unit,
  onQuickGroupCall: () -> Unit,
  onQuickNewMessage: () -> Unit,
  onOpenChat: (String) -> Unit,
  onJoinGroupCall: (String) -> Unit,
  onCallbackMissed: (CallSession) -> Unit,
  onFindByIdRequested: (String) -> Unit,
) {
  val clipboardManager = LocalClipboardManager.current
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  val myShortId = profile?.shortId?.ifBlank { "..." } ?: "..."

  val recentItems = remember(chatsState, callsState, searchQuery, myUid) {
    val chats = (chatsState as? UiState.Success)?.data ?: emptyList()
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()

    val missedCalls = calls.filter {
      it.callerId != myUid && (it.status == "missed" || it.status == "rejected")
    }.take(6)

    val combined = buildList {
      chats.forEach { chat ->
        add(RecentFeedItem.ChatEntry(chat, chat.lastMessageAt?.toDate()?.time ?: 0L))
      }
      missedCalls.forEach { call ->
        add(RecentFeedItem.MissedCallEntry(call, call.createdAt?.toDate()?.time ?: 0L))
      }
    }.sortedByDescending { it.sortTimeMs }

    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) combined
    else combined.filter { item ->
      when (item) {
        is RecentFeedItem.ChatEntry -> {
          item.chat.displayTitleFor(myUid).lowercase().contains(q) ||
            item.chat.otherShortIdFor(myUid).lowercase().contains(q) ||
            item.chat.lastMessageText.lowercase().contains(q)
        }
        is RecentFeedItem.MissedCallEntry -> {
          item.call.callerName.lowercase().contains(q) ||
            item.call.callerShortId.lowercase().contains(q)
        }
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Top Header: আমার ID + চ্যাট + Search Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                if (myShortId != "...") {
                  clipboardManager.setText(AnnotatedString(myShortId))
                  onCopyIdFeedback("আপনার ID ($myShortId) কপি হয়েছে")
                }
              }
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "আমার ID: $myShortId",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = "ID কপি করুন",
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = "চ্যাট",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          modifier = Modifier.size(48.dp),
        ) {
          IconButton(
            onClick = {
              showSearch = !showSearch
              if (!showSearch) searchQuery = ""
            },
            modifier = Modifier.testTag("home_search_button"),
          ) {
            Icon(
              imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
              contentDescription = "খুঁজুন",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    // Expandable Search Bar
    item {
      AnimatedVisibility(visible = showSearch) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নাম বা User ID লিখুন (যেমন: 1002)") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_search_input"),
          )
          if (searchQuery.trim().length >= 3) {
            Button(
              onClick = { onFindByIdRequested(searchQuery.trim()) },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AlaapBlue),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("'$searchQuery' ID দিয়ে নতুন ইউজার খুঁজুন")
            }
          }
        }
      }
    }

    // 4 Quick Action Cards Row (ভয়েস কল, ভিডিও কল, গ্রুপ কল, নতুন মেসেজ)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickActionCard(
          label = "ভয়েস কল",
          icon = Icons.Rounded.Call,
          circleColor = AlaapGreen,
          onClick = onQuickVoiceCall,
          modifier = Modifier.weight(1f).testTag("quick_voice_call"),
        )
        QuickActionCard(
          label = "ভিডিও কল",
          icon = Icons.Rounded.Videocam,
          circleColor = AlaapBlue,
          onClick = onQuickVideoCall,
          modifier = Modifier.weight(1f).testTag("quick_video_call"),
        )
        QuickActionCard(
          label = "গ্রুপ কল",
          icon = Icons.Rounded.Groups,
          circleColor = AlaapPurple,
          onClick = onQuickGroupCall,
          modifier = Modifier.weight(1f).testTag("quick_group_call"),
        )
        QuickActionCard(
          label = "নতুন মেসেজ",
          icon = Icons.Rounded.ChatBubble,
          circleColor = AlaapAmber,
          onClick = onQuickNewMessage,
          modifier = Modifier.weight(1f).testTag("quick_new_message"),
        )
      }
    }

    // Section Title: সাম্প্রতিক
    item {
      Text(
        text = "সাম্প্রতিক",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
      )
    }

    // State Handling
    when {
      chatsState is UiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = AlaapBlue)
          }
        }
      }
      chatsState is UiState.Error -> {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(18.dp),
          ) {
            Text(
              text = chatsState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      }
      recentItems.isEmpty() -> {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(AlaapBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChatBubble,
                  contentDescription = null,
                  tint = AlaapBlue,
                  modifier = Modifier.size(28.dp),
                )
              }
              Spacer(Modifier.height(12.dp))
              Text(
                text = "এখনো কোনো চ্যাট বা কল নেই",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(Modifier.height(6.dp))
              Text(
                text = "উপরের 'নতুন মেসেজ' বা 'ভয়েস কল' বাটনে চাপ দিয়ে বন্ধুর ID লিখে কল বা চ্যাট শুরু করুন। আপনার ID ($myShortId) বন্ধুকে দিন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
      else -> {
        items(recentItems, key = { item ->
          when (item) {
            is RecentFeedItem.ChatEntry -> "chat_${item.chat.id}"
            is RecentFeedItem.MissedCallEntry -> "call_${item.call.id}"
          }
        }) { item ->
          when (item) {
            is RecentFeedItem.ChatEntry -> RecentChatCard(
              chat = item.chat,
              myUid = myUid,
              onClick = { onOpenChat(item.chat.id) },
              onJoinCall = { onJoinGroupCall(item.chat.activeCallId) },
            )
            is RecentFeedItem.MissedCallEntry -> RecentMissedCallCard(
              call = item.call,
              onClick = { onCallbackMissed(item.call) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionCard(
  label: String,
  icon: ImageVector,
  circleColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.minimumInteractiveComponentSize(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(circleColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = Color.White,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun RecentChatCard(
  chat: ChatRoom,
  myUid: String,
  onClick: () -> Unit,
  onJoinCall: () -> Unit,
) {
  val title = chat.displayTitleFor(myUid)
  val otherTyping = chat.typingUids.any { it != myUid }
  val subtitle = when {
    otherTyping -> "টাইপ করছে..."
    chat.activeCallId.isNotBlank() -> "গ্রুপ কল চলছে · যোগ দিন"
    chat.lastMessageSenderId == myUid && chat.lastMessageType != "system" ->
      "আপনি: ${chat.lastMessageText}"
    else -> chat.lastMessageText
  }

  val badge = if (chat.groupChat) {
    title.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
  } else {
    chat.otherShortIdFor(myUid).takeLast(1).ifBlank { title.take(1) }
  }

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("recent_chat_${chat.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AlaapAvatar(
        name = title,
        badgeText = badge,
        seed = chat.id,
        overrideColor = if (chat.groupChat) AlaapPurple else AlaapBlue,
      )

      Spacer(Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = when {
            otherTyping -> AlaapBlue
            chat.activeCallId.isNotBlank() -> AlaapGreen
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(Modifier.width(10.dp))

      if (chat.activeCallId.isNotBlank()) {
        Button(
          onClick = onJoinCall,
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(containerColor = AlaapGreen),
          contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
          modifier = Modifier.height(36.dp).testTag("join_group_call_${chat.id}"),
        ) {
          Text(
            text = "Join",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
        }
      } else {
        Text(
          text = formatBengaliTime(chat.lastMessageAt),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RecentMissedCallCard(
  call: CallSession,
  onClick: () -> Unit,
) {
  val label = if (call.videoCall) "মিসড ভিডিও কল" else "মিসড ভয়েস কল"
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AlaapAvatar(
        name = call.callerName,
        badgeText = call.callerShortId.takeLast(1).ifBlank { call.callerName.take(1) },
        seed = call.callerId,
        overrideColor = AlaapAmber,
      )
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = call.callerName.ifBlank { "অজানা ইউজার" },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium,
          color = AlaapRed,
          fontWeight = FontWeight.Medium,
        )
      }
      Spacer(Modifier.width(10.dp))
      Text(
        text = formatBengaliTime(call.createdAt),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
