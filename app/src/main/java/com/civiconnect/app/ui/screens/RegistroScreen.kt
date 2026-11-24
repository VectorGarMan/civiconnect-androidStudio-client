package com.civiconnect.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.civiconnect.app.domain.ItemUbicacion
import com.civiconnect.app.domain.Ubicacion
import com.civiconnect.app.ui.CiviViewModel
import com.civiconnect.app.ui.components.BotonPrincipal
import com.civiconnect.app.ui.components.CiviTextField
import com.civiconnect.app.ui.components.MensajesEfecto
import com.civiconnect.app.ui.components.SelectorMenu
import com.civiconnect.app.ui.components.emailValido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(vm: CiviViewModel, onVolver: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var esGubernamental by remember { mutableStateOf(false) }
    var estado by remember { mutableStateOf<ItemUbicacion?>(null) }
    var municipio by remember { mutableStateOf<ItemUbicacion?>(null) }
    var colonia by remember { mutableStateOf<ItemUbicacion?>(null) }
    var intento by remember { mutableStateOf(false) }

    val cargando by vm.cargando.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    MensajesEfecto(vm, snackbar)

    val estados by vm.estadosUb.collectAsState()
    val municipios by vm.municipiosUb.collectAsState()
    val colonias by vm.coloniasUb.collectAsState()
    // Si la API no estaba disponible al abrir la app, aquí se reintenta.
    LaunchedEffect(Unit) { vm.cargarCatalogos() }

    val errorNombre = if (intento && nombre.trim().length < 3) "Mínimo 3 caracteres" else null
    val errorEmail = if (intento && !emailValido(email)) "Ingresa un correo válido" else null
    val errorContrasena = if (intento && contrasena.length < 4) "Mínimo 4 caracteres" else null
    val errorConfirmar = if (intento && confirmar != contrasena) "Las contraseñas no coinciden" else null
    val errorUbicacion = if (intento && colonia == null) "Selecciona estado, municipio y colonia" else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CiviTextField(nombre, { nombre = it }, "Nombre de usuario", error = errorNombre)
            CiviTextField(email, { email = it }, "Correo electrónico", error = errorEmail, tipoTeclado = KeyboardType.Email)
            CiviTextField(contrasena, { contrasena = it }, "Contraseña", esContrasena = true, error = errorContrasena)
            CiviTextField(confirmar, { confirmar = it }, "Confirmar contraseña", esContrasena = true, error = errorConfirmar)

            Text("Tipo de usuario", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !esGubernamental, onClick = { esGubernamental = false })
                Text("Ciudadano")
                RadioButton(selected = esGubernamental, onClick = { esGubernamental = true })
                Text("Gubernamental")
            }

            Text("Ubicación", style = MaterialTheme.typography.labelLarge)
            SelectorMenu(
                etiqueta = "Estado",
                opciones = estados.map { it.nombre },
                seleccion = estado?.nombre,
                onSeleccion = { elegido ->
                    estados.firstOrNull { it.nombre == elegido }?.let {
                        estado = it
                        municipio = null
                        colonia = null
                        vm.seleccionarEstado(it.id)
                    }
                }
            )
            SelectorMenu(
                etiqueta = "Municipio",
                opciones = municipios.map { it.nombre },
                seleccion = municipio?.nombre,
                habilitado = estado != null,
                onSeleccion = { elegido ->
                    municipios.firstOrNull { it.nombre == elegido }?.let {
                        municipio = it
                        colonia = null
                        vm.seleccionarMunicipio(it.id)
                    }
                }
            )
            SelectorMenu(
                etiqueta = "Colonia",
                opciones = colonias.map { it.nombre },
                seleccion = colonia?.nombre,
                habilitado = municipio != null,
                onSeleccion = { elegido -> colonias.firstOrNull { it.nombre == elegido }?.let { colonia = it } }
            )
            errorUbicacion?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            BotonPrincipal(
                texto = "Registrarme",
                cargando = cargando,
                onClick = {
                    intento = true
                    val e = estado
                    val m = municipio
                    val c = colonia
                    val datosValidos = nombre.trim().length >= 3 && emailValido(email) &&
                        contrasena.length >= 4 && confirmar == contrasena
                    if (datosValidos && e != null && m != null && c != null) {
                        vm.registrar(
                            nombre.trim(), email.trim(), contrasena,
                            esGubernamental, Ubicacion(e.nombre, m.nombre, c.nombre, c.id), onVolver
                        )
                    }
                }
            )
        }
    }
}
