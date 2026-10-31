package com.hari.androidtvremote.ui.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Persist the guest choice independently of credentials or any TV pairing code. */
internal object AnyTvEntryPreference {
    private const val PREFS = "anytv_account_entry"
    private const val MODE = "entry_mode"
    fun isGuest(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODE, "") == "guest"
    fun selectGuest(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(MODE, "guest").commit()
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(MODE).apply()
    }
}

/**
 * Entry choice only. onSignIn MUST launch a working authenticated route.
 * Do not connect that button to a mock login or collect passwords in this screen.
 */
@Composable
internal fun AnyTvAccountEntry(
    onContinueAsGuest: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF09090B))
            .padding(horizontal = 28.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AnyTV", fontSize = 38.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFFF1744))
        Spacer(Modifier.height(24.dp))
        Text("Como deseja continuar?", fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold, color = Color.White,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Seus filmes e séries do seu jeito.", fontSize = 14.sp,
            color = Color(0xFFBBB5B9), textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
        ) { Text("Continuar sem login", color = Color.White) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Entrar ou criar conta", color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("Você pode criar uma conta depois, sem perder seus dados locais.",
            color = Color(0xFFBBB5B9), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
