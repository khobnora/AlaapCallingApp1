package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.call.CallForegroundService
import com.example.call.WebRtcCallManager
import com.example.data.CallSession
import com.example.data.IceCandidateRecord
import com.example.ui.components.AlaapAvatar
import com.example.ui.components.formatDuration
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapRed
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun IncomingCallScreen(
  call: CallSession,
  onAccept: () -> Unit,
  onReject: () -> Unit,
) {
  val callLabel = when {
    call.groupCall && call.videoCall -> "ইনকামিং গ্রুপ ভিডিও কল..."
    call.groupCall -> "ইনকামিং গ্রুপ ভয়েস কল..."
    call.videoCall -> "ইনকামিং ভিডিও কল..."
    else -> "ইনকামিং ভয়েস কল..."
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B1120))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(28.dp),
  ) {
    Column(
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AlaapAvatar(
        name = call.callerName.ifBlank { "কলার" },
        size = 104.dp,
        seed = call.callerId,
      )
      Spacer(Modifier.height(22.dp))
      Text(
        text = if (call.groupCall) call.groupTitle.ifBlank { call.callerName } else call.callerName,
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = "ID: ${call.callerShortId}",
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 15.sp,
      )
      Spacer(Modifier.height(10.dp))
      Text(
        text = callLabel,
        color = Color(0xFF93C5FD),
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
      )
    }

    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(bottom = 36.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = CircleShape,
          color = AlaapRed,
          modifier = Modifier.size(72.dp),
        ) {
          IconButton(
            onClick = onReject,
            modifier = Modifier.testTag("incoming_reject_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.CallEnd,
              contentDescription = "রিজেক্ট",
              tint = Color.White,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("রিজেক্ট", color = Color.White, fontWeight = FontWeight.Medium)
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = CircleShape,
          color = AlaapGreen,
          modifier = Modifier.size(72.dp),
        ) {
          IconButton(
            onClick = onAccept,
            modifier = Modifier.testTag("incoming_accept_button"),
          ) {
            Icon(
              imageVector = if (call.videoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
              contentDescription = "রিসিভ",
              tint = Color.White,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("রিসিভ", color = Color.White, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@Composable
fun ActiveCallScreen(
  call: CallSession,
  myUid: String,
  durationSeconds: Long,
  iceCandidates: List<IceCandidateRecord>,
  onOfferCreated: (String) -> Unit,
  onAnswerCreated: (String) -> Unit,
  onIceCandidate: (sdpMid: String, sdpMLineIndex: Int, candidate: String) -> Unit,
  onEndCall: () -> Unit,
) {
  val context = LocalContext.current

  var isMuted by remember { mutableStateOf(false) }
  var isSpeakerOn by remember { mutableStateOf(call.videoCall) }
  var isCameraOn by remember { mutableStateOf(call.videoCall) }
  var permissionsGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
        (!call.videoCall || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { result ->
    permissionsGranted = result[Manifest.permission.RECORD_AUDIO] == true &&
      (!call.videoCall || result[Manifest.permission.CAMERA] == true)
  }

  LaunchedEffect(Unit) {
    if (!permissionsGranted) {
      val req = mutableListOf(Manifest.permission.RECORD_AUDIO)
      if (call.videoCall) req.add(Manifest.permission.CAMERA)
      permissionLauncher.launch(req.toTypedArray())
    }
  }

  val peerTitle = remember(call, myUid) {
    if (call.groupCall) {
      call.groupTitle.ifBlank { "গ্রুপ কল (${call.joinedUids.size})" }
    } else {
      val otherUid = call.participantUids.firstOrNull { it != myUid } ?: myUid
      call.participantNames[otherUid] ?: call.callerName.ifBlank { "কল" }
    }
  }

  val isCaller = call.callerId == myUid

  val rtcManager = remember(call.id) {
    WebRtcCallManager(
      context = context,
      onLocalOfferCreated = onOfferCreated,
      onLocalAnswerCreated = onAnswerCreated,
      onLocalIceCandidate = onIceCandidate,
    )
  }

  LaunchedEffect(permissionsGranted, call.id) {
    if (permissionsGranted) {
      rtcManager.start(enableVideo = call.videoCall, createOffer = isCaller)
      CallForegroundService.start(context, peerTitle, call.videoCall)
    }
  }

  // Handle remote offer/answer SDP
  LaunchedEffect(call.offerSdp, permissionsGranted) {
    if (permissionsGranted && !isCaller && call.offerSdp.isNotBlank()) {
      rtcManager.handleRemoteOfferAndCreateAnswer(call.offerSdp, call.videoCall)
    }
  }

  LaunchedEffect(call.answerSdp, permissionsGranted) {
    if (permissionsGranted && isCaller && call.answerSdp.isNotBlank()) {
      rtcManager.handleRemoteAnswer(call.answerSdp)
    }
  }

  LaunchedEffect(iceCandidates, permissionsGranted) {
    if (permissionsGranted && iceCandidates.isNotEmpty()) {
      rtcManager.addRemoteIceCandidates(iceCandidates, myUid)
    }
  }

  DisposableEffect(call.id) {
    onDispose {
      rtcManager.release()
      CallForegroundService.stop(context)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF172554), Color(0xFF090D16)))
      )
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    // Video renderers when video call is active
    if (call.videoCall && isCameraOn && permissionsGranted) {
      AndroidView(
        factory = { ctx ->
          SurfaceViewRenderer(ctx).apply {
            init(rtcManager.eglBase.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
            rtcManager.attachRemoteRenderer(this)
          }
        },
        modifier = Modifier.fillMaxSize(),
      )

      // Floating Local Preview PiP
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 72.dp, end = 16.dp)
          .size(width = 112.dp, height = 156.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Black),
      ) {
        AndroidView(
          factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
              init(rtcManager.eglBase.eglBaseContext, null)
              setMirror(true)
              setZOrderMediaOverlay(true)
              setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
              rtcManager.attachLocalRenderer(this)
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    // Center Voice Call / Connecting Avatar
    if (!call.videoCall || !isCameraOn || call.status == "ringing") {
      Column(
        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AlaapAvatar(
          name = peerTitle,
          size = 110.dp,
          seed = call.id,
        )
        Spacer(Modifier.height(20.dp))
        Text(
          text = peerTitle,
          color = Color.White,
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = when {
            call.status == "ringing" -> "রিং হচ্ছে..."
            call.groupCall -> "যুক্ত আছেন ${call.joinedUids.size} জন · ${formatDuration(durationSeconds)}"
            else -> formatDuration(durationSeconds)
          },
          color = Color(0xFF93C5FD),
          fontSize = 16.sp,
        )
      }
    } else {
      // Top status bar overlay in video mode
      Column(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 20.dp, top = 20.dp),
      ) {
        Text(
          text = peerTitle,
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (call.status == "ringing") "রিং হচ্ছে..." else formatDuration(durationSeconds),
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 14.sp,
        )
      }
    }

    // Bottom Call Controls Bar
    Surface(
      color = Color(0xCC111827),
      shape = RoundedCornerShape(28.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 28.dp)
        .fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CallControlButton(
          icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
          label = if (isMuted) "আনমিউট" else "মিউট",
          active = isMuted,
          onClick = {
            isMuted = !isMuted
            rtcManager.setMuted(isMuted)
          },
          modifier = Modifier.testTag("call_mute_button"),
        )

        CallControlButton(
          icon = if (isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
          label = "স্পিকার",
          active = isSpeakerOn,
          onClick = {
            isSpeakerOn = !isSpeakerOn
            rtcManager.setSpeakerEnabled(isSpeakerOn)
          },
          modifier = Modifier.testTag("call_speaker_button"),
        )

        CallControlButton(
          icon = if (isCameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
          label = "ক্যামেরা",
          active = isCameraOn,
          onClick = {
            isCameraOn = !isCameraOn
            rtcManager.setCameraEnabled(isCameraOn)
          },
          modifier = Modifier.testTag("call_camera_button"),
        )

        if (isCameraOn) {
          CallControlButton(
            icon = Icons.Rounded.FlipCameraAndroid,
            label = "সুইচ",
            active = false,
            onClick = { rtcManager.switchCamera() },
            modifier = Modifier.testTag("call_switch_camera_button"),
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            shape = CircleShape,
            color = AlaapRed,
            modifier = Modifier.size(54.dp),
          ) {
            IconButton(
              onClick = onEndCall,
              modifier = Modifier.testTag("call_end_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "কল কাটুন",
                tint = Color.White,
              )
            }
          }
          Spacer(Modifier.height(4.dp))
          Text("কাটুন", color = Color.White, fontSize = 11.sp)
        }
      }
    }
  }
}

@Composable
private fun CallControlButton(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      shape = CircleShape,
      color = if (active) Color.White else Color.White.copy(alpha = 0.16f),
      modifier = Modifier.size(50.dp),
    ) {
      IconButton(onClick = onClick, modifier = modifier) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (active) Color(0xFF111827) else Color.White,
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(label, color = Color.White, fontSize = 11.sp)
  }
}
