package com.ambientalia.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ambientalia.R
import com.ambientalia.ui.theme.VerdeFondo
import kotlinx.coroutines.delay

/**
 * Pantalla de intro: reproduce la animación de la regadera
 * (`res/raw/intro_riego.webm`, ~3,5 s) al arrancar la app y avisa al terminar.
 *
 * El video lleva el fondo de la app (#F0FBEB) ya pintado, así que se ve bien en
 * cualquier dispositivo sin depender del soporte de canal alfa del reproductor.
 *
 * Tiene dos redes de seguridad: si el video termina → [onTerminado]; y si algo
 * falla (formato no soportado, decode caído) → también, para que la app nunca
 * se quede pegada en la intro.
 */
@Composable
fun IntroScreen(onTerminado: () -> Unit) {
    val contexto = LocalContext.current
    val uri = remember {
        Uri.parse("android.resource://${contexto.packageName}/${R.raw.intro_riego}")
    }

    // Cierre de emergencia: si el video no dispara ni completion ni error.
    LaunchedEffect(Unit) {
        delay(5000)
        onTerminado()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VerdeFondo),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        start()
                    }
                    setOnCompletionListener { onTerminado() }
                    setOnErrorListener { _, _, _ ->
                        onTerminado()
                        true
                    }
                }
            },
            onRelease = { it.stopPlayback() },
            modifier = Modifier.size(480.dp),
        )
    }
}
