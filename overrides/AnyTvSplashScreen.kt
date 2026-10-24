package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hari.androidtvremote.R
import kotlinx.coroutines.delay

/** The approved AnyTV artwork and current installed version are splash-only. */
@Composable
fun SplashRoute(
    showOnboarding: Boolean,
    onFinished: (Boolean) -> Unit
) {
    LaunchedEffect(showOnboarding) {
        delay(1800L)
        onFinished(showOnboarding)
    }
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
                .versionName.orEmpty().substringBefore("-")
        }.getOrDefault("?")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(\n            listOf(Color(0xFF18020A), Color.Black, Color.Black)))
            .statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.anytv_wordmark),
                contentDescription = "AnyTV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(.84f).height(255.dp)
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                color = Color(0xFFFF1744),
                trackColor = Color(0xFF4E1426),
                modifier = Modifier.fillMaxWidth(.54f).height(4.dp)
            )
            Spacer(Modifier.height(23.dp))
            Text(
                text = "CONECTANDO SEU ANDROID\nDE QUALQUER LUGAR",
                textAlign = TextAlign.Center,
                color = Color(0xFFD5CFD2), fontSize = 11.sp, lineHeight = 18.sp
            )
        }
        Text(
            text = "v$installedVersion",
            color = Color(0xFF9095A4),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp)
        )
    }
}
