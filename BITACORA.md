# Bitácora de integración y registro de cambios – CiviConnect móvil

**Proyecto:** CiviConnect · **Stack:** Android Studio, Kotlin, Jetpack Compose (Material 3)
**Alcance de esta versión:** prototipo funcional en *modo demo* (datos locales simulados).

---

## Actividad 5 – Sprint 1: Prototipo móvil funcional

### Qué se implementó
| Elemento | Archivo(s) | Criterio que cubre |
|---|---|---|
| Navegación entre 5 pantallas (login, registro, reportes, detalle, crear) | `ui/Navegacion.kt` | Navegación operativa |
| Login con validación de correo y contraseña | `ui/screens/LoginScreen.kt` | Validaciones básicas |
| Registro con validaciones y ubicación Estado › Municipio › Colonia | `ui/screens/RegistroScreen.kt` | Validaciones básicas |
| Lista de reportes con filtro por estado y orden por votos | `ui/screens/ReportesScreen.kt` | Flujo principal con datos de prueba |
| Estados de pantalla Cargando / Éxito / Error | `ui/CiviViewModel.kt` (`UiState`) | Manejo de estados |
| Componentes reutilizables (campo, botón, chip, selector, tarjeta, cargando, error) | `ui/components/Componentes.kt` | Componentes reutilizables |

### Decisiones de diseño (POO)
- **Abstracción y herencia:** `Usuario` es abstracta; `Ciudadano` y `Gubernamental` heredan de ella.
- **Polimorfismo:** cada tipo de usuario decide sus permisos (`puedeCrearReportes()`, `puedeCambiarEstado()`, `puedeVotar()`, `comentarioEsOficial()`), sin `if` por tipo repartidos en la interfaz.
- **Encapsulamiento:** `Reporte` expone `totalVotos` y `haVotado()` en lugar de dejar manipular la lista de votantes desde la UI.
- **Separación de responsabilidades:** `domain` (reglas) / `data` (origen de datos) / `ui` (pantallas).

### Evidencia (capturas)
1. Login · 2. Login con error de validación · 3. Registro · 4. Lista de reportes · 5. Filtro "En proceso"

---

## Actividad 6 – Sprint 2: Integración funcional

### Funcionalidades agregadas (4)
| # | Funcionalidad | Caso de uso | Regla de negocio |
|---|---|---|---|
| 1 | Votar / quitar voto | CU-009, CU-017 | Solo ciudadanos; no se puede votar el propio reporte |
| 2 | Comentar reporte | CU-010 | Si lo escribe un usuario gubernamental se marca como **respuesta oficial** (CU-012) |
| 3 | Crear reporte | CU-005, CU-006, CU-007 | Solo ciudadanos; estado inicial *Pendiente*; ubicación tomada del perfil |
| 4 | Cambiar estado del reporte | CU-011 | Solo usuarios gubernamentales |

### Datos locales y punto de sustitución
Los datos viven en memoria en `data/RepositorioLocal.kt` (incluye latencia simulada con `delay`).
La app solo conoce las interfaces `AuthRepository` y `ReporteRepository`.
**Punto de sustitución:** la línea `private val repositorio = RepositorioLocal()` de `CiviViewModel`.
Para usar la API REST real (Spring Boot, `/api/usuarios`, `/api/reporte`, `/api/ubicacion`) se crea `RepositorioApi`
con las mismas interfaces y se reemplaza esa línea; las pantallas no cambian.

### Manejo de errores básicos
- Credenciales incorrectas, correo ya registrado, reporte inexistente, voto duplicado.
- Permisos: gubernamental intentando crear reporte, ciudadano votando su propio reporte.
- Los mensajes se muestran con *Snackbar* y los fallos de carga con pantalla de error + botón *Reintentar*.

### Flujo completo demostrable (inicio → resultado)
Login (ciudadano) → ver reportes → crear reporte → aparece en la lista como *Pendiente* → cerrar sesión →
Login (gubernamental) → abrir el reporte → comentar (respuesta oficial) → cambiar estado a *Solucionado* →
volver a la lista y filtrar por *Solucionado*.

### Registro de cambios
| Versión | Cambio |
|---|---|
| 0.1 | Estructura del proyecto, tema Material 3, modelo de dominio |
| 0.2 | Login, registro y navegación (Sprint 1) |
| 0.3 | Lista de reportes con filtros y estados de carga/error (Sprint 1) |
| 1.0 | Detalle, votos, comentarios, crear reporte, cambio de estado (Sprint 2) |

### Evidencia (capturas)
6. Detalle de reporte · 7. Voto registrado · 8. Comentario oficial · 9. Cambio de estado · 10. Nuevo reporte creado · 11. Error de credenciales

### Pendiente para siguientes iteraciones
Conexión a la API real, evidencias fotográficas, persistencia de sesión, recuperación de contraseña, edición y eliminación de comentarios.

---

## Actividad 7 – Integración con la API real

### Qué cambió
| Área | Cambio |
|---|---|
| Datos | `RepositorioLocal` se reemplazó por `RepositorioApi` (OkHttp + org.json) que implementa las mismas interfaces |
| Catálogos | Categorías, prioridades y estados de reporte se cargan de la API (ya no son enums fijos) |
| Ubicación | Estado › Municipio › Colonia se carga en cascada desde `/api/ubicacion/*` |
| Reportes | Lista, detalle, votos (`/votar`, `/eliminarVoto`), comentarios y creación contra `/api/reporte/*` |
| Estado | La API no tiene endpoint de estado: se envía el reporte completo por `crearActualizar` con el nuevo `idestadoreporte` |
| Evidencias | Se muestran las fotos del reporte y se pueden adjuntar hasta 3 al crear (JPEG comprimido, multipart) |
| Seguridad | Usuarios gubernamentales no verificados no pueden iniciar sesión |

### Registro de cambios
| Versión | Cambio |
|---|---|
| 1.1 | Conexión completa con la API REST, catálogos dinámicos, evidencias fotográficas |

### Pendiente
Persistencia de sesión, recuperación de contraseña, edición y eliminación de comentarios, hilos de respuestas.
