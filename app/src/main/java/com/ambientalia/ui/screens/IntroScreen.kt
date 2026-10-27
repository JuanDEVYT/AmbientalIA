package com.ambientalia.ui.screens

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ambientalia.R
import kotlinx.coroutines.delay

/**
 * Color exacto del fondo del vídeo de la animación (`intro_riego.webm`, #EEFAE8).
 * Se usa también fuera del vídeo para que no se vea ni un borde de costura.
 */
private val ColorAnimacion = 0xFFEEFAE8.toInt()

/**
 * Pantalla de intro: reproduce la animación de la regadera
 * (`res/raw/intro_riego.webm`, ~3,5 s) al arrancar la app y avisa al terminar.
 *
 * Se usa [TextureView] (y no VideoView/SurfaceView) porque el SurfaceView deja
 * el área que no cubre el vídeo en **negro**; el TextureView es una vista normal:
 * se mide a cuadrado, y lo que quede alrededor es el fondo verde de este Box,
 * así que nunca aparecen barras negras.
 *
 * Tiene dos redes de seguridad: si el vídeo termina → [onTerminado]; y si algo
 * falla (formato no soportado, decode caído) → también, para que la app nunca
 * se quede pegada en la intro.
 */
@Composable
fun IntroScreen(onTerminado: () -> Unit) {
    val contexto = LocalContext.current
    val uri = remember {
        Uri.parse("android.resource://${contexto.packageName}/${R.raw.intro_riego}")
    }

    // Cierre de emergencia: si el vídeo no dispara ni completion ni error.
    LaunchedEffect(Unit) {
        delay(5000)
        onTerminado()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(ColorAnimacion)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                IntroTextureView(ctx, uri, alTerminar = onTerminado)
            },
            onRelease = { it.liberar() },
            // Cuadrado atado al ancho: nunca desborda la pantalla y el vídeo
            // (que es 1:1) se ve completo sin recortes.
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f),
        )
    }
}

/**
 * TextureView con su [MediaPlayer] acoplado al ciclo de vida de la superficie:
 * se crea al haber textura disponible y se libera al destruirla o al retirar la
 * vista. Siempre deja el mismo [alTerminar] como salida.
 */
private class IntroTextureView(
    private val contexto: Context,
    private val uri: Uri,
    private val alTerminar: () -> Unit,
) : TextureView(contexto), TextureView.SurfaceTextureListener {

    private var reproductor: MediaPlayer? = null
    private var superficie: Surface? = null

    init {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(textura: SurfaceTexture, ancho: Int, alto: Int) {
        val borde = Surface(textura)
        superficie = borde
        reproductor = MediaPlayer().apply {
            setDataSource(contexto, uri)
            setSurface(borde)
            setOnPreparedListener { mp ->
                mp.isLooping = false
                mp.start()
            }
            setOnCompletionListener { alTerminar() }
            setOnErrorListener { _, _, _ ->
                alTerminar()
                true
            }
            prepareAsync()
        }
    }

    override fun onSurfaceTextureSizeChanged(textura: SurfaceTexture, ancho: Int, alto: Int) = Unit

    override fun onSurfaceTextureDestroyed(textura: SurfaceTexture): Boolean {
        liberar()
        return true
    }

    override fun onSurfaceTextureUpdated(textura: SurfaceTexture) = Unit

    /** Idempotente: se llama desde la destrucción de la textura y desde onRelease. */
    fun liberar() {
        runCatching { reproductor?.release() }
        runCatching { superficie?.release() }
        reproductor = null
        superficie = null
    }
}
