package com.civiconnect.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civiconnect.app.data.AuthRepository
import com.civiconnect.app.data.CatalogoRepository
import com.civiconnect.app.data.RepositorioApi
import com.civiconnect.app.data.ReporteRepository
import com.civiconnect.app.domain.Catalogos
import com.civiconnect.app.domain.Categoria
import com.civiconnect.app.domain.EstadoReporte
import com.civiconnect.app.domain.ItemUbicacion
import com.civiconnect.app.domain.Prioridad
import com.civiconnect.app.domain.Reporte
import com.civiconnect.app.domain.Ubicacion
import com.civiconnect.app.domain.Usuario
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de pantalla: cargando, éxito o error. */
sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>
    data class Exito<T>(val datos: T) : UiState<T>
    data class Error(val mensaje: String) : UiState<Nothing>
}

class CiviViewModel : ViewModel() {

    // La URL del servidor se configura en data/ApiConfig (archivo ApiClient.kt).
    private val repositorio = RepositorioApi()
    private val auth: AuthRepository = repositorio
    private val reportesRepo: ReporteRepository = repositorio
    private val catalogoRepo: CatalogoRepository = repositorio

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _reportes = MutableStateFlow<UiState<List<Reporte>>>(UiState.Cargando)
    val reportes: StateFlow<UiState<List<Reporte>>> = _reportes.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _catalogos = MutableStateFlow<Catalogos?>(null)
    val catalogos: StateFlow<Catalogos?> = _catalogos.asStateFlow()

    private val _estadosUb = MutableStateFlow<List<ItemUbicacion>>(emptyList())
    val estadosUb: StateFlow<List<ItemUbicacion>> = _estadosUb.asStateFlow()

    private val _municipiosUb = MutableStateFlow<List<ItemUbicacion>>(emptyList())
    val municipiosUb: StateFlow<List<ItemUbicacion>> = _municipiosUb.asStateFlow()

    private val _coloniasUb = MutableStateFlow<List<ItemUbicacion>>(emptyList())
    val coloniasUb: StateFlow<List<ItemUbicacion>> = _coloniasUb.asStateFlow()

    private var jobMunicipios: Job? = null
    private var jobColonias: Job? = null

    init {
        cargarCatalogos()
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    // ---------- Catálogos y ubicación (vienen de la API) ----------

    /** Es seguro llamarla varias veces: solo pide lo que aún no se ha cargado. */
    fun cargarCatalogos() {
        viewModelScope.launch {
            if (_catalogos.value == null) {
                catalogoRepo.catalogos()
                    .onSuccess { _catalogos.value = it }
                    .onFailure { _mensaje.value = it.message ?: "No se pudieron cargar los catálogos." }
            }
            if (_estadosUb.value.isEmpty() && _catalogos.value != null) {
                catalogoRepo.estados()
                    .onSuccess { _estadosUb.value = it }
                    .onFailure { _mensaje.value = it.message ?: "No se pudieron cargar los estados." }
            }
        }
    }

    fun seleccionarEstado(idEstado: Long) {
        jobMunicipios?.cancel()
        jobColonias?.cancel()
        _municipiosUb.value = emptyList()
        _coloniasUb.value = emptyList()
        jobMunicipios = viewModelScope.launch {
            catalogoRepo.municipios(idEstado)
                .onSuccess { _municipiosUb.value = it }
                .onFailure { _mensaje.value = it.message ?: "No se pudieron cargar los municipios." }
        }
    }

    fun seleccionarMunicipio(idMunicipio: Long) {
        jobColonias?.cancel()
        _coloniasUb.value = emptyList()
        jobColonias = viewModelScope.launch {
            catalogoRepo.colonias(idMunicipio)
                .onSuccess { _coloniasUb.value = it }
                .onFailure { _mensaje.value = it.message ?: "No se pudieron cargar las colonias." }
        }
    }

    // ---------- Autenticación ----------

    fun login(email: String, contrasena: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _errorAuth.value = null
            auth.login(email.trim(), contrasena)
                .onSuccess {
                    _usuario.value = it
                    cargarCatalogos()
                    onExito()
                }
                .onFailure { _errorAuth.value = it.message ?: "No se pudo iniciar sesión." }
            _cargando.value = false
        }
    }

