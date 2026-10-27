package com.ambientalia.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.ambientalia.ui.theme.Agua
import com.ambientalia.ui.theme.Ambar
import com.ambientalia.ui.theme.VerdePrincipal
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animaciones de riego y cuidado de plantas, dibujadas con `Canvas`
 * (sin dependencias nuevas, sin imágenes: puro vector a cualquier DPI).
 *
 * Todas son disparables con un `Boolean` y se auto-reproducen una vez:
 * pon `visible = true`, espera la duración y vuelve a `false`.
 *
 * | Composable           | Uso                                              |
 * | -------------------- | ------------------------------------------------ |
 * | [RiegoAnimado]       | Lluvia de gotas + salpicaduras al tocar "Regar"   |
 * | [rebotarAlRegar]     | Rebote elástico de la planta al recibir agua      |
 * | [EstallidoChispas]   | Chispas ámbar de subida de nivel / racha          |
 * | [GotaCargando]       | Loader de gota que respira (escáner, IA)          |
 * | [HojaGirando]        | Spinner de hoja girando (carga larga)             |
 */

// ── Formas base ─────────────────────────────────────────────────────────────

/** Gota de agua: punta arriba, panza redonda. Dibujada en el origen. */
private fun Path.gota(radio: Float, alto: Float) {
    reset()
    moveTo(0f, -alto)
    cubicTo(radio * 1.2f, -alto * 0.45f, radio, radio * 0.6f, 0f, radio)
    cubicTo(-radio, radio * 0.6f, -radio * 1.2f, -alto * 0.45f, 0f, -alto)
    close()
}

/** Destello de 4 puntas (estrella fina) centrado en [centro]. */
private fun Path.destello(centro: Offset, radio: Float) {
    reset()
    moveTo(centro.x, centro.y - radio)
    lineTo(centro.x + radio * 0.26f, centro.y)
    lineTo(centro.x, centro.y + radio)
    lineTo(centro.x - radio * 0.26f, centro.y)
    close()
    moveTo(centro.x - radio, centro.y)
    lineTo(centro.x, centro.y - radio * 0.26f)
    lineTo(centro.x + radio, centro.y)
    lineTo(centro.x, centro.y + radio * 0.26f)
    close()
}

/** Hoja apuntada en ambos extremos, horizontal, centrada en el origen. */
private fun Path.hoja(largo: Float, ancho: Float) {
    reset()
    moveTo(-largo, 0f)
    cubicTo(-largo * 0.3f, -ancho, largo * 0.3f, -ancho, largo, 0f)
    cubicTo(largo * 0.3f, ancho, -largo * 0.3f, ancho, -largo, 0f)
    close()
}

// ── 1. Riego: gotas cayendo + salpicaduras ──────────────────────────────────

/**
 * Lluvia de gotas que cae sobre el contenido y salpica al tocar el suelo.
 * Cada gota nace con un retardo y una posición distintos (semillas fijas,
 * así el render es siempre el mismo). Dura ~[duracionMs] y se apaga sola.
 *
 * ```
 * Box {
 *     LeafCard { … }                                   // la tarjeta
 *     RiegoAnimado(visible = regando, Modifier.matchParentSize())
 * }
 * ```
 */
@Composable
fun RiegoAnimado(
    visible: Boolean,
    modifier: Modifier = Modifier,
    duracionMs: Int = 1350,
    cantidadGotas: Int = 14,
    colorAgua: Color = Agua,
) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progreso.snapTo(0f)
            progreso.animateTo(1f, tween(duracionMs, easing = LinearEasing))
        } else {
            progreso.snapTo(1f) // oculto en reposo
        }
    }
    val ms = progreso.value * duracionMs

    Canvas(modifier = modifier) {
        if (!visible && progreso.value >= 1f) return@Canvas
        val ancho = size.width
        val alto = size.height
        if (ancho <= 0f || alto <= 0f) return@Canvas

        val inicioY = alto * 0.12f
        val sueloY = alto * 0.78f
        val recorrido = sueloY - inicioY

        repeat(cantidadGotas) { i ->
            val rnd = Random(i * 7919 + 13)
            val retraso = rnd.nextFloat() * 360f
            if (ms < retraso) return@repeat // aún no ha salido

            val x0 = ancho * (0.16f + rnd.nextFloat() * 0.68f)
            val caidaMs = 440f + rnd.nextFloat() * 190f
            val radio = 3f + rnd.nextFloat() * 2.6f
            val deriva = 5f + rnd.nextFloat() * 9f
            val t = ((ms - retraso) / caidaMs).coerceIn(0f, 1f)

            if (t < 1f) {
                // Cae acelerando (t²) y se estira según su velocidad.
                val gravedad = t * t
                val y = inicioY + recorrido * gravedad
                val alfa = when {
                    t < 0.12f -> t / 0.12f
                    t > 0.88f -> (1f - t) / 0.12f
                    else -> 1f
                }
                val estira = 1f + gravedad * 0.9f
                withTransform({
                    translate(left = x0 + deriva * gravedad, top = y)
                    rotate(degrees = 5f)
                }) {
                    val p = Path().apply { gota(radio, radio * 3.4f * estira) }
                    drawPath(p, colorAgua.copy(alpha = alfa * 0.95f))
                    drawCircle(
                        color = Color.White.copy(alpha = alfa * 0.65f),
                        radius = radio * 0.3f,
                        center = Offset(-radio * 0.32f, radio * 0.5f),
                    )
                }
            } else {
                // Salpicadura: aro que se abre + dos gotitas que rebotan.
                val p = ((ms - retraso - caidaMs) / 280f).coerceIn(0f, 1f)
                if (p >= 1f) return@repeat
                val aro = radio * (1.3f + p * 3.6f)
                val salto = radio * 6f * p * (1f - p)
                drawCircle(
                    color = colorAgua.copy(alpha = (1f - p) * 0.5f),
                    radius = aro,
                    center = Offset(x0 + deriva, sueloY),
                    style = Stroke(width = radio * 0.6f),
                )
                drawCircle(
                    color = colorAgua.copy(alpha = (1f - p) * 0.85f),
                    radius = radio * 0.45f,
                    center = Offset(x0 + deriva - aro * 0.95f, sueloY - salto),
                )
                drawCircle(
                    color = colorAgua.copy(alpha = (1f - p) * 0.85f),
                    radius = radio * 0.38f,
                    center = Offset(x0 + deriva + aro * 0.95f, sueloY - salto * 0.8f),
                )
            }
        }
    }
}

