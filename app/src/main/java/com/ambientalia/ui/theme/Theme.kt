package com.ambientalia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeScheme = lightColorScheme(
    primary = VerdePrincipal,
    onPrimary = Color.White,
    primaryContainer = VerdeHoja,
    onPrimaryContainer = VerdeBosque,
    secondary = VerdeOscuro,
    onSecondary = Color.White,
    secondaryContainer = VerdeMenta,
    onSecondaryContainer = VerdeBosque,
    tertiary = Ambar,
    onTertiary = VerdeBosque,
    background = VerdeFondo,
    onBackground = VerdeBosque,
    surface = VerdeFondo,
    onSurface = VerdeBosque,
    surfaceVariant = VerdeHoja,
    onSurfaceVariant = VerdeTextoSuave,
    outline = VerdeBorde,
    outlineVariant = VerdeHoja,
    error = RojoRiego,
    onError = Color.White,
)

@Composable
fun AmbientalIaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VerdeScheme,
        typography = AmbientalTypography,
        content = content,
    )
}
