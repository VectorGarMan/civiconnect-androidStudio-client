package com.civiconnect.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Lee una imagen elegida de la galería y la reduce/comprime a JPEG para subirla a la API. */
fun comprimirImagen(contexto: Context, uri: Uri, maxLado: Int = 1280, calidad: Int = 80): ByteArray? {
    return try {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        var muestreo = 1
        while (limites.outWidth / muestreo > maxLado * 2 || limites.outHeight / muestreo > maxLado * 2) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val original = contexto.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        } ?: return null
        val escala = minOf(1f, maxLado.toFloat() / maxOf(original.width, original.height))
        val listo: Bitmap = if (escala < 1f) {
            Bitmap.createScaledBitmap(
                original, (original.width * escala).toInt(), (original.height * escala).toInt(), true
            )
        } else {
            original
        }
        val salida = ByteArrayOutputStream()
        listo.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        salida.toByteArray()
    } catch (e: Exception) {
        null
    }
}

private fun decodificar(bytes: ByteArray, maxLado: Int = 1024): Bitmap? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
    var muestreo = 1
    while (limites.outWidth / muestreo > maxLado || limites.outHeight / muestreo > maxLado) muestreo *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
}

@Composable
fun ImagenEvidencia(bytes: ByteArray, modifier: Modifier = Modifier) {
    val imagen = remember(bytes) { decodificar(bytes)?.asImageBitmap() }
    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = "Evidencia del reporte",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
