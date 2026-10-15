package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.anytv_wordmark),
            contentDescription = "AnyTV — Remote Android Control",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(.90f).height(310.dp)
        )
        Text(
            text = "v$installedVersion",
            color = Color(0xFF9095A4),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp)
        )
    }
}