    fun registrar(
        nombreUsuario: String,
        email: String,
        contrasena: String,
        esGubernamental: Boolean,
        ubicacion: Ubicacion,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            _cargando.value = true
            auth.registrar(nombreUsuario, email, contrasena, esGubernamental, ubicacion)
                .onSuccess {
                    _mensaje.value = if (esGubernamental) {
                        "Cuenta creada. Podrás iniciar sesión cuando verifiquen tu cuenta gubernamental."
                    } else {
                        "Cuenta creada. Ya puedes iniciar sesión."
                    }
                    onExito()
                }
                .onFailure { _mensaje.value = it.message ?: "No se pudo crear la cuenta." }
            _cargando.value = false
        }
    }

    fun cerrarSesion() {
        _usuario.value = null
        _errorAuth.value = null
        _reportes.value = UiState.Cargando
    }

    // ---------- Reportes ----------

    fun cargarReportes(silencioso: Boolean = false) {
        val u = _usuario.value ?: return
        viewModelScope.launch {
            if (!silencioso) _reportes.value = UiState.Cargando
            reportesRepo.obtenerReportes(u.id)
                .onSuccess { _reportes.value = UiState.Exito(it) }
                .onFailure { _reportes.value = UiState.Error(it.message ?: "No se pudieron cargar los reportes.") }
        }
    }

    private fun ejecutar(mensajeOk: String, accion: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            accion()
                .onSuccess {
                    _mensaje.value = mensajeOk
                    cargarReportes(silencioso = true)
                }
                .onFailure { _mensaje.value = it.message ?: "Ocurrió un error." }
        }
    }

    fun crearReporte(
        titulo: String,
        descripcion: String,
        categoria: Categoria,
        prioridad: Prioridad,
        calle: String,
        solucion: String,
        evidencias: List<ByteArray>,
        onExito: () -> Unit
    ) {
        val u = _usuario.value ?: return
        // Regla de negocio resuelta por polimorfismo: cada tipo de usuario decide.
        if (!u.puedeCrearReportes()) {
            _mensaje.value = "Usuarios gubernamentales no pueden crear reportes."
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            reportesRepo.crearReporte(u, titulo, descripcion, categoria, prioridad, calle, solucion, evidencias)
                .onSuccess {
                    _mensaje.value = "Reporte publicado con estado Pendiente."
                    cargarReportes(silencioso = true)
                    onExito()
                }
                .onFailure { _mensaje.value = it.message ?: "No se pudo crear el reporte." }
            _cargando.value = false
        }
    }

    fun alternarVoto(reporte: Reporte) {
        val u = _usuario.value ?: return
        if (!u.puedeVotar(reporte)) {
            _mensaje.value = "No puedes votar este reporte."
            return
        }
        if (reporte.votadoPorMi) {
            ejecutar("Voto retirado.") { reportesRepo.quitarVoto(reporte.id, u.id) }
        } else {
            ejecutar("¡Voto registrado!") { reportesRepo.votar(reporte.id, u.id) }
        }
    }

    fun comentar(idReporte: Long, contenido: String) {
        val u = _usuario.value ?: return
        if (contenido.isBlank()) return
        val mensajeOk = if (u.comentarioEsOficial()) "Respuesta oficial publicada." else "Comentario publicado."
        ejecutar(mensajeOk) { reportesRepo.comentar(idReporte, u, contenido) }
    }

    fun cambiarEstado(idReporte: Long, nuevoEstado: EstadoReporte) {
        val u = _usuario.value ?: return
        if (!u.puedeCambiarEstado()) {
            _mensaje.value = "No tienes permiso para cambiar el estado."
            return
        }
        ejecutar("Estado actualizado a ${nuevoEstado.etiqueta}.") {
            reportesRepo.cambiarEstado(idReporte, nuevoEstado)
        }
    }
}
