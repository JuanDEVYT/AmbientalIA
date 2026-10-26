package com.ambientalia.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeBorde
import com.ambientalia.ui.theme.VerdeOscuro
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave
import com.ambientalia.ui.theme.VerdeMenta
import com.ambientalia.ui.theme.VerdeBosque

/** Sombra sólida estilo Duolingo: sin blur, desplazada hacia abajo. */
@Composable
fun Modifier.chunkyShadow(height: Dp = 4.dp, color: Color = VerdeOscuro): Modifier = this.then(
    Modifier.graphicsLayer { translationY = height.toPx() }.background(color, RoundedCornerShape(18.dp)),
)

/**
 * Botón "abultado": base sólida verde oscuro debajo y cara verde encima.
 * Se hunde 4 dp al tocarlo.
 */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = VerdePrincipal,
    contentColor: Color = Color.White,
    shadowHeight: Dp = 4.dp,
    icon: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed) shadowHeight else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sink",
    )
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        // Base (sombra sólida)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .graphicsLayer { translationY = shadowHeight.toPx() }
                .background(VerdeOscuro, shape),
        )
        // Cara
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .graphicsLayer { translationY = sink.toPx() }
                .clip(shape)
                .background(if (enabled) containerColor else VerdeHoja, shape)
                .border(2.dp, if (enabled) VerdeOscuro else VerdeBorde, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) contentColor else VerdeTextoSuave,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Tarjeta verde con borde grueso: el bloque base de toda la app. */
@Composable
fun LeafCard(
    modifier: Modifier = Modifier,
    color: Color = VerdeHoja,
    borderColor: Color = VerdeOscuro,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color, shape)
            .border(2.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Barra de progreso con estilo Duolingo: relleno redondeado + borde. */
@Composable
fun DuolingoProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    trackColor: Color = Color.White,
    fillColor: Color = VerdePrincipal,
) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress")
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor, shape)
            .border(2.dp, VerdeBorde, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = animated.coerceAtLeast(0.02f) }
                .background(fillColor, shape),
        )
    }
}

/** Banner ecológico: el claim de "agua mínima o nula" visible en la interfaz. */
@Composable
fun EcoBanner(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VerdeMenta, shape)
            .border(2.dp, VerdeBorde, shape)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Punto verde "vivo"
        Box(
            modifier = Modifier
                .size(if (compact) 10.dp else 12.dp)
                .clip(RoundedCornerShape(50))
                .background(VerdePrincipal),
        )
        Column {
            Text(
                text = if (compact) "Servidores con consumo de agua mínimo o nulo" else "IA con agua mínima o nula",
                style = MaterialTheme.typography.labelLarge,
                color = VerdeBosque,
            )
            if (!compact) {
                Text(
                    text = "Nuestros modelos corren en centros de datos con consumo de agua mínimo o nulo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeTextoSuave,
                )
            }
        }
    }
}

/** Título de sección con emoji, estilo Duolingo. */
@Composable
fun SectionTitle(emoji: String, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = VerdeBosque,
        )
    }
}

/** Icono dentro de una pastilla verde. */
@Composable
fun IconPill(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    container: Color = VerdePrincipal,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint)
    }
}

/** Texto con sombra sólida sutil para títulos grandes. */
@Composable
fun DisplayText(text: String, modifier: Modifier = Modifier, color: Color = VerdeBosque) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.displaySmall.copy(
            shadow = Shadow(color = VerdeOscuro, offset = Offset(0f, 2f), blurRadius = 0f),
        ),
    )
}
