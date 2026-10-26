# 🌱 AmbientalIA — Plan de desarrollo

App de celular (Android) con chat de IA, escáner de plantas y recordatorios de riego.
Interfaz **estilo Duolingo, paleta 100 % verde**, en **español**. IA conectada a tu
**JuanAI / 9router** (API compatible con OpenAI).

---

## 1. Decisiones ya tomadas

| Tema | Decisión |
|---|---|
| Plataforma | **Kotlin + Jetpack Compose** (Android), sin dependencias nativas raras |
| Compilación | **GitHub Actions** compila el APK; no se compila en la PC local |
| IA | **Directo desde la app** a `https://studio-9router.onrender.com/v1` |
| Recordatorios | **Locales** en el dispositivo (sin servidor, sin cuentas) |
| Plantas | **Catálogo precargado + escáner con visión de la IA** |
| Idioma | Solo español (system prompt en español) |
| Entregable | Plan + **MVP completo** funcional |

> ⚠️ **Riesgo aceptado (decisión tuya):** la API key vive dentro de la app. Cualquiera
> con el APK puede extraerla y gastar tu cuota. Mitigaciones aplicadas en el MVP:
> key guardada en `EncryptedSharedPreferences`, **nunca** en logs ni en recursos
> compilados, campo para pegar/apagar la key desde Ajustes, y uso de **una key
> aparte para esta app** (una key por proyecto) para poder apagarla sin romper el
> resto. El paso 2 recomendado es meter un proxy; el código ya está aislado en un
> único cliente, así que cambiarlo es tocar una sola clase.

---

## 2. Stack

| Capa | Elección |
|---|---|
| Lenguaje | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3, BOM actual |
| Navegación | `navigation-compose` (4 destinos + barra inferior) |
| HTTP | OkHttp 4 + `kotlinx-serialization` (JSON OpenAI) |
| Streaming | SSE manual sobre OkHttp (`stream: true`) |
| Cámara | CameraX (foto del escáner) |
| Imágenes | Coil (miniaturas de plantas) |
| Recordatorios | `AlarmManager` (alarma diaria exacta) + `NotificationCompat` |
| Persistencia | `SharedPreferences` / `EncryptedSharedPreferences` + DataStore no hace falta |
| Build | Gradle Kotlin DSL, AGP 8.7, catalog `libs.versions.toml` |
| CI | GitHub Actions → `assembleRelease` → APK firmado + artefacto |

---

## 3. Conexión con JuanAI (del `.md`)

```
Base URL : https://studio-9router.onrender.com/v1
Header    : Authorization: Bearer sk-...
Chat      : POST /chat/completions   (formato OpenAI, texto en choices[0].message.content)
Modelos   : GET  /models             (nombres "proveedor/modelo")
Visión    : POST /chat/completions con content tipo image_url (data:image/jpeg;base64,...)
Streaming : "stream": true  →  SSE, acumular delta.content
```

Reglas del `.md` implementadas en `AiClient.kt`:

1. Reintento automático en `503` (Render se despierta) y `429`, con espera 1s → 2s → 4s
   y respeto de `Retry-After`.
2. Arranque en frío: el primer mensaje puede tardar; se muestra estado "despertando el
   servidor…" en la burbuja de carga.
3. Si un modelo no soporta `tools` o visión, la app degrada y lo dice en pantalla.
4. El nombre de modelo se obtiene de `GET /models` y se puede cambiar en Ajustes.
5. La key **no** está en el código: se pega en Ajustes la primera vez.

---

## 4. Arquitectura

