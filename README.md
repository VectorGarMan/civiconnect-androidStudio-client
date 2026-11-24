# CiviConnect – Prototipo móvil (Android)

Kotlin + Jetpack Compose (Material 3). Proyecto Integrador: POO + Desarrollo de Aplicaciones Nativas.

## Cómo abrirlo
1. Android Studio → **Open** → selecciona esta carpeta.
2. Espera el *Gradle Sync* (necesita internet la primera vez).
3. Elige un emulador (o tu celular) y pulsa **Run ▶**.

> Si Android Studio ofrece "Update Gradle/AGP", puedes aceptarlo.

## Conexión con la API
La app consume la API REST de CiviConnect (Spring Boot + PostgreSQL). La dirección se cambia en **una** línea:
`app/src/main/java/com/civiconnect/app/data/ApiClient.kt` → `ApiConfig.BASE_URL`

| Cómo corres la app | BASE_URL |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:8080` (ya viene así) |
| Celular físico en la misma WiFi | `http://IP_DE_TU_PC:8080` (ej. `http://192.168.1.50:8080`) |

La API debe estar encendida y el firewall de la PC debe permitir el puerto 8080.
Los usuarios son los de la base de datos (ej. `juan.perez@gmail.com` / `password123`).
Un usuario gubernamental solo puede entrar si `empleadogubverificado = true`.

## Estructura
```
domain/   Modelo POO: Usuario (abstracta) → Ciudadano / Gubernamental, Reporte, Comentario, enums
data/     Interfaces de repositorio + ApiClient (OkHttp) + RepositorioApi (API real)
ui/       CiviViewModel (estado), navegación, tema
ui/components/  Componentes reutilizables
ui/screens/     Login, Registro, Reportes, Detalle, CrearReporte
```
