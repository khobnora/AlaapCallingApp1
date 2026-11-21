package com.example.call

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class CallForegroundService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val peerName = intent?.getStringExtra(EXTRA_PEER_NAME) ?: "আলাপ কল"
    val isVideo = intent?.getBooleanExtra(EXTRA_IS_VIDEO, false) ?: false

    NotificationHelper.ensureChannels(this)
    val openIntent = Intent(this, MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      3001,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val subtitle = if (isVideo) "ভিডিও কল চলছে · ট্যাপ করে ফিরুন" else "ভয়েস কল চলছে · ট্যাপ করে ফিরুন"
    val notification = NotificationCompat.Builder(this, NotificationHelper.ACTIVE_CALL_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(peerName)
      .setContentText(subtitle)
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setContentIntent(pendingIntent)
      .build()

    startForeground(NotificationHelper.ACTIVE_CALL_NOTIF_ID, notification)
    return START_NOT_STICKY
  }

  companion object {
    private const val EXTRA_PEER_NAME = "extra_peer_name"
    private const val EXTRA_IS_VIDEO = "extra_is_video"

    fun start(context: Context, peerName: String, isVideo: Boolean) {
      val intent = Intent(context, CallForegroundService::class.java).apply {
        putExtra(EXTRA_PEER_NAME, peerName)
        putExtra(EXTRA_IS_VIDEO, isVideo)
      }
      try {
        ContextCompat.startForegroundService(context, intent)
      } catch (_: Exception) {
      }
    }

    fun stop(context: Context) {
      try {
        context.stopService(Intent(context, CallForegroundService::class.java))
      } catch (_: Exception) {
      }
    }
  }
}
