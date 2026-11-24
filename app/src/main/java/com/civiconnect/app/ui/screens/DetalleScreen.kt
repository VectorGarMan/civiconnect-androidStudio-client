package com.civiconnect.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.civiconnect.app.domain.Reporte
import com.civiconnect.app.ui.CiviViewModel
import com.civiconnect.app.ui.UiState
import com.civiconnect.app.ui.components.CargandoBox
import com.civiconnect.app.ui.components.EtiquetaChip
import com.civiconnect.app.ui.components.ImagenEvidencia
import com.civiconnect.app.ui.components.MensajesEfecto
import com.civiconnect.app.ui.components.SelectorMenu
import com.civiconnect.app.ui.components.colorEstado
import com.civiconnect.app.ui.components.formato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(vm: CiviViewModel, idReporte: Long, onVolver: () -> Unit) {
    val usuario by vm.usuario.collectAsState()
    val estadoReportes by vm.reportes.collectAsState()
    val catalogos by vm.catalogos.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    MensajesEfecto(vm, snackbar)
    var texto by remember { mutableStateOf("") }

    val u = usuario
    val reporte: Reporte? = when (val s = estadoReportes) {
        is UiState.Exito -> s.datos.firstOrNull { it.id == idReporte }
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del reporte") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (u != null && reporte != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            placeholder = {
                                Text(if (u.comentarioEsOficial()) "Escribe una respuesta oficial…" else "Escribe un comentario…")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )
                        IconButton(onClick = {
                            if (texto.isNotBlank()) {
                                vm.comentar(reporte.id, texto.trim())
                                texto = ""
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (reporte == null) {
            CargandoBox(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EtiquetaChip(reporte.estado.etiqueta, colorEstado(reporte.estado))
                            EtiquetaChip("Prioridad ${reporte.prioridad.etiqueta}", MaterialTheme.colorScheme.primary)
                            EtiquetaChip(reporte.categoria.etiqueta, MaterialTheme.colorScheme.secondary)
                        }
                        Text(
                            reporte.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Por ${reporte.creador} · ${reporte.fecha.formato()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${reporte.calle} · ${reporte.ubicacion.resumen()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(reporte.descripcion, style = MaterialTheme.typography.bodyLarge)
                        reporte.evidencias.forEach { foto ->
                            ImagenEvidencia(foto, Modifier.fillMaxWidth().height(200.dp))
                        }
                        if (reporte.solucionPropuesta.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Solución propuesta", fontWeight = FontWeight.Bold)
                                    Text(reporte.solucionPropuesta)
                                }
                            }
                        }
                    }
                }

                item {
                    if (u != null && u.puedeVotar(reporte)) {
                        if (reporte.votadoPorMi) {
                            OutlinedButton(onClick = { vm.alternarVoto(reporte) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Quitar voto (${reporte.totalVotos})")
                            }
                        } else {
                            Button(onClick = { vm.alternarVoto(reporte) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Votar (${reporte.totalVotos})")
                            }
                        }
                    } else {
                        Text(
                            "${reporte.totalVotos} votos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (u != null && u.puedeCambiarEstado()) {
                    item {
                        SelectorMenu(
                            etiqueta = "Cambiar estado",
                            opciones = (catalogos?.estadosVisibles ?: emptyList()).map { it.etiqueta },
                            seleccion = reporte.estado.etiqueta,
                            onSeleccion = { elegido ->
                                catalogos?.estados?.firstOrNull { it.etiqueta == elegido }?.let { nuevo ->
                                    vm.cambiarEstado(reporte.id, nuevo)
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        "Comentarios (${reporte.comentarios.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (reporte.comentarios.isEmpty()) {
                    item {
                        Text(
                            "Aún no hay comentarios. ¡Sé el primero!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(reporte.comentarios, key = { it.id }) { comentario ->
                    val colorFondo = if (comentario.esOficial) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorFondo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(comentario.autor, fontWeight = FontWeight.Bold)
                                if (comentario.esOficial) {
                                    Spacer(Modifier.width(8.dp))
                                    EtiquetaChip("Respuesta oficial", MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Text(comentario.contenido)
                            Text(
                                comentario.fecha.formato(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
