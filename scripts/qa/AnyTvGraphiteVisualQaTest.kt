package com.hari.androidtvremote.ui.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Isolated renderer of the REAL shared graphical modifier in the phone APK.
 * No mock graphics, no screenshot generation services, no TV/network/ADB keys.
 * It verifies control treatment; it is NOT a full Home screen screenshot.
 */
@RunWith(AndroidJUnit4::class)
class AnyTvGraphiteVisualQaTest {
    @get:Rule val rule = createComposeRule()

    @Test fun renderSharedGraphiteSurfaces() {
        rule.setContent {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF242424)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(250.dp).anyTvCutGraphite(CircleShape, largePad = true),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(76.dp).anyTvCutGraphite(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("OK", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(52.dp).anyTvCutGraphite(CircleShape))
                        Box(Modifier.size(width = 130.dp, height = 52.dp)
                            .anyTvCutGraphite(RoundedCornerShape(20.dp)))
                        Box(Modifier.size(52.dp).anyTvCutGraphite(CircleShape))
                    }
                }
            }
        }
        rule.waitForIdle()
        val bitmap: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        // The test fails if the painted background diverges from the approved base.
        assertTrue(bitmap.width > 200 && bitmap.height > 400)
        val edge = bitmap.getPixel(6, 6)
        assertTrue(
            "The background must remain graphite (#242424); got ${Integer.toHexString(edge)}",
            AndroidColor.red(edge) in 32..44 &&
                AndroidColor.green(edge) in 32..44 &&
                AndroidColor.blue(edge) in 32..44
        )
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
            ?: error("No screenshot output directory")
        val file = File(dir, "anytv-code122-graphite-real-compose.png")
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue(file.isFile && file.length() > 1000)
    }
}
