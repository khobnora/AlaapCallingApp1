package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.call.NotificationHelper
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.AlaapTheme

class MainActivity : ComponentActivity() {
  private var pendingCallId by mutableStateOf<String?>(null)
  private var pendingAction by mutableStateOf<String?>(null)
  private var pendingChatId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
    }
    extractNotificationExtras(intent)
    setContent {
      AlaapTheme {
        AppNavigation(
          pendingCallId = pendingCallId,
          pendingAction = pendingAction,
          pendingChatId = pendingChatId,
          onClearPendingIntents = {
            pendingCallId = null
            pendingAction = null
            pendingChatId = null
          },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    extractNotificationExtras(intent)
  }

  private fun extractNotificationExtras(intent: Intent?) {
    intent ?: return
    val callId = intent.getStringExtra(NotificationHelper.EXTRA_CALL_ID)
    val action = intent.getStringExtra(NotificationHelper.EXTRA_ACTION)
    val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
    if (!callId.isNullOrBlank()) {
      pendingCallId = callId
      pendingAction = action
    }
    if (!chatId.isNullOrBlank()) {
      pendingChatId = chatId
    }
  }
}
