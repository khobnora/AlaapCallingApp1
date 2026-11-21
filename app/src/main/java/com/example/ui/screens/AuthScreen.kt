package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleAuthHelper
import com.example.ui.theme.AlaapBlue
import com.example.ui.theme.AlaapGreen
import com.example.ui.theme.AlaapPurple

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    GoogleAuthHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(AlaapBlue, Color(0xFF3B82F6)))
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = "আলাপ লোগো",
            tint = Color.White,
            modifier = Modifier.size(42.dp),
          )
        }

        Spacer(Modifier.height(20.dp))

        Text(
          text = "আলাপ",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = "সহজ ও নিরাপদ ভয়েস, ভিডিও, গ্রুপ কল এবং মেসেজিং",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            FeatureRow(
              icon = Icons.Rounded.Videocam,
              tint = AlaapBlue,
              title = "১-টু-১ ভয়েস ও ভিডিও কল",
              subtitle = "ইউনিক User ID দিয়ে কল করুন, রিং ও ফুল-স্ক্রিন অ্যালার্ট",
            )
            FeatureRow(
              icon = Icons.Rounded.Groups,
              tint = AlaapPurple,
              title = "গ্রুপ চ্যাট ও গ্রুপ কল",
              subtitle = "এক বাটনে সবাইকে কল দিন এবং চলমান কলে Join করুন",
            )
            FeatureRow(
              icon = Icons.Rounded.ChatBubble,
              tint = AlaapGreen,
              title = "রিয়েল-টাইম মেসেজ ও ছবি",
              subtitle = "টাইপিং ইন্ডিকেটর, সিন টিক, ছবি ও ফাইল শেয়ারিং",
            )
          }
        }

        Spacer(Modifier.height(24.dp))

        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
          ) {
            Text(
              text = errorMessage!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            GoogleAuthHelper.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
                errorMessage = "Google সাইন-ইন বাতিল হয়েছে বা এই APK-এর সাইনিং কী (SHA-1) Firebase-এ ম্যাচ করেনি। AI Studio থেকে সরাসরি APK বানিয়ে চেষ্টা করুন।"
              },
            )
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AlaapBlue),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color.White,
              strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text("সাইন ইন হচ্ছে...")
          } else {
            Text(
              text = "Google দিয়ে সাইন ইন করুন",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: ImageVector,
  tint: Color,
  title: String,
  subtitle: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(tint.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