```
app/src/main/java/com/ambientalia/
├── AmbientalIaApp.kt          // Application: canal de notificaciones, WorkManager
├── MainActivity.kt            // Scaffold + NavBar + 4 pantallas
├── ai/
│   ├── AiClient.kt            // OkHttp + JSON OpenAI, streaming SSE, reintentos, visión
│   ├── Models.kt              // DTOs de la API
│   └── Prompt.kt              // system prompt (español, personalidad AmbientalIA)
├── data/
│   ├── Prefs.kt               // API key cifrada, modelo, hora del recordatorio, plantas
│   ├── PlantCatalog.kt        // ~24 plantas: riego, luz, cuidados, datos eco
│   └── UserPlants.kt          // plantas del usuario + estado de riego
├── notify/
│   └── WaterReminder.kt       // alarma diaria local + notificaciones + boot
├── ui/
│   ├── theme/                 // Color.kt, Type.kt, Theme.kt (verde Duolingo)
│   ├── components/            // EcoBanner, ChunkyButton, Card Verde, Burbuja chat
│   └── screens/               // Chat, Escáner, Mis plantas, Ajustes
└── res/                       // strings (es), iconos adaptativos, colores
```

**Estado:** `ViewModel` ligero por pantalla + `StateFlow`. Sin DI (los objetos son
singleton en memoria) para mantener el MVP pequeño y legible.

---

## 5. Pantallas

### 5.1 💬 Chat
- Burbujas estilo Duolingo: fichas redondeadas gruesas, sombra sólida abajo (sin blur),
  fondo verde claro, avatar hoja.
- Entrada con botón redondo grande tipo Duolingo.
- Streaming de la respuesta carácter a carácter.
- Chips de sugerencias: *"¿Cuánta agua necesita mi suculenta?"*, *"¿Por qué se amarillentan mis hojas?"*…
- **Banner ecológico fijo arriba:** *"Hablamos con modelos servidos en centros de datos
  con consumo de agua mínimo o nulo"* — con punto verde parpadeante.

### 5.2 📸 Escáner de plantas
1. Cámara (CameraX) o galería → foto.
2. Se envía a un **modelo con visión** (`image_url` base64).
3. Respuesta estructurada (JSON del modelo): **nombre de la planta, estado (sano /
   estrés / plaga), qué le falta, recomendaciones, riego sugerido**.
4. Ficha visual: tarjeta grande verde con nombre en tipografía redondeada, badge de
   estado con color, checklist de recomendaciones y botón *"Guardar en mis plantas"*.

### 5.3 🌿 Mis plantas
- Catálogo + plantas guardadas con foto, riego programado y **barra de progreso
  estilo Duolingo** de días sin regar.
- Cada planta, al regarla, sube de "nivel" (gamacificación verde).
- Fichas de cuidados del catálogo precargado.

### 5.4 ⚙️ Ajustes
- API key (pegar / borrar), selector de modelo (traído de `GET /models`), modelo de visión.
- **Recordatorio de riego:** hora (picker), activar/desactivar, texto del permiso.
- Sobre el eco: qué se afirma y por qué (transparencia).

---

## 6. Sistema de diseño: "verde Duolingo"

| Token | Valor | Uso |
|---|---|---|
| `VerdePrincipal` | `#58CC02` | Botones, progreso, acentos |
| `VerdeOscuro` | `#43A302` | Borde/sombra sólida inferior (3 dp, sin blur) |
| `VerdeFondo` | `#F0FBEB` | Fondo de pantalla |
| `VerdeHoja` | `#D7F5D0` | Cards y burbujas |
| `VerdeBosque` | `#175B1A` | Texto principal |
| `Ámbar` | `#FFC800` | Rachas, logros |
| Tipografía | Rounded (oppins redondeada / `FontFamily.SansSerif` + peso bold) | Títulos mayúsculas |

Reglas: esquinas de 16–24 dp, bordes de 2 dp en verde oscuro, **sombra sólida de 3 dp
debajo** de cada botón (se "hunde" al pulsar), todo el texto en verde bosque sobre
claro, animaciones rebote (`spring`) en botones y barras.

---

## 7. Notificaciones de riego (locales)

- Permiso `POST_NOTIFICATIONS` (Android 13+) + `SCHEDULE_EXACT_ALARM` para que suene
  a la hora exacta aunque el celular esté en reposo.
