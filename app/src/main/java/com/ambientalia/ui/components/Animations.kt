package com.ambientalia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambientalia.ui.theme.VerdeBosque
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave

/**
 * Efecto de pulsación Duolingo: el elemento se hunde un poquito y vuelve
 * con rebote. Sin ripple: en su lugar escala suave.
 */
@Composable
fun Modifier.duolingoPress(onClick: () -> Unit, enabled: Boolean = true): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Entrada escalonada de listas: cada elemento sube, aparece y crece un poco,
 * con un retardo proporcional a su posición.
 */
@Composable
fun EntradaDuolingo(index: Int, contenido: @Composable () -> Unit) {
    val retardo = (index % 8) * 55
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = 320, delayMillis = retardo)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 420, delayMillis = retardo),
                initialOffsetY = { it / 3 },
            ) +
            scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                initialScale = 0.9f,
            ),
        exit = fadeOut(),
    ) {
        contenido()
    }
}

/** Hoja que rebota mientras la IA piensa. */
@Composable
fun HojaPensando(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "hoja")
    val salto by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "salto",
    )
    val giro by transicion.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "giro",
    )
    Text(
        text = "🌿",
        fontSize = 24.sp,
        modifier = modifier
            .graphicsLayer {
                translationY = -8.dp.toPx() * salto
                rotationZ = giro
            },
    )
}

/** Tres puntitos que laten. */
@Composable
fun PuntosPensando() {
    val transicion = rememberInfiniteTransition(label = "puntos")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by transicion.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = i * 160, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "punto$i",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(8.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(VerdePrincipal, RoundedCornerShape(50)),
            )
        }
    }
}

/** Frases ambientales que rotan mientras la IA trabaja. */
val FRASES_AMBIENTALES = listOf(
    "Regando las plantas…",
    "Cuidando el medio ambiente…",
    "Abonando la tierra…",
    "Podando hojitas secas…",
    "Fotosintetizando…",
    "Dando luz a las hojas…",
    "Compostando restos…",
    "Aclimatando la maceta…",
    "Regando el huerto…",
)

@Composable
fun FraseAmbiental(modifier: Modifier = Modifier) {
    var posicion by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            posicion = (posicion + 1) % FRASES_AMBIENTALES.size
        }
    }
    Crossfade(targetState = posicion, animationSpec = tween(420), label = "frase") { i ->
        Text(
            text = FRASES_AMBIENTALES[i],
            style = MaterialTheme.typography.bodyMedium,
            color = VerdeTextoSuave,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier,
        )
    }
}

/** Bloque completo de "pensando" para el chat. */
@Composable
fun PensandoAmbiental(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HojaPensando()
        Column {
            FraseAmbiental()
            Spacer(Modifier.height(4.dp))
            PuntosPensando()
        }
    }
}

/** Solo la frase ambiental y los puntos: para dentro de una burbuja. */
@Composable
fun PensandoTexto(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FraseAmbiental()
        Spacer(Modifier.height(6.dp))
        PuntosPensando()
    }
}
