package com.civiconnect.app.data

import android.util.Base64
import com.civiconnect.app.domain.Catalogos
import com.civiconnect.app.domain.Categoria
import com.civiconnect.app.domain.Ciudadano
import com.civiconnect.app.domain.Comentario
import com.civiconnect.app.domain.EstadoReporte
import com.civiconnect.app.domain.Gubernamental
import com.civiconnect.app.domain.ItemUbicacion
import com.civiconnect.app.domain.Prioridad
import com.civiconnect.app.domain.Reporte
import com.civiconnect.app.domain.Ubicacion
import com.civiconnect.app.domain.Usuario
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementación contra la API REST de CiviConnect (Spring Boot).
 * Endpoints: /api/usuarios/*, /api/reporte/*, /api/ubicacion/*.
 */
class RepositorioApi(private val api: ApiClient = ApiClient()) :
    AuthRepository, ReporteRepository, CatalogoRepository {

    private var tiposUsuario: Map<Long, String>? = null
    private var catalogosCache: Catalogos? = null

    // ---------- Utilidades ----------

    private fun ahora(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun fechaDe(texto: String?): LocalDate =
        try {
            LocalDate.parse(texto!!.take(10))
        } catch (e: Exception) {
            LocalDate.now()
        }

    private fun items(json: JSONObject, claveId: String): List<ItemUbicacion> =
        json.lista().map { ItemUbicacion(it.lng(claveId) ?: 0L, it.str("nombre") ?: "") }

    private suspend fun tipos(): Map<Long, String> {
        tiposUsuario?.let { return it }
        val mapa = api.get("/api/usuarios/tipoUsuario/obtener").lista()
            .associate { (it.lng("idtipousuario") ?: 0L) to (it.str("nombre") ?: "") }
        tiposUsuario = mapa
        return mapa
    }

    private suspend fun catalogosInterno(): Catalogos {
        catalogosCache?.let { return it }
        val categorias = api.get("/api/reporte/categoria/obtener").lista()
            .map { Categoria(it.lng("idcategoria") ?: 0L, it.str("nombre") ?: "") }
        val prioridades = api.get("/api/reporte/nivelPrioridad/obtener").lista()
            .map { Prioridad(it.lng("idnivelprioridad") ?: 0L, it.str("nombre") ?: "", it.optInt("nivelnumerico")) }
            .sortedByDescending { it.nivel }
        val estados = api.get("/api/reporte/estadoReporte/obtener").lista()
            .map { EstadoReporte(it.lng("idestadoreporte") ?: 0L, it.str("nombre") ?: "", it.str("color") ?: "#888888") }
        if (estados.isEmpty()) throw ApiException("La API no devolvió estados de reporte.")
        return Catalogos(categorias, prioridades, estados).also { catalogosCache = it }
    }

    // ---------- CatalogoRepository ----------

    override suspend fun catalogos(): Result<Catalogos> = runCatching { catalogosInterno() }

    override suspend fun estados(): Result<List<ItemUbicacion>> =
        runCatching { items(api.get("/api/ubicacion/estado/obtener"), "idestado") }

    override suspend fun municipios(idEstado: Long): Result<List<ItemUbicacion>> = runCatching {
        items(
            api.get("/api/ubicacion/municipio/obtenerPorEstado", mapOf("idestado" to idEstado.toString())),
            "idmunicipio"
        )
    }

    override suspend fun colonias(idMunicipio: Long): Result<List<ItemUbicacion>> = runCatching {
        items(
            api.get("/api/ubicacion/colonia/obtenerPorMunicipio", mapOf("idmunicipio" to idMunicipio.toString())),
            "idcolonia"
        )
    }

    // ---------- AuthRepository ----------

    override suspend fun login(email: String, contrasena: String): Result<Usuario> = runCatching {
        val respuesta = api.post(
            "/api/usuarios/login",
            JSONObject().put("email", email).put("contrasena", contrasena)
        )
        val datos = respuesta.optJSONObject("data")
            ?: throw ApiException(respuesta.str("mensaje")?.takeIf { it.isNotBlank() } ?: "Credenciales incorrectas.")

        val id = datos.lng("idusuario") ?: 0L
        val idTipo = datos.lng("idtipousuario") ?: 0L
        val esGub = tipos()[idTipo].equals("Gubernamental", ignoreCase = true)
        if (esGub && !datos.optBoolean("empleadogubverificado", false)) {
            throw ApiException("Tu cuenta gubernamental aún no ha sido verificada.")
        }

        val idColonia = datos.lng("idcolonia") ?: 0L
        val ubicacion = try {
            val u = api.get("/api/usuarios/ubicacion/$id").optJSONObject("data")
            Ubicacion(
                estado = u?.str("nombreEstado") ?: "",
                municipio = u?.str("nombreMunicipio") ?: "",
                colonia = u?.str("nombreColonia") ?: "",
                idColonia = u?.lng("idColonia") ?: idColonia
            )
        } catch (e: ApiException) {
            Ubicacion("", "", "", idColonia)
        }

        val nombre = datos.str("nombreusuario") ?: ""
        val correo = datos.str("email") ?: email
        if (esGub) Gubernamental(id, nombre, correo, ubicacion) else Ciudadano(id, nombre, correo, ubicacion)
    }

    override suspend fun registrar(
        nombreUsuario: String,
        email: String,
        contrasena: String,
        esGubernamental: Boolean,
        ubicacion: Ubicacion
    ): Result<Unit> = runCatching {
        val nombreTipo = if (esGubernamental) "Gubernamental" else "Ciudadano"
        val idTipo = tipos().entries.firstOrNull { it.value.equals(nombreTipo, ignoreCase = true) }?.key
            ?: if (esGubernamental) 2L else 1L
        api.post(
            "/api/usuarios/crear",
            JSONObject()
                .put("idtipousuario", idTipo)
                .put("idcolonia", ubicacion.idColonia)
                .put("email", email)
                .put("contrasena", contrasena)
                .put("nombreusuario", nombreUsuario)
                .put("fecharegistro", LocalDate.now().toString())
        )
        Unit
    }

    // ---------- ReporteRepository ----------

    private fun parsearComentario(c: JSONObject, autores: Map<Long, String>): Comentario {
        val idUsuario = c.lng("idusuario") ?: 0L
        return Comentario(
            id = c.lng("idcomentario") ?: 0L,
            idUsuario = idUsuario,
            autor = autores[idUsuario] ?: "Usuario $idUsuario",
            contenido = c.str("contenido") ?: "",
            fecha = fechaDe(c.str("fechacreacion")),
            esOficial = c.optBoolean("esoficial", false)
        )
    }

    private fun parsearReporte(
        o: JSONObject,
        cat: Catalogos,
        autores: Map<Long, String>,
        votados: Set<Long>,
        comentariosExtra: List<JSONObject>?
    ): Reporte? {
        val v = o.optJSONObject("reporteView") ?: return null
        val id = v.lng("idreporte") ?: 0L

        val nombreCategoria = v.str("categoria") ?: ""
        val nombrePrioridad = v.str("prioridad") ?: ""
        val nombreEstado = v.str("estadoreporte") ?: ""

        val categoria = cat.categorias.firstOrNull { it.etiqueta == nombreCategoria }
            ?: Categoria(0L, nombreCategoria)
        val prioridad = cat.prioridades.firstOrNull { it.etiqueta == nombrePrioridad }
            ?: Prioridad(0L, nombrePrioridad, v.optInt("nivelnumerico"))
        val estado = cat.estados.firstOrNull { it.etiqueta == nombreEstado }
            ?: EstadoReporte(0L, nombreEstado, v.str("colorestado") ?: "#888888")

        val calle = listOfNotNull(v.str("calle"), v.str("referencia"))
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val crudos = comentariosExtra ?: o.lista("comentarios")
        val comentarios = crudos.map { parsearComentario(it, autores) }.sortedBy { it.id }

        val evidencias = o.lista("evidencias").mapNotNull { e ->
            try {
                e.str("archivo")?.let { Base64.decode(it, Base64.DEFAULT) }
            } catch (ex: IllegalArgumentException) {
                null
            }
        }

        return Reporte(
            id = id,
            idUsuarioCreador = v.lng("idusuariocreador") ?: 0L,
            creador = v.str("creador") ?: "",
            titulo = v.str("titulo") ?: "",
            descripcion = v.str("descripcion") ?: "",
            categoria = categoria,
            prioridad = prioridad,
            estado = estado,
            ubicacion = Ubicacion(v.str("estado") ?: "", v.str("municipio") ?: "", v.str("colonia") ?: ""),
            calle = calle,
            solucionPropuesta = v.str("solucionpropuesta") ?: "",
            fecha = fechaDe(v.str("fechacreacion")),
            totalVotos = v.optInt("totalvotos"),
            votadoPorMi = id in votados,
            comentarios = comentarios,
            evidencias = evidencias
        )
    }

    override suspend fun obtenerReportes(idUsuario: Long): Result<List<Reporte>> = runCatching {
        val cat = catalogosInterno()
        val lista = api.get("/api/reporte/obtener").lista()

        // Los comentarios solo traen idusuario; el nombre sale de la lista de usuarios.
        val autores: Map<Long, String> = try {
            api.get("/api/usuarios/obtener").lista()
                .associate { (it.lng("idusuario") ?: 0L) to (it.str("nombreusuario") ?: "") }
        } catch (e: ApiException) {
            emptyMap()
        }

        val votados: Set<Long> = try {
            api.get("/api/reporte/obtenerVotadoPorUsuario/$idUsuario").lista()
                .mapNotNull { it.optJSONObject("reporteView")?.lng("idreporte") }
                .toSet()
        } catch (e: ApiException) {
            emptySet()
        }

        lista.mapNotNull { item ->
            val vista = item.optJSONObject("reporteView")
            val idReporte = vista?.lng("idreporte") ?: 0L
            val totalComentarios = vista?.optInt("totalcomentarios") ?: 0
            // Si el listado no trae los comentarios incrustados, se piden aparte.
            val extra: List<JSONObject>? =
                if (item.lista("comentarios").isEmpty() && totalComentarios > 0) {
                    try {
                        api.get("/api/reporte/comentario/obtenerPorReporte/$idReporte").lista()
                    } catch (e: ApiException) {
                        null
                    }
                } else {
                    null
                }
            parsearReporte(item, cat, autores, votados, extra)
        }
    }

    override suspend fun crearReporte(
        autor: Usuario,
        titulo: String,
        descripcion: String,
        categoria: Categoria,
        prioridad: Prioridad,
        calle: String,
        solucionPropuesta: String,
        evidencias: List<ByteArray>
    ): Result<Unit> = runCatching {
        val cat = catalogosInterno()
        val marca = ahora()
        val reporte = JSONObject()
            .put("idusuario", autor.id)
            .put("idcolonia", autor.ubicacion.idColonia)
            .put("idnivelprioridad", prioridad.id)
            .put("idestadoreporte", cat.estadoInicial().id)
            .put("idcategoria", categoria.id)
            .put("titulo", titulo)
            .put("descripcion", descripcion)
            .put("fechacreacion", marca)
            .put("fechaactualizacion", marca)
            .put("solucionpropuesta", solucionPropuesta.ifBlank { JSONObject.NULL })
            .put("calle", calle.ifBlank { JSONObject.NULL })
        api.multipartReporte("/api/reporte/crearActualizar", reporte, evidencias)
        Unit
    }

    override suspend fun votar(idReporte: Long, idUsuario: Long): Result<Unit> = runCatching {
        api.post(
            "/api/reporte/votar",
            JSONObject().put("idusuario", idUsuario).put("idreporte", idReporte)
        )
        Unit
    }

    override suspend fun quitarVoto(idReporte: Long, idUsuario: Long): Result<Unit> = runCatching {
        api.delete(
            "/api/reporte/eliminarVoto",
            JSONObject().put("idusuario", idUsuario).put("idreporte", idReporte)
        )
        Unit
    }

    override suspend fun comentar(idReporte: Long, autor: Usuario, contenido: String): Result<Unit> = runCatching {
        val marca = ahora()
        api.post(
            "/api/reporte/comentario/crearActualizar",
            JSONObject()
                .put("idusuario", autor.id)
                .put("idreporte", idReporte)
                .put("contenido", contenido)
                .put("fechacreacion", marca)
                .put("fechaactualizacion", marca)
                .put("editado", false)
                .put("esoficial", autor.comentarioEsOficial())
        )
        Unit
    }

    /**
     * La API no tiene un endpoint solo para el estado: se baja la entidad completa del reporte,
     * se cambia idestadoreporte y se reenvía por crearActualizar (sin tocar las evidencias).
     */
    override suspend fun cambiarEstado(idReporte: Long, nuevoEstado: EstadoReporte): Result<Unit> = runCatching {
        val entidad = api.get("/api/reporte/obtenerEntidadPorId/$idReporte").optJSONObject("data")
            ?: throw ApiException("El reporte no existe.")
        entidad.put("idestadoreporte", nuevoEstado.id)
        entidad.put("fechaactualizacion", ahora())
        api.multipartReporte("/api/reporte/crearActualizar", entidad, emptyList())
        Unit
    }
}
