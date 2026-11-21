package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.UserProfile
import com.example.ui.components.AlaapAvatar
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapRed

@Composable
fun SettingsTab(
  profile: UserProfile?,
  onEditProfileClick: () -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
  onToggleRingtone: (Boolean) -> Unit,
  onCopyFeedback: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  var permissionRefreshTick by remember { mutableStateOf(0) }
  val permsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) {
    permissionRefreshTick++
  }

  val hasMic = remember(permissionRefreshTick) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
  }
  val hasCam = remember(permissionRefreshTick) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  }
  val hasNotif = remember(permissionRefreshTick) {
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "সেটিংস",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    // Profile Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            AlaapAvatar(
              name = profile?.displayName ?: "আলাপ",
              photoUrl = profile?.photoUrl ?: "",
              size = 64.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = profile?.displayName?.ifBlank { "আলাপ ইউজার" } ?: "লোড হচ্ছে...",
                style = MaterialTheme.typography.titleLarge,
              )
              Spacer(Modifier.height(4.dp))
              Row(
                modifier = Modifier.clickable {
                  val id = profile?.shortId ?: return@clickable
                  clipboard.setText(AnnotatedString(id))
                  onCopyFeedback("আপনার ID ($id) কপি হয়েছে")
                },
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "আমার ID: ${profile?.shortId ?: "..."}",
                  style = MaterialTheme.typography.labelLarge,
                  color = AlaapBlue,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Rounded.ContentCopy,
                  contentDescription = "কপি করুন",
                  tint = AlaapBlue,
                  modifier = Modifier.size(16.dp),
                )
              }
              if (!profile?.phone.isNullOrBlank()) {
                Text(
                  text = profile!!.phone,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          Spacer(Modifier.height(16.dp))
          OutlinedButton(
            onClick = onEditProfileClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_button"),
          ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("প্রোফাইল, ছবি ও ID এডিট করুন")
          }
        }
      }
    }

    // Notification & Ringtone Settings
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = AlaapBlue)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("কল ও মেসেজ নোটিফিকেশন", style = MaterialTheme.typography.titleMedium)
              Text(
                "অ্যাপ বন্ধ বা লক থাকলেও অ্যালার্ট পাবেন",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = profile?.notificationsEnabled ?: true,
              onCheckedChange = onToggleNotifications,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.RingVolume, contentDescription = null, tint = AlaapGreen)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("ইনকামিং কল রিংটোন", style = MaterialTheme.typography.titleMedium)
              Text(
                "কল এলে রিং বাজবে ও ভাইব্রেট করবে",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = profile?.callRingtoneEnabled ?: true,
              onCheckedChange = onToggleRingtone,
            )
          }
        }
      }
    }

    // Permissions Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = AlaapBlue)
            Spacer(Modifier.width(10.dp))
            Text("অ্যাপ পারমিশন", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "মাইক্রোফোন: ${if (hasMic) "চালু" else "বন্ধ"} · ক্যামেরা: ${if (hasCam) "চালু" else "বন্ধ"} · নোটিফিকেশন: ${if (hasNotif) "চালু" else "বন্ধ"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (!hasMic || !hasCam || !hasNotif) {
            Button(
              onClick = {
                val list = mutableListOf(
                  Manifest.permission.RECORD_AUDIO,
                  Manifest.permission.CAMERA,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permsLauncher.launch(list.toTypedArray())
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AlaapBlue),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("প্রয়োজনীয় পারমিশন দিন")
            }
          }
        }
      }
    }

    // Logout Button
    item {
      Button(
        onClick = onSignOut,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AlaapRed),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("logout_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("লগআউট করুন")
      }
    }
  }
}
