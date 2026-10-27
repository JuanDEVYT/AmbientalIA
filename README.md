# 🌱 AmbientalIA

App de Android (Kotlin + Jetpack Compose) con **chat de IA**, **escáner de plantas**,
**catálogo de cuidados** y **recordatorio diario de riego**, con interfaz verde estilo
Duolingo. Toda la IA habla con tu **JuanAI / 9router** (API compatible con OpenAI).

> El plan completo (arquitectura, fases, decisiones y criterios de aceptación) está en
> [PLAN.md](PLAN.md).

---

## Qué hace

| Pantalla | Qué hay |
|---|---|
| 💬 **Chat** | Conversación en streaming con la IA, sugerencias y banner ecológico |
| 📸 **Escanear** | Cámara o galería → modelo con visión → nombre, estado, problemas, recomendaciones y riego |
| 🪴 **Plantas** | Tus plantas con barra de progreso, nivel y riego; catálogo precargado de 18 plantas |
| ⚙️ **Ajustes** | API key cifrada, modelos, recordatorio diario y compromiso ambiental |

La interfaz declara en pantalla que los modelos corren en **centros de datos con
consumo de agua mínimo o nulo** (banner en Chat y en Ajustes).

---

## Conexión con tu JuanAI

```
Base URL : https://studio-9router.onrender.com/v1
Modelos  : GET  /models
Chat     : POST /chat/completions   (formato OpenAI)
Visión   : content tipo image_url (data:image/jpeg;base64,…)
```

1. Abre JuanAI → **Conexión API** y crea una key para este proyecto (una por proyecto).
2. Instala el APK, entra a **Ajustes ⚙️**, pega la key y pulsa **Guardar clave**.
3. La app lista los modelos y te deja elegir el de texto y el de visión.

La key se guarda cifrada con `EncryptedSharedPreferences` (Keystore del dispositivo).
**Está dentro del APK**, así que si lo distribuyes conviene meter un proxy delante
(fase F8 del plan) y usar una key dedicada que puedas apagar.

---

## Recordatorios de riego

- Alarma local diaria a la hora que elijas (`AlarmManager`), sin servidor ni cuentas.
- Se reprograma sola tras reiniciar el móvil.
- En Android 12+ pide *alarmas exactas*; si no se concede, avisa y sigue con alarma
  inexacta.
- Botón **Probar aviso** en Ajustes.

---

## 🎨 Animaciones de plantas

Set de animaciones nativas (Jetpack Compose + `Canvas`, **sin dependencias nuevas
ni imágenes**: puro vector, nítido a cualquier DPI) en
`ui/components/WateringAnimations.kt`:

| Composable | Qué hace | Dónde encaja |
|---|---|---|
| `RiegoAnimado(visible)` | Lluvia de gotas con salpicaduras | Sobre la tarjeta al tocar **Regar 💧** |
| `Modifier.rebotarAlRegar(regando)` | Rebote elástico de la planta | En el icono de la planta |
| `EstallidoChispas(visible)` | Destellos ámbar que irradian | Insignia "+1 nivel", rachas, logros |
| `GotaCargando()` | Gota que se aplasta y estira | Loader del escáner / burbuja del chat |
| `HojaGirando()` | Hoja girando en círculo | Spinner de carga larga |

El color del agua es el token `Agua` de `ui/theme/Color.kt`. Uso básico:

```kotlin
Box {
    LeafCard { … }
    RiegoAnimado(visible = regando, Modifier.matchParentSize())
}
```

Ya está cableado en `PlantsScreen.TarjetaPlanta`: al regar, la tarjeta se llena
de gotas, la planta rebota y estallan chispas ámbar junto a "+1 nivel".

### Intro de bienvenida

`res/raw/intro_riego.webm` (~124 KB, 1080×1080, 3,5 s): la regadera riega una
planta que crece y florece con chispas ámbar. Se genera con Remotion y se
reproduce con `IntroScreen` al arrancar la app (`MainActivity.AmbientalRoot`);
si el video no puede reproducirse en el dispositivo, la app sigue directa al
menú sin quedarse pegada.

---

## Compilar (GitHub Actions)

La app **no se compila en local**: el workflow [build.yml](.github/workflows/build.yml)
lo hace en cada push a `main`, en cada PR y bajo demanda (`workflow_dispatch`).

1. Sube el proyecto a un repo de GitHub.
2. Actions → **Build APK** → espera el job; el APK queda como artefacto
   (`AmbientalIA-apk`).
3. Etiqueta un commit como `v1.0.0` y el workflow publica la release con el APK.

### Secretos opcionales de firma

Si no existen, la release sale sin firmar (usa el APK de `debug` para instalar):

| Secreto | Contenido |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | contraseña del keystore |
| `KEY_ALIAS` | alias |
| `KEY_PASSWORD` | contraseña de la clave |

### En local (opcional)

```bash
gradle :app:assembleDebug     # necesita Android SDK y JDK 17
```

---

## Estructura

```
app/src/main/java/com/ambientalia/
├── AmbientalIaApp.kt     # arranque: prefs, plantas, canal y alarma
├── MainActivity.kt       # navegación de 4 pestañas + permisos
├── ai/                   # AiClient (streaming, reintentos, visión), modelos, prompts
├── data/                 # Prefs (key cifrada), catálogo, plantas del usuario
├── notify/               # WaterReminder, AlarmReceiver, BootReceiver
└── ui/                   # tema verde, componentes Duolingo, 4 pantallas
```
