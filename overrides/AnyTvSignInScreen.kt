package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Presentation only: handlers must be backed by the installed Supabase Auth SDK.
 * Never persist credentials, report fake sign-in, or activate this screen by itself.
 */
@Composable
internal fun AnyTvSignInScreen(
    busy: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onResetPassword: (String) -> Unit,
    onGuest: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }
    val valid = email.contains("@") && email.substringAfter("@").contains(".") &&
        password.length >= 6
    Column(
        Modifier.fillMaxSize().background(Color(0xFF09090B))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AnyTV", style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFF1744))
        Spacer(Modifier.height(24.dp))
        Text(if (registering) "Criar conta" else "Entrar na sua conta",
            color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = email, onValueChange = { email = it.take(254) },
            label = { Text("E-mail") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it.take(128) },
            label = { Text("Senha") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (registering) onSignUp(email.trim(), password)
            else onSignIn(email.trim(), password)
        }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (registering) "Criar conta" else "Entrar")
        }
        OutlinedButton(onClick = onGoogle, enabled = !busy,
            modifier = Modifier.fillMaxWidth()) { Text("Continuar com Google") }
        TextButton(onClick = { registering = !registering }, enabled = !busy) {
            Text(if (registering) "Já tenho conta" else "Criar uma conta")
        }
        TextButton(onClick = { if (email.contains("@")) onResetPassword(email.trim()) },
            enabled = !busy && email.contains("@")) {
            Text("Esqueci minha senha")
        }
        TextButton(onClick = onGuest, enabled = !busy) {
            Text("Continuar sem login")
        }
    }
}