- `AlarmManager.setExactAndAllowWhileIdle()` programa la alarma de mañana y se
  reprograma sola tras cada disparo; si Android 12+ no permite alarmas exactas,
  se cae a una alarma inexacta y Ajustes ofrece el botón para concederla.
- Texto: *"🪴 ¡Hora de regar tus plantas! Toca para ver cuáles necesitan agua."*
- Toca → abre la app directamente en **Mis plantas**.
- Si el usuario tiene plantas guardadas, la notificación las nombra una a una.
- Botón "probar notificación" en Ajustes.

---

## 8. Fases de trabajo

| # | Fase | Resultado |
|---|---|---|
| **F0** | Repo, Gradle, catálogo de versiones, manifest, íconos | Proyecto abre en Android Studio / CI |
| **F1** | Tema verde Duolingo + componentes base | Pantallas "bonitas" con datos falsos |
| **F2** | `AiClient` (chat, streaming, modelos, reintentos 503/429) | Chat real contra JuanAI |
| **F3** | Escáner con visión + ficha de planta | Foto → diagnóstico de la planta |
| **F4** | Catálogo + Mis plantas + riego/gamificación | Ciclo completo de plantas |
| **F5** | Notificaciones locales de riego | Recordatorio diario real |
| **F6** | Banner eco + Ajustes + permisos | MVP completo |
| **F7** | GitHub Actions: build, lint, APK firmado | APK descargable en Actions |

**Orden de implementación:** F0 → F1 → F2 → F3 → F4 → F5 → F6 → F7 (cada fase deja la
app compilando).

---

## 9. CI/CD (GitHub Actions)

- `.github/workflows/build.yml`
  - `push` y `pull_request` a `main`.
  - JDK 17 (Temurin) + Gradle cache + Android SDK preinstalado del runner.
  - `./gradlew :app:assembleRelease :app:lintRelease`.
  - Sube `app-release.apk` como artefacto.
- Firma: keystore en repo como **secret** (`ANDROID_KEYSTORE_BASE64`, `KEY_ALIAS`,
  `KEYSTORE_PASSWORD`, `KEY_PASSWORD`); si no existen, sale **debuggable=false sin
  firmar de release → se usa `assembleDebug` como respaldo**, sin romper el pipeline.
- Release automático con tag `v*` → APK adjunto a la release.

---

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Render dormido → `503` | Reintentos con backoff + `Retry-After`, aviso "despertando servidor" |
| Key filtrada con el APK | Key cifrada, key dedicada, campo para apagarla, proxy en F8 |
| Modelo sin visión | Selector de modelo de visión aparte; si falla, mensaje claro |
| Cuota de la IA gastada por prompts largos | System prompt corto, historial limitado a los últimos 10 mensajes |
| Sin internet | Estados de error bonitos con botón "Reintentar" |
| Notificación no suena en MIUI/One UI | Instrucciones en Ajustes (batería sin restricciones, alarma exacta) |

---

## 11. Criterios de aceptación del MVP

1. Instala el APK desde Actions y arranca en el celular.
2. Al abrir, pide la API key y elige modelo; `GET /models` carga la lista.
3. El chat responde en streaming, en español, con la personalidad AmbientalIA.
4. El banner de agua mínimo/nulo es visible en Chat y Ajustes.
5. Escanear una foto devuelve nombre, estado y recomendaciones, y se puede guardar.
6. "Mis plantas" muestra el catálogo y el estado de riego con barra de progreso.
7. El recordatorio diario se programa y llega a la hora elegida (botón de prueba).
8. `./gradlew build` pasa en GitHub Actions y hay APK descargable.

---

## 12. Después del MVP (F8+)

1. **Proxy** para sacar la key del APK (Cloudflare Workers o tu Render).
2. Multiidioma, modo oscuro, widgets de pantalla de inicio.
3. Gamificación: rachas, insignias, XP por plantas cuidadas.
4. iOS (el diseño ya es nativo, tocaría portar a KMP/SwiftUI).
5. Sync en la nube con cuentas opcionales.
