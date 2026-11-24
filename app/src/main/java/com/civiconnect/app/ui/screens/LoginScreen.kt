package com.civiconnect.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.civiconnect.app.ui.CiviViewModel
import com.civiconnect.app.ui.components.BotonPrincipal
import com.civiconnect.app.ui.components.CiviTextField
import com.civiconnect.app.ui.components.MensajesEfecto
import com.civiconnect.app.ui.components.emailValido

@Composable
fun LoginScreen(
    vm: CiviViewModel,
    onIrRegistro: () -> Unit,
    onLoginExitoso: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }
    val cargando by vm.cargando.collectAsState()
    val errorAuth by vm.errorAuth.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    MensajesEfecto(vm, snackbar)

    val errorEmail = if (intento && !emailValido(email)) "Ingresa un correo válido" else null
    val errorContrasena = if (intento && contrasena.length < 4) "Mínimo 4 caracteres" else null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                "CiviConnect",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Reportes ciudadanos conectados con tu gobierno",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            CiviTextField(
                valor = email,
                onCambio = { email = it },
                etiqueta = "Correo electrónico",
                error = errorEmail,
                tipoTeclado = KeyboardType.Email
            )
            CiviTextField(
                valor = contrasena,
                onCambio = { contrasena = it },
                etiqueta = "Contraseña",
                esContrasena = true,
                error = errorContrasena
            )

            errorAuth?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            BotonPrincipal(
                texto = "Iniciar sesión",
                cargando = cargando,
                onClick = {
                    intento = true
                    if (emailValido(email) && contrasena.length >= 4) {
                        vm.login(email, contrasena, onLoginExitoso)
                    }
                }
            )
            TextButton(onClick = onIrRegistro) { Text("¿No tienes cuenta? Regístrate") }

        }
    }
}
