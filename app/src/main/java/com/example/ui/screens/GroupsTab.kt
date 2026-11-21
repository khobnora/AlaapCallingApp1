package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ChatRoom
import com.example.data.UiState
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.toBengaliDigits
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapPurple

@Composable
fun GroupsTab(
  chatsState: UiState<List<ChatRoom>>,
  onCreateGroupClick: () -> Unit,
  onOpenGroupChat: (String) -> Unit,
  onStartGroupCall: (ChatRoom, Boolean) -> Unit,
  onJoinActiveGroupCall: (String) -> Unit,
) {
  val groups = ((chatsState as? UiState.Success)?.data ?: emptyList()).filter { it.groupChat }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "গ্রুপসমূহ",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
          onClick = onCreateGroupClick,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AlaapPurple),
          modifier = Modifier.testTag("create_group_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("নতুন গ্রুপ")
        }
      }
    }

    when (chatsState) {
      is UiState.Loading -> {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlaapPurple)
          }
        }
      }
      is UiState.Error -> {
        item {
          Text(chatsState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        if (groups.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Groups,
                  contentDescription = null,
                  tint = AlaapPurple,
                  modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text("কোনো গ্রুপ তৈরি করা হয়নি", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                  text = "পরিবার বা বন্ধুদের নিয়ে গ্রুপ বানিয়ে এক ক্লিকে সবাইকে ভয়েস বা ভিডিও কল করুন।",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(groups, key = { it.id }) { group ->
            Card(
              onClick = { onOpenGroupChat(group.id) },
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
                  name = group.title,
                  badgeText = group.title.take(1).uppercase(),
                  seed = group.id,
                  overrideColor = AlaapPurple,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = group.title.ifBlank { "গ্রুপ" },
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${toBengaliDigits(group.memberUids.size.toString())} জন সদস্য",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (group.activeCallId.isNotBlank()) {
                    Text(
                      text = "গ্রুপ কল চলছে",
                      style = MaterialTheme.typography.labelMedium,
                      color = AlaapGreen,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                if (group.activeCallId.isNotBlank()) {
                  Button(
                    onClick = { onJoinActiveGroupCall(group.activeCallId) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AlaapGreen),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                  ) {
                    Text("Join", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                } else {
                  IconButton(onClick = { onStartGroupCall(group, false) }) {
                    Icon(
                      imageVector = Icons.Rounded.Call,
                      contentDescription = "গ্রুপ ভয়েস কল",
                      tint = AlaapGreen,
                    )
                  }
                  IconButton(onClick = { onStartGroupCall(group, true) }) {
                    Icon(
                      imageVector = Icons.Rounded.Videocam,
                      contentDescription = "গ্রুপ ভিডিও কল",
                      tint = AlaapBlue,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
