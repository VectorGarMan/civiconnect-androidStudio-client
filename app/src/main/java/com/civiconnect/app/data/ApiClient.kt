package com.civiconnect.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Dirección de la API.
 *  - Emulador de Android Studio:  http://10.0.2.2:8080   (10.0.2.2 = la PC que corre el emulador)
 *  - Celular físico (misma WiFi): http://IP_DE_TU_PC:8080  (ej. http://192.168.1.50:8080)
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8080"
}

/** Error "esperado" de la API o de red, con un mensaje listo para mostrar al usuario. */
class ApiException(mensaje: String) : Exception(mensaje)

// ---------- Utilidades para leer JSON de forma segura (org.json devuelve "null" como texto) ----------

fun JSONObject.str(clave: String): String? = if (isNull(clave)) null else optString(clave)

fun JSONObject.lng(clave: String): Long? = if (isNull(clave)) null else optLong(clave)

/** Convierte el arreglo "data" (o el que se indique) en una lista de objetos. */
fun JSONObject.lista(clave: String = "data"): List<JSONObject> {
    val arreglo = optJSONArray(clave) ?: return emptyList()
    return (0 until arreglo.length()).mapNotNull { arreglo.optJSONObject(it) }
}

/**
 * Cliente HTTP mínimo. Todas las respuestas de la API vienen envueltas en
 * { status, mensaje, error, data }: aquí se valida el sobre y se devuelve completo.
 */
class ApiClient(private val baseUrl: String = ApiConfig.BASE_URL) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    suspend fun get(ruta: String, consulta: Map<String, String> = emptyMap()): JSONObject {
        val url = (baseUrl.trimEnd('/') + ruta).toHttpUrl().newBuilder().apply {
            consulta.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        return ejecutar(Request.Builder().url(url).get().build())
    }

    suspend fun post(ruta: String, cuerpo: JSONObject): JSONObject =
        ejecutar(Request.Builder().url(baseUrl.trimEnd('/') + ruta).post(cuerpoJson(cuerpo)).build())

    /** La API usa DELETE con cuerpo JSON (eliminarVoto, eliminarComentario). */
    suspend fun delete(ruta: String, cuerpo: JSONObject): JSONObject =
        ejecutar(Request.Builder().url(baseUrl.trimEnd('/') + ruta).delete(cuerpoJson(cuerpo)).build())

    /** crearActualizar de reportes: parte "reporte" (JSON como texto) + partes "evidencia" (archivos). */
    suspend fun multipartReporte(ruta: String, reporte: JSONObject, evidencias: List<ByteArray>): JSONObject {
        val formulario = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reporte", reporte.toString())
        evidencias.forEachIndexed { i, bytes ->
            formulario.addFormDataPart(
                "evidencia", "evidencia$i.jpg",
                bytes.toRequestBody("image/jpeg".toMediaType())
            )
        }
        return ejecutar(Request.Builder().url(baseUrl.trimEnd('/') + ruta).post(formulario.build()).build())
    }

    private fun cuerpoJson(json: JSONObject): RequestBody = json.toString().toRequestBody(tipoJson)

    private suspend fun ejecutar(peticion: Request): JSONObject = withContext(Dispatchers.IO) {
        try {
            http.newCall(peticion).execute().use { respuesta ->
                val texto = respuesta.body?.string().orEmpty()
                val json = try {
                    JSONObject(texto)
                } catch (e: JSONException) {
                    null
                }
                val mensaje = json?.str("mensaje")?.takeIf { it.isNotBlank() }
                val error = json?.str("error")?.takeIf { it.isNotBlank() }
                val status = json?.str("status")?.lowercase()
                val fallo = !respuesta.isSuccessful || error != null ||
                    status == "error" || status == "fail" || status == "failed"
                if (fallo) {
                    throw ApiException(mensaje ?: error ?: "Error del servidor (${respuesta.code}).")
                }
                json ?: JSONObject()
            }
        } catch (e: IOException) {
            throw ApiException(
                "No se pudo conectar con el servidor. Verifica que la API esté encendida " +
                    "y que la dirección en ApiConfig.kt sea la correcta."
            )
        }
    }
}
