package com.civiconnect.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.civiconnect.app.domain.EstadoReporte
import com.civiconnect.app.domain.Reporte
import com.civiconnect.app.ui.CiviViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------- Utilidades ----------

fun emailValido(texto: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(texto.trim()).matches()

fun LocalDate.formato(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

/** El color viene de la API en hexadecimal (ej. "#FFA500"). */
fun colorEstado(estado: EstadoReporte): Color =
    try {
        Color(android.graphics.Color.parseColor(estado.colorHex))
    } catch (e: IllegalArgumentException) {
        Color(0xFF2980B9)
    }

// ---------- Mensajes (Snackbar) ----------

@Composable
fun MensajesEfecto(vm: CiviViewModel, host: SnackbarHostState) {
    val mensaje by vm.mensaje.collectAsState()
    LaunchedEffect(mensaje) {
        val texto = mensaje
        if (texto != null) {
            host.showSnackbar(texto)
            vm.limpiarMensaje()
        }
    }
}

// ---------- Componentes reutilizables ----------

@Composable
fun CiviTextField(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esContrasena: Boolean = false,
    error: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    unaLinea: Boolean = true,
    minLineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(),
        singleLine = unaLinea,
        minLines = minLineas,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(texto)
        }
    }
}

@Composable
fun EtiquetaChip(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SelectorMenu(
    etiqueta: String,
    opciones: List<String>,
    seleccion: String?,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { abierto = true },
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (seleccion == null) etiqueta else "$etiqueta: $seleccion",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
fun CargandoBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
fun ReporteCard(reporte: Reporte, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaChip(reporte.estado.etiqueta, colorEstado(reporte.estado))
                Spacer(Modifier.width(8.dp))
                Text(
                    reporte.categoria.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                reporte.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    reporte.ubicacion.resumen(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text("${reporte.totalVotos} votos", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(16.dp))
                Text(
                    "${reporte.comentarios.size} comentarios",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
