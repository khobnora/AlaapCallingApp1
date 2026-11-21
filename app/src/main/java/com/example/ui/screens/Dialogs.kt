package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ShortIdLookup
import com.example.data.UserContact
import com.example.data.UserProfile
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.compressUriToBase64
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapRed

enum class PeerActionMode {
  ANY,
  VOICE_CALL,
  VIDEO_CALL,
  MESSAGE,
}

@Composable
fun FindPeerDialog(
  mode: PeerActionMode,
  initialQuery: String = "",
  contacts: List<UserContact>,
  isLoading: Boolean,
  foundPeer: ShortIdLookup?,
  errorText: String?,
  onSearchId: (String) -> Unit,
  onVoiceCallPeer: (ShortIdLookup) -> Unit,
  onVideoCallPeer: (ShortIdLookup) -> Unit,
  onMessagePeer: (ShortIdLookup) -> Unit,
  onDismiss: () -> Unit,
) {
  var idInput by remember(initialQuery) { mutableStateOf(initialQuery) }

  val dialogTitle = when (mode) {
    PeerActionMode.VOICE_CALL -> "ভয়েস কল করুন"
    PeerActionMode.VIDEO_CALL -> "ভিডিও কল করুন"
    PeerActionMode.MESSAGE -> "নতুন মেসেজ পাঠান"
    PeerActionMode.ANY -> "ID দিয়ে খুঁজুন"
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(dialogTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = idInput,
            onValueChange = { idInput = it },
            label = { Text("বন্ধুর User ID (যেমন: 1002)") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("peer_id_input"),
          )
          Spacer(Modifier.width(8.dp))
          IconButton(
            onClick = { onSearchId(idInput) },
            modifier = Modifier.testTag("peer_search_submit"),
          ) {
            Icon(Icons.Rounded.Search, contentDescription = "খুঁজুন", tint = AlaapBlue)
          }
        }

        if (isLoading) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), color = AlaapBlue)
          }
        }

        if (errorText != null) {
          Text(errorText, color = AlaapRed, style = MaterialTheme.typography.bodyMedium)
        }

        if (foundPeer != null) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                AlaapAvatar(name = foundPeer.displayName, photoUrl = foundPeer.photoUrl, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(foundPeer.displayName, style = MaterialTheme.typography.titleMedium)
                  Text("ID: ${foundPeer.shortId}", style = MaterialTheme.typography.bodyMedium)
                }
              }
              Spacer(Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { onVoiceCallPeer(foundPeer) },
                  colors = ButtonDefaults.buttonColors(containerColor = AlaapGreen),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("কল")
                }
                Button(
                  onClick = { onVideoCallPeer(foundPeer) },
                  colors = ButtonDefaults.buttonColors(containerColor = AlaapBlue),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("ভিডিও")
                }
                OutlinedButton(
                  onClick = { onMessagePeer(foundPeer) },
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        if (contacts.isNotEmpty()) {
          Text(
            text = "সংরক্ষিত কন্টাক্ট",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LazyColumn(
            modifier = Modifier.height(180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(contacts, key = { it.contactUid }) { contact ->
              val asLookup = ShortIdLookup(
                userId = contact.contactUid,
                shortId = contact.shortId,
                displayName = contact.displayName,
                photoUrl = contact.photoUrl,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    when (mode) {
                      PeerActionMode.VOICE_CALL -> onVoiceCallPeer(asLookup)
                      PeerActionMode.VIDEO_CALL -> onVideoCallPeer(asLookup)
                      PeerActionMode.MESSAGE -> onMessagePeer(asLookup)
                      PeerActionMode.ANY -> onMessagePeer(asLookup)
                    }
                  }
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AlaapAvatar(name = contact.displayName, photoUrl = contact.photoUrl, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(contact.displayName, style = MaterialTheme.typography.titleMedium)
                  Text("ID: ${contact.shortId}", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onVoiceCallPeer(asLookup) }) {
                  Icon(Icons.Rounded.Call, contentDescription = "কল", tint = AlaapGreen)
                }
                IconButton(onClick = { onVideoCallPeer(asLookup) }) {
                  Icon(Icons.Rounded.Videocam, contentDescription = "ভিডিও", tint = AlaapBlue)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
    },
  )
}

@Composable
fun EditProfileDialog(
  profile: UserProfile,
  onSave: (name: String, shortId: String, phone: String, photoUrl: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(profile.displayName) }
  var shortId by remember { mutableStateOf(profile.shortId) }
  var phone by remember { mutableStateOf(profile.phone) }
  var photoUrl by remember { mutableStateOf(profile.photoUrl) }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    uri?.let {
      val b64 = compressUriToBase64(context, it, maxBytes = 120_000)
      if (b64 != null) {
        photoUrl = "data:image/jpeg;base64,$b64"
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("প্রোফাইল এডিট করুন") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AlaapAvatar(name = name.ifBlank { "আ" }, photoUrl = photoUrl, size = 76.dp)
        OutlinedButton(
          onClick = {
            photoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("প্রোফাইল ছবি পরিবর্তন")
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("নাম") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = shortId,
          onValueChange = { shortId = it },
          label = { Text("ইউনিক ID (যেমন: 1001)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("ফোন নম্বর (ঐচ্ছিক)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, shortId, phone, photoUrl) },
        colors = ButtonDefaults.buttonColors(containerColor = AlaapBlue),
      ) {
        Text("সেভ করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("বাতিল") }
    },
  )
}
