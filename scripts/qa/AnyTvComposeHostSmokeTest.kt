package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PROOF OF TEST HARNESS ONLY. Exercises real Compose LaunchedEffect,
 * snapshot state, recomposition and semantics in Robolectric on local JVM.
 * It DOES NOT execute AnyTV's production genre-screen LaunchedEffect,
 * does not demonstrate R03 is fixed, and does not render its existing UI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnyTvComposeHostSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualComposeEffectRunsAgainWhenRetryStateChanges() {
        compose.setContent {
            var retry by remember { mutableIntStateOf(0) }
            var fired by remember { mutableIntStateOf(0) }
            LaunchedEffect(retry) {
                fired += 1
            }
            Column {
                Text("effect-fires-$fired")
                Button(onClick = { retry += 1 }) {
                    Text("retry")
                }
            }
        }
        compose.onNodeWithText("effect-fires-1").assertExists()
        compose.onNodeWithText("retry").performClick()
        compose.onNodeWithText("effect-fires-2").assertExists()
        println("ANYTV_QA_ROBOLECTRIC_COMPOSE_LAUNCHEDEFFECT_HOST_PASS")
    }
}
