package com.civiconnect.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.civiconnect.app.domain.Categoria
import com.civiconnect.app.domain.Prioridad
import com.civiconnect.app.ui.CiviViewModel
import com.civiconnect.app.ui.components.BotonPrincipal
import com.civiconnect.app.ui.components.CiviTextField
import com.civiconnect.app.ui.components.ImagenEvidencia
import com.civiconnect.app.ui.components.comprimirImagen
import com.civiconnect.app.ui.components.MensajesEfecto
import com.civiconnect.app.ui.components.SelectorMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReporteScreen(vm: CiviViewModel, onVolver: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<Categoria?>(null) }
    var prioridad by remember { mutableStateOf<Prioridad?>(null) }
    var calle by remember { mutableStateOf("") }
    var solucion by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
    var intento by remember { mutableStateOf(false) }

    val usuario by vm.usuario.collectAsState()
    val catalogos by vm.catalogos.collectAsState()
    val contexto = LocalContext.current
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) comprimirImagen(contexto, uri)?.let { fotos = fotos + it }
    }
    LaunchedEffect(Unit) { vm.cargarCatalogos() }
    val cargando by vm.cargando.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    MensajesEfecto(vm, snackbar)

    val errorTitulo = if (intento && titulo.trim().length < 5) "Mínimo 5 caracteres" else null
    val errorDescripcion = if (intento && descripcion.trim().length < 10) "Describe el problema (mínimo 10 caracteres)" else null
    val errorSelectores = if (intento && (categoria == null || prioridad == null)) "Selecciona categoría y prioridad" else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo reporte") },
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
            usuario?.let {
                Text(
                    "Ubicación: ${it.ubicacion.resumen()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CiviTextField(titulo, { titulo = it }, "Título", error = errorTitulo)
            CiviTextField(
                descripcion, { descripcion = it }, "Descripción",
                error = errorDescripcion, unaLinea = false, minLineas = 3
            )
            SelectorMenu(
                etiqueta = "Categoría",
                opciones = (catalogos?.categorias ?: emptyList()).map { it.etiqueta },
                seleccion = categoria?.etiqueta,
                onSeleccion = { elegida -> categoria = catalogos?.categorias?.firstOrNull { it.etiqueta == elegida } }
            )
            SelectorMenu(
                etiqueta = "Prioridad",
                opciones = (catalogos?.prioridades ?: emptyList()).map { it.etiqueta },
                seleccion = prioridad?.etiqueta,
                onSeleccion = { elegida -> prioridad = catalogos?.prioridades?.firstOrNull { it.etiqueta == elegida } }
            )
            errorSelectores?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            CiviTextField(calle, { calle = it }, "Calle o referencia (opcional)")
            CiviTextField(
                solucion, { solucion = it }, "Solución propuesta (opcional)",
                unaLinea = false, minLineas = 2
            )

            OutlinedButton(
                onClick = { selectorFoto.launch("image/*") },
                enabled = fotos.size < 3,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar foto de evidencia (${fotos.size}/3)")
            }
            fotos.forEachIndexed { indice, foto ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImagenEvidencia(foto, Modifier.weight(1f).height(90.dp))
                    TextButton(onClick = { fotos = fotos.filterIndexed { i, _ -> i != indice } }) {
                        Text("Quitar")
                    }
                }
            }

            BotonPrincipal(
                texto = "Publicar reporte",
                cargando = cargando,
                onClick = {
                    intento = true
                    val cat = categoria
                    val pri = prioridad
                    val datosValidos = titulo.trim().length >= 5 && descripcion.trim().length >= 10
                    if (datosValidos && cat != null && pri != null) {
                        vm.crearReporte(
                            titulo.trim(), descripcion.trim(), cat, pri,
                            calle.trim(), solucion.trim(), fotos, onVolver
                        )
                    }
                }
            )
        }
    }
}
