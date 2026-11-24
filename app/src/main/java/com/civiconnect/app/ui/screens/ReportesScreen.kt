package com.civiconnect.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.civiconnect.app.domain.EstadoReporte
import com.civiconnect.app.ui.CiviViewModel
import com.civiconnect.app.ui.UiState
import com.civiconnect.app.ui.components.CargandoBox
import com.civiconnect.app.ui.components.ErrorBox
import com.civiconnect.app.ui.components.MensajesEfecto
import com.civiconnect.app.ui.components.ReporteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    vm: CiviViewModel,
    onDetalle: (Long) -> Unit,
    onCrear: () -> Unit,
    onSalir: () -> Unit
) {
    val usuario by vm.usuario.collectAsState()
    val estadoReportes by vm.reportes.collectAsState()
    val catalogos by vm.catalogos.collectAsState()
    var filtro by remember { mutableStateOf<EstadoReporte?>(null) }
    val snackbar = remember { SnackbarHostState() }
    MensajesEfecto(vm, snackbar)

    LaunchedEffect(Unit) { vm.cargarReportes() }

    val u = usuario

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                actions = { TextButton(onClick = onSalir) { Text("Salir") } }
            )
        },
        floatingActionButton = {
            if (u != null && u.puedeCrearReportes()) {
                FloatingActionButton(onClick = onCrear) {
                    Icon(Icons.Filled.Add, contentDescription = "Crear reporte")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (u != null) {
                Text(
                    text = "Hola, ${u.nombreUsuario} · ${u.tipo}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtro == null,
                    onClick = { filtro = null },
                    label = { Text("Todos") }
                )
                (catalogos?.estadosVisibles ?: emptyList()).forEach { estado ->
                    FilterChip(
                        selected = filtro == estado,
                        onClick = { filtro = estado },
                        label = { Text(estado.etiqueta) }
                    )
                }
            }

            when (val s = estadoReportes) {
                is UiState.Cargando -> CargandoBox()
                is UiState.Error -> ErrorBox(s.mensaje, onReintentar = { vm.cargarReportes() })
                is UiState.Exito -> {
                    val lista = s.datos
                        .filter { filtro == null || it.estado.etiqueta == filtro?.etiqueta }
                        .sortedByDescending { it.totalVotos }
                    if (lista.isEmpty()) {
                        Text(
                            "No hay reportes con ese filtro.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(lista, key = { it.id }) { reporte ->
                                ReporteCard(reporte, onClick = { onDetalle(reporte.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
