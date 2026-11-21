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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CallSession
import com.example.data.UiState
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.formatBengaliTime
import com.example.ui.components.formatDuration
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapRed

@Composable
fun CallsHistoryTab(
  myUid: String,
  callsState: UiState<List<CallSession>>,
  onRedialCall: (CallSession, Boolean) -> Unit,
) {
  var selectedFilter by remember { mutableIntStateOf(0) }
  val filters = listOf("সব কল", "মিসড", "ইনকামিং", "আউটগোয়িং")

  val calls = (callsState as? UiState.Success)?.data ?: emptyList()
  val filtered = remember(calls, selectedFilter, myUid) {
    when (selectedFilter) {
      1 -> calls.filter { it.callerId != myUid && (it.status == "missed" || it.status == "rejected") }
      2 -> calls.filter { it.callerId != myUid }
      3 -> calls.filter { it.callerId == myUid }
      else -> calls
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "কল হিস্ট্রি",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters.size) { index ->
          FilterChip(
            selected = selectedFilter == index,
            onClick = { selectedFilter = index },
            label = { Text(filters[index]) },
          )
        }
      }
    }

    when (callsState) {
      is UiState.Loading -> {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AlaapBlue)
          }
        }
      }
      is UiState.Error -> {
        item {
          Text(
            text = callsState.message,
            color = AlaapRed,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      is UiState.Success -> {
        if (filtered.isEmpty()) {
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
                  imageVector = Icons.Rounded.Call,
                  contentDescription = null,
                  tint = AlaapGreen,
                  modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                  text = "কোনো কল রেকর্ড নেই",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "নিচের সবুজ কল বাটন দিয়ে যেকোনো ID-তে কল করুন",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(filtered, key = { it.id }) { call ->
            CallHistoryItemCard(
              call = call,
              myUid = myUid,
              onVoiceRedial = { onRedialCall(call, false) },
              onVideoRedial = { onRedialCall(call, true) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CallHistoryItemCard(
  call: CallSession,
  myUid: String,
  onVoiceRedial: () -> Unit,
  onVideoRedial: () -> Unit,
) {
  val isOutgoing = call.callerId == myUid
  val isMissed = !isOutgoing && (call.status == "missed" || call.status == "rejected")
  val peerUid = call.participantUids.firstOrNull { it != myUid } ?: myUid
  val displayTitle = when {
    call.groupCall -> call.groupTitle.ifBlank { "গ্রুপ কল" }
    isOutgoing -> call.participantNames[peerUid] ?: "আলাপ ইউজার"
    else -> call.callerName.ifBlank { "আলাপ ইউজার" }
  }

  val directionText = when {
    isMissed -> "মিসড কল"
    isOutgoing -> "আউটগোয়িং কল"
    else -> "ইনকামিং কল"
  }
  val durationLabel = if (call.durationSeconds > 0L) " · ${formatDuration(call.durationSeconds)}" else ""

  Card(
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
        name = displayTitle,
        seed = peerUid,
      )
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = displayTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when {
              isMissed -> Icons.AutoMirrored.Rounded.CallMissed
              isOutgoing -> Icons.AutoMirrored.Rounded.CallMade
              else -> Icons.AutoMirrored.Rounded.CallReceived
            },
            contentDescription = null,
            tint = if (isMissed) AlaapRed else AlaapGreen,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = "$directionText$durationLabel · ${formatBengaliTime(call.createdAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isMissed) AlaapRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isMissed) FontWeight.Medium else FontWeight.Normal,
          )
        }
      }

      IconButton(onClick = onVoiceRedial) {
        Icon(
          imageVector = Icons.Rounded.Call,
          contentDescription = "ভয়েস কল",
          tint = AlaapGreen,
        )
      }
      IconButton(onClick = onVideoRedial) {
        Icon(
          imageVector = Icons.Rounded.Videocam,
          contentDescription = "ভিডিও কল",
          tint = AlaapBlue,
        )
      }
    }
  }
}
