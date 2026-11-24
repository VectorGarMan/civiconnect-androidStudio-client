package com.civiconnect.app.data

import com.civiconnect.app.domain.Catalogos
import com.civiconnect.app.domain.Categoria
import com.civiconnect.app.domain.EstadoReporte
import com.civiconnect.app.domain.ItemUbicacion
import com.civiconnect.app.domain.Prioridad
import com.civiconnect.app.domain.Reporte
import com.civiconnect.app.domain.Ubicacion
import com.civiconnect.app.domain.Usuario

/*
 * La app solo conoce estas interfaces. Hoy las implementa RepositorioApi (API REST de CiviConnect).
 * La URL del servidor se cambia en ApiConfig.kt.
 */

interface AuthRepository {
    suspend fun login(email: String, contrasena: String): Result<Usuario>

    suspend fun registrar(
        nombreUsuario: String,
        email: String,
        contrasena: String,
        esGubernamental: Boolean,
        ubicacion: Ubicacion
    ): Result<Unit>
}

interface CatalogoRepository {
    suspend fun catalogos(): Result<Catalogos>
    suspend fun estados(): Result<List<ItemUbicacion>>
    suspend fun municipios(idEstado: Long): Result<List<ItemUbicacion>>
    suspend fun colonias(idMunicipio: Long): Result<List<ItemUbicacion>>
}

interface ReporteRepository {
    /** [idUsuario] sirve para marcar qué reportes ya votó el usuario actual. */
    suspend fun obtenerReportes(idUsuario: Long): Result<List<Reporte>>

    suspend fun crearReporte(
        autor: Usuario,
        titulo: String,
        descripcion: String,
        categoria: Categoria,
        prioridad: Prioridad,
        calle: String,
        solucionPropuesta: String,
        evidencias: List<ByteArray>
    ): Result<Unit>

    suspend fun votar(idReporte: Long, idUsuario: Long): Result<Unit>

    suspend fun quitarVoto(idReporte: Long, idUsuario: Long): Result<Unit>

    suspend fun comentar(idReporte: Long, autor: Usuario, contenido: String): Result<Unit>

    suspend fun cambiarEstado(idReporte: Long, nuevoEstado: EstadoReporte): Result<Unit>
}
