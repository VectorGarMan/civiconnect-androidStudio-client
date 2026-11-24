package com.civiconnect.app.domain

import java.time.LocalDate

// ---------- Catálogos (vienen de la API; cada uno trae su id de base de datos) ----------

data class EstadoReporte(val id: Long, val etiqueta: String, val colorHex: String)

data class Prioridad(val id: Long, val etiqueta: String, val nivel: Int)

data class Categoria(val id: Long, val etiqueta: String)

/** Elemento de la cascada Estado > Municipio > Colonia. */
data class ItemUbicacion(val id: Long, val nombre: String)

data class Catalogos(
    val categorias: List<Categoria>,
    val prioridades: List<Prioridad>,
    val estados: List<EstadoReporte>
) {
    /** "Cancelado" no aparece en las vistas de la API, así que tampoco se ofrece en la app. */
    val estadosVisibles: List<EstadoReporte>
        get() = estados.filter { !it.etiqueta.equals("Cancelado", ignoreCase = true) }

    fun estadoInicial(): EstadoReporte =
        estados.firstOrNull { it.etiqueta.equals("Pendiente", ignoreCase = true) } ?: estados.first()
}

// ---------- Ubicación jerárquica: Estado > Municipio > Colonia ----------

data class Ubicacion(
    val estado: String,
    val municipio: String,
    val colonia: String,
    val idColonia: Long = 0
) {
    fun resumen(): String = "$colonia, $municipio, $estado"
}

// ---------- Usuarios (abstracción + herencia + polimorfismo) ----------

abstract class Usuario(
    val id: Long,
    val nombreUsuario: String,
    val email: String,
    val ubicacion: Ubicacion
) {
    abstract val tipo: String
    abstract fun puedeCrearReportes(): Boolean
    abstract fun puedeCambiarEstado(): Boolean
    abstract fun puedeVotar(reporte: Reporte): Boolean
    abstract fun comentarioEsOficial(): Boolean
}

class Ciudadano(
    id: Long,
    nombreUsuario: String,
    email: String,
    ubicacion: Ubicacion
) : Usuario(id, nombreUsuario, email, ubicacion) {
    override val tipo: String = "Ciudadano"
    override fun puedeCrearReportes(): Boolean = true
    override fun puedeCambiarEstado(): Boolean = false
    // Un ciudadano no puede votar su propio reporte
    override fun puedeVotar(reporte: Reporte): Boolean = reporte.idUsuarioCreador != id
    override fun comentarioEsOficial(): Boolean = false
}

class Gubernamental(
    id: Long,
    nombreUsuario: String,
    email: String,
    ubicacion: Ubicacion
) : Usuario(id, nombreUsuario, email, ubicacion) {
    override val tipo: String = "Gubernamental"
    override fun puedeCrearReportes(): Boolean = false
    override fun puedeCambiarEstado(): Boolean = true
    override fun puedeVotar(reporte: Reporte): Boolean = false
    override fun comentarioEsOficial(): Boolean = true
}

// ---------- Reportes y comentarios ----------

data class Comentario(
    val id: Long,
    val idUsuario: Long,
    val autor: String,
    val contenido: String,
    val fecha: LocalDate,
    val esOficial: Boolean
)

data class Reporte(
    val id: Long,
    val idUsuarioCreador: Long,
    val creador: String,
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoReporte,
    val ubicacion: Ubicacion,
    val calle: String,
    val solucionPropuesta: String,
    val fecha: LocalDate,
    val totalVotos: Int,
    /** true si el usuario con sesión iniciada ya votó este reporte. */
    val votadoPorMi: Boolean,
    val comentarios: List<Comentario>,
    /** Fotos de evidencia (bytes JPEG/PNG ya decodificados de base64). */
    val evidencias: List<ByteArray>
)
