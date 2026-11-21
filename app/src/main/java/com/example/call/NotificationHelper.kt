package com.example.call

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CallSession

object NotificationHelper {
  const val CALL_CHANNEL_ID = "alaap_incoming_call_channel"
  const val ACTIVE_CALL_CHANNEL_ID = "alaap_active_call_channel"
  const val MESSAGE_CHANNEL_ID = "alaap_message_channel"

  const val INCOMING_CALL_NOTIF_ID = 1001
  const val ACTIVE_CALL_NOTIF_ID = 1002
  const val EXTRA_CALL_ID = "extra_call_id"
  const val EXTRA_CHAT_ID = "extra_chat_id"
  const val EXTRA_ACTION = "extra_action"
  const val ACTION_ACCEPT_CALL = "action_accept_call"
  const val ACTION_REJECT_CALL = "action_reject_call"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val callChannel = NotificationChannel(
      CALL_CHANNEL_ID,
      "ইনকামিং কল",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "ভয়েস ও ভিডিও কলের ফুল-স্ক্রিন রিং নোটিফিকেশন"
      enableVibration(true)
      vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
      setSound(
        ringtoneUri,
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }

    val activeChannel = NotificationChannel(
      ACTIVE_CALL_CHANNEL_ID,
      "চলমান কল",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "কল চলাকালীন ব্যাকগ্রাউন্ড সার্ভিস স্ট্যাটাস"
    }

    val msgChannel = NotificationChannel(
      MESSAGE_CHANNEL_ID,
      "নতুন মেসেজ",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "চ্যাট ও গ্রুপ মেসেজের নোটিফিকেশন"
      enableVibration(true)
    }

    manager.createNotificationChannels(listOf(callChannel, activeChannel, msgChannel))
  }

  private fun canPostNotifications(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
  }

  fun showIncomingCallNotification(context: Context, call: CallSession) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_CALL_ID, call.id)
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      context,
      2001,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val acceptIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_CALL_ID, call.id)
      putExtra(EXTRA_ACTION, ACTION_ACCEPT_CALL)
    }
    val acceptPendingIntent = PendingIntent.getActivity(
      context,
      2002,
      acceptIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val rejectIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_CALL_ID, call.id)
      putExtra(EXTRA_ACTION, ACTION_REJECT_CALL)
    }
    val rejectPendingIntent = PendingIntent.getActivity(
      context,
      2003,
      rejectIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val callTypeText = when {
      call.groupCall && call.videoCall -> "ইনকামিং গ্রুপ ভিডিও কল"
      call.groupCall -> "ইনকামিং গ্রুপ ভয়েস কল"
      call.videoCall -> "ইনকামিং ভিডিও কল"
      else -> "ইনকামিং ভয়েস কল"
    }

    val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(call.callerName.ifBlank { "অজানা কলার" })
      .setContentText("$callTypeText (ID: ${call.callerShortId})")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .setContentIntent(fullScreenPendingIntent)
      .addAction(android.R.drawable.ic_menu_call, "রিসিভ", acceptPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "রিজেক্ট", rejectPendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(INCOMING_CALL_NOTIF_ID, notification)
  }

  fun cancelIncomingCallNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIF_ID)
  }

  fun showMessageNotification(
    context: Context,
    senderName: String,
    messageText: String,
    chatId: String,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_CHAT_ID, chatId)
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      chatId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(senderName)
      .setContentText(messageText)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(chatId.hashCode(), notification)
  }
}