// ── 2. Rebote de la planta al recibir agua ──────────────────────────────────

/**
 * Modifier que hace crecer y volver con rebote elástico cuando
 * [regando] pasa a `true`. Va justo en el modifier de la planta.
 *
 * ```
 * Box(Modifier = Modifier.size(66.dp).rebotarAlRegar(regando)…) { PlantArt(…) }
 * ```
 */
@Composable
fun Modifier.rebotarAlRegar(regando: Boolean, intensidad: Float = 1.18f): Modifier {
    var objetivo by remember { mutableFloatStateOf(1f) }
    val escala by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "rebotarRegar",
    )
    LaunchedEffect(regando) {
        if (regando) {
            objetivo = intensidad
            delay(250)
            objetivo = 1f
        }
    }
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

// ── 3. Chispas de recompensa (subida de nivel / racha) ──────────────────────

/**
 * Estallido de destellos ámbar que irradia desde el centro.
 * Ideal detrás de la insignia "+1 nivel" o de un logro de racha.
 *
 * ```
 * Box(contentAlignment = Alignment.Center) {
 *     EstallidoChispas(visible = regando, Modifier.size(150.dp).align(Alignment.Center))
 *     Text("💧 +1 nivel")
 * }
 * ```
 */
@Composable
fun EstallidoChispas(
    visible: Boolean,
    modifier: Modifier = Modifier,
    cantidad: Int = 12,
    colores: List<Color> = listOf(Ambar, Color.White),
    duracionMs: Int = 850,
    escala: Float = 1f,
) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progreso.snapTo(0f)
            progreso.animateTo(1f, tween(duracionMs, easing = FastOutLinearInEasing))
        }
    }
    Canvas(modifier = modifier) {
        val t = progreso.value
        if (!visible || t <= 0f || t >= 1f) return@Canvas
        val centro = center
        val radioMax = size.minDimension / 2f * escala

        repeat(cantidad) { i ->
            val rnd = Random(i * 31 + 7)
            val angulo = (i.toFloat() / cantidad) * 360f + rnd.nextFloat() * 26f
            val retardo = rnd.nextFloat() * 0.25f
            val tLocal = ((t - retardo) / (1f - retardo)).coerceIn(0f, 1f)
            if (tLocal <= 0f) return@repeat

            val rad = angulo * (PI_F / 180f)
            val dist = radioMax * (0.22f + tLocal * 0.82f)
            val pos = Offset(
                centro.x + cos(rad) * dist,
                centro.y + sin(rad) * dist,
            )
            val alfa = 1f - tLocal
            val radio = radioMax * 0.14f * (1f - tLocal * 0.5f)
            val p = Path().apply { destello(pos, radio) }
            drawPath(p, colores[i % colores.size].copy(alpha = alfa))
        }
    }
}

// ── 4. Loader de gota (respira) ─────────────────────────────────────────────

/**
 * Gota que se aplasta y estira en bucle: indicador de carga para el
 * escáner ("analizando tu planta…") o la burbuja del chat.
 */
@Composable
fun GotaCargando(
    modifier: Modifier = Modifier,
    color: Color = Agua,
) {
    val transicion = rememberInfiniteTransition(label = "gotaCarga")
    val comprime by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "comprime",
    )
    val salto by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "saltoGota",
    )
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f + r * 0.35f * (1f - comprime)
        val alto = r * 2.6f * (1f + comprime * 0.28f)
        withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = 1f - comprime * 0.16f, scaleY = 1f + comprime * 0.1f, pivot = Offset.Zero)
        }) {
            val p = Path().apply { gota(r * 0.8f, alto) }
            drawPath(p, color)
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = r * 0.2f,
                center = Offset(-r * 0.28f, r * 0.45f),
            )
        }
        // aro de agua debajo
        drawCircle(
            color = color.copy(alpha = 0.35f * (1f - salto)),
            radius = r * (0.35f + salto * 0.5f),
            center = Offset(cx, size.height * 0.92f),
            style = Stroke(width = r * 0.14f),
        )
    }
}

// ── 5. Spinner de hoja ──────────────────────────────────────────────────────

/**
 * Hoja que gira en círculo: carga larga con aire de huerto.
 * Sustituye el clásico círculo giratorio en escaneos y descargas.
 */
@Composable
fun HojaGirando(
    modifier: Modifier = Modifier,
    color: Color = VerdePrincipal,
) {
    val transicion = rememberInfiniteTransition(label = "hojaGira")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "giro",
    )
    val respira by transicion.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respira",
    )
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        rotate(giro) {
            val p = Path().apply { hoja(r * 0.88f * respira, r * 0.5f * respira) }
            drawPath(p, color)
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = r * 0.16f,
                center = Offset(r * 0.1f, -r * 0.1f),
            )
        }
    }
}

/** PI en Float, para no andar convirtiendo en cada frame. */
private const val PI_F = 3.14159265f
