package com.apolo.llmstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userbutton.UserButton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val authState by combine(Clerk.isInitialized, Clerk.userFlow) { ready, user ->
                    when {
                        !ready -> AuthState.Loading
                        user == null -> AuthState.SignedOut
                        else -> AuthState.SignedIn
                    }
                }.collectAsState(initial = AuthState.Loading)

                when (authState) {
                    AuthState.Loading -> androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    AuthState.SignedOut -> Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Apolo LLM Studio", style = MaterialTheme.typography.headlineMedium)
                            Text("Entre para acessar seu ambiente Linux remoto.")
                            AuthView()
                        }
                    }
                    AuthState.SignedIn -> StudioHome()
                }
            }
        }
    }
}

private enum class AuthState { Loading, SignedOut, SignedIn }

@Composable
private fun StudioHome() {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B1020)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("APOLO", color = Color(0xFF9AB7FF), style = MaterialTheme.typography.labelLarge)
                Text("LLM Studio", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            UserButton()
        }
        Text("Seu ambiente de desenvolvimento", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Surface(color = Color(0xFF151E33), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Terminal Linux x86-64", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("Executor remoto ainda precisa ser provisionado e conectado.", color = Color(0xFFB8C5DC))
                Text("Estado: aguardando servidor", color = Color(0xFFFFD18A))
            }
        }
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color(0xFF050811), shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TERMINAL", color = Color(0xFF77E0B5), style = MaterialTheme.typography.labelLarge)
                Text("$ _", color = Color(0xFFDAE4F5))
                Text("A conexão será ativada após publicar e configurar a API x86-64.", color = Color(0xFF8291AA))
            }
        }
        Button(onClick = { scope.launch { Clerk.auth.signOut() } }, modifier = Modifier.fillMaxWidth()) {
            Text("Sair")
        }
    }
}
