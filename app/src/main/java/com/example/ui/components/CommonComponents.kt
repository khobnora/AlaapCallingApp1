package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChatRoom
import com.example.ui.theme.AlaapAmber
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapPurple
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val avatarPalette = listOf(
  AlaapBlue,
  AlaapPurple,
  AlaapAmber,
  AlaapGreen,
  Color(0xFF0E7490),
)

@Composable
fun AlaapAvatar(
  name: String,
  photoUrl: String = "",
  badgeText: String = "",
  size: Dp = 48.dp,
  seed: String = name,
  overrideColor: Color? = null,
) {
  val bgColor = overrideColor ?: avatarPalette[abs(seed.hashCode()) % avatarPalette.size]
  val initial = badgeText.ifBlank {
    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "আ"
  }

  val decodedBitmap = remember(photoUrl) {
    if (photoUrl.startsWith("data:image") || (photoUrl.length > 200 && !photoUrl.startsWith("http"))) {
      decodeBase64Bitmap(photoUrl)
    } else null
  }

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(bgColor),
    contentAlignment = Alignment.Center,
  ) {
    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = "$name ছবি",
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(size),
        )
      }
      photoUrl.startsWith("http") -> {
        AsyncImage(
          model = photoUrl,
          contentDescription = "$name ছবি",
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(size),
        )
      }
      else -> {
        Text(
          text = initial,
          color = Color.White,
          fontSize = (size.value * 0.4f).sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

fun ChatRoom.displayTitleFor(myUid: String): String {
  if (groupChat) return title.ifBlank { "গ্রুপ চ্যাট" }
  val otherUid = memberUids.firstOrNull { it != myUid } ?: myUid
  return memberNames[otherUid]?.ifBlank { "আলাপ ইউজার" } ?: title.ifBlank { "আলাপ ইউজার" }
}

fun ChatRoom.otherShortIdFor(myUid: String): String {
  if (groupChat) return ""
  val otherUid = memberUids.firstOrNull { it != myUid } ?: return ""
  return memberShortIds[otherUid] ?: ""
}

fun formatBengaliTime(timestamp: Timestamp?): String {
  if (timestamp == null) return "এখন"
  val date = timestamp.toDate()
  val cal = Calendar.getInstance().apply { time = date }
  val now = Calendar.getInstance()
  val sameDay = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

  val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
  val isYesterday = cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
    cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

  val raw = when {
    sameDay -> SimpleDateFormat("HH:mm", Locale.US).format(date)
    isYesterday -> "গতকাল"
    else -> SimpleDateFormat("dd/MM", Locale.US).format(date)
  }
  return toBengaliDigits(raw)
}

fun formatDuration(seconds: Long): String {
  val mins = seconds / 60
  val secs = seconds % 60
  return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun toBengaliDigits(input: String): String {
  val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
  val sb = StringBuilder(input.length)
  for (ch in input) {
    if (ch in '0'..'9') sb.append(bn[ch - '0']) else sb.append(ch)
  }
  return sb.toString()
}

fun compressUriToBase64(context: Context, uri: Uri, maxBytes: Int = 180_000): String? {
  return try {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(input)
    input.close()
    original ?: return null

    val maxDim = 720
    val scale = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height))
    val resized = if (scale < 1f) {
      Bitmap.createScaledBitmap(
        original,
        (original.width * scale).toInt().coerceAtLeast(1),
        (original.height * scale).toInt().coerceAtLeast(1),
        true,
      )
    } else original

    var quality = 78
    var bytes: ByteArray
    do {
      val baos = ByteArrayOutputStream()
      resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
      bytes = baos.toByteArray()
      quality -= 12
    } while (bytes.size > maxBytes && quality >= 30)

    Base64.encodeToString(bytes, Base64.NO_WRAP)
  } catch (_: Exception) {
    null
  }
}

fun readFileToBase64(context: Context, uri: Uri, maxRawBytes: Int = 380_000): Pair<String, String>? {
  return try {
    var fileName = "attachment"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (idx >= 0 && cursor.moveToFirst()) {
        fileName = cursor.getString(idx) ?: "attachment"
      }
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.size > maxRawBytes) return Pair("TOO_LARGE", fileName)
    Pair(Base64.encodeToString(bytes, Base64.NO_WRAP), fileName)
  } catch (_: Exception) {
    null
  }
}

fun decodeBase64Bitmap(raw: String): Bitmap? {
  return try {
    val clean = raw.substringAfter("base64,", raw)
    val bytes = Base64.decode(clean, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  } catch (_: Exception) {
    null
  }
}
