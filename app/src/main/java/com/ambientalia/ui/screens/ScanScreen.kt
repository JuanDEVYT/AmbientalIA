package com.ambientalia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.ambientalia.ai.AiClient
import com.ambientalia.ai.Prompt
import com.ambientalia.data.MisPlantas
import com.ambientalia.data.Prefs
import com.ambientalia.data.PlantaUsuario
import com.ambientalia.ui.components.ChunkyButton
import com.ambientalia.ui.components.EcoBanner
import com.ambientalia.ui.components.LeafCard
import com.ambientalia.ui.theme.Ambar
import com.ambientalia.ui.theme.NaranjaAlerta
import com.ambientalia.ui.theme.RojoRiego
import com.ambientalia.ui.theme.VerdeBosque
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeMenta
import com.ambientalia.ui.theme.VerdeOscuro
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File

// ── Respuesta estructurada del modelo con visión ───────────────────────────

@Serializable
data class RiegoInfo(
    @SerialName("frecuencia_dias") val frecuenciaDias: Int = 7,
    val cantidad: String = "",
    val senal: String = "",
)

@Serializable
data class AnalisisPlanta(
    val nombre: String = "Planta desconocida",
    @SerialName("nombre_cientifico") val cientifico: String = "",
    val emoji: String = "🌿",
    val estado: String = "desconocido",
    val confianza: Double = 0.5,
    val resumen: String = "",
    val problemas: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList(),
    val riego: RiegoInfo? = null,
    val luz: String = "",
)

private val JSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

/** Redimensiona a 1024 px y codifica en base64: menos peso = menos energía. */
private fun prepararImagen(file: File): String? = runCatching {
    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    val maximo = 1024f
    val factor = minOf(1f, maximo / maxOf(original.width, original.height).toFloat())
    val redim = if (factor < 1f) {
        Bitmap.createScaledBitmap(
            original,
            (original.width * factor).toInt().coerceAtLeast(1),
            (original.height * factor).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        original
    }
    val salida = ByteArrayOutputStream()
    redim.compress(Bitmap.CompressFormat.JPEG, 80, salida)
    Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

// ── Pantalla ───────────────────────────────────────────────────────────────

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mostrandoCamara by remember { mutableStateOf(false) }
    var foto by remember { mutableStateOf<File?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resultado by remember { mutableStateOf<AnalisisPlanta?>(null) }
    var guardado by remember { mutableStateOf(false) }

    fun analizar(file: File) {
        foto = file
        resultado = null
        guardado = false
        error = null
        val base64 = prepararImagen(file)
        if (base64 == null) {
            cargando = false
            error = "No pude leer esa foto. Prueba con otra."
            return
        }
        if (Prefs.apiKey.isBlank()) {
            cargando = false
            error = "Falta tu API key: pégala en Ajustes ⚙️ para analizar plantas."
            return
        }
        cargando = true
        scope.launch {
            try {
                val texto = AiClient.escanearPlanta(
                    imagenBase64 = base64,
                    contexto = "",
                    modelo = Prefs.modeloVision,
                )
                resultado = JSON.decodeFromString(
                    AnalisisPlanta.serializer(),
                    Prompt.limpiarJson(texto),
                )
                error = null
            } catch (e: Exception) {
                resultado = null
                error = e.message?.take(160) ?: "No pude analizar la foto."
            } finally {
                cargando = false
                mostrandoCamara = false
            }
        }
    }

    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido -> mostrandoCamara = concedido }

    fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            mostrandoCamara = true
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copiarDeGaleria(context, uri) { archivo, fallo ->
            if (archivo != null) analizar(archivo) else error = fallo
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        EcoBanner(modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(8.dp))
        Text(
            text = "📸 Escáner de plantas",
            style = MaterialTheme.typography.headlineSmall,
            color = VerdeBosque,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "La IA mira la foto y te dice qué planta es, cómo está y qué necesita.",
            style = MaterialTheme.typography.bodyMedium,
            color = VerdeTextoSuave,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))

        when {
            cargando -> TarjetaCargando()
            resultado != null -> TarjetaResultado(
                analisis = resultado!!,
                guardado = guardado,
                foto = foto,
                onGuardar = {
                    guardarPlanta(resultado!!, foto)
                    guardado = true
                },
                onOtra = {
                    resultado = null
                    foto = null
                    guardado = false
                    error = null
                },
            )
            error != null -> TarjetaError(error!!) {
                val actual = foto
                if (actual != null) analizar(actual) else abrirCamara()
            }
            else -> TarjetaAcciones(
                mostrandoCamara = mostrandoCamara,
                onTomarFoto = ::abrirCamara,
                onGaleria = { galeria.launch("image/*") },
                onCerrarCamara = { mostrandoCamara = false },
                onCapturada = { archivo -> if (archivo != null) analizar(archivo) },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Cámara ─────────────────────────────────────────────────────────────────

@Composable
private fun TarjetaAcciones(
    mostrandoCamara: Boolean,
    onTomarFoto: () -> Unit,
    onGaleria: () -> Unit,
    onCerrarCamara: () -> Unit,
    onCapturada: (File?) -> Unit,
) {
    if (mostrandoCamara) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 16.dp),
        ) {
            CamaraPreview(onCapturada = onCapturada, onCerrar = onCerrarCamara)
        }
        return
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LeafCard(color = VerdeHoja) {
            Text(
                text = "Sube una foto de la planta",
                style = MaterialTheme.typography.titleMedium,
                color = VerdeBosque,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Busca buena luz y encuadra hojas y tallo. La foto se recorta a 1024 px antes de enviarse.",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
            Spacer(Modifier.height(14.dp))
            ChunkyButton(
                text = "Abrir cámara",
                onClick = onTomarFoto,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            ChunkyButton(
                text = "Elegir de la galería",
                onClick = onGaleria,
                containerColor = Color.White,
                contentColor = VerdeBosque,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = VerdeBosque,
                    )
                },
            )
        }
    }
}

@Composable
private fun CamaraPreview(onCapturada: (File?) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val futuro = ProcessCameraProvider.getInstance(context)
        futuro.addListener(
            {
                runCatching {
                    val proveedor = futuro.get()
                    proveedor.unbindAll()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    proveedor.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun disparar() {
        val archivo = File(context.cacheDir, "planta_${System.currentTimeMillis()}.jpg")
        val destino = ImageCapture.OutputFileOptions.Builder(archivo).build()
        imageCapture.takePicture(
            destino,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) =
                    onCapturada(archivo)

                override fun onError(exception: ImageCaptureException) = onCapturada(null)
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onCerrar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.40f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar cámara", tint = Color.White)
        }

        // Disparo estilo Duolingo
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(76.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(5.dp, VerdePrincipal, RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { disparar() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(VerdePrincipal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Disparar",
                    tint = Color.White,
                )
            }
        }
    }
}

// ── Utilidades ─────────────────────────────────────────────────────────────

private fun copiarDeGaleria(
    context: android.content.Context,
    uri: Uri,
    onListo: (File?, String?) -> Unit,
) {
    runCatching {
        val destino = File(context.cacheDir, "galeria_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        onListo(destino, null)
    }.onFailure { onListo(null, "No pude abrir esa imagen.") }
}

private fun guardarPlanta(analisis: AnalisisPlanta, foto: File?) {
    MisPlantas.agregar(
        PlantaUsuario(
            id = "p_${System.currentTimeMillis()}",
            nombre = analisis.nombre,
            cientifico = analisis.cientifico,
            emoji = analisis.emoji.ifBlank { "🌿" },
            foto = foto?.absolutePath,
            diasRiego = analisis.riego?.frecuenciaDias?.takeIf { it > 0 } ?: 7,
            luz = analisis.luz,
            estado = analisis.estado,
            resumen = analisis.resumen,
            problemas = analisis.problemas,
            recomendaciones = analisis.recomendaciones,
            senalRiego = analisis.riego?.senal.orEmpty(),
            ultimoRiego = System.currentTimeMillis(),
            fechaAlta = System.currentTimeMillis(),
        ),
    )
}

// ── Tarjetas de resultado ──────────────────────────────────────────────────

@Composable
private fun TarjetaCargando() {
    LeafCard(modifier = Modifier.padding(horizontal = 16.dp), color = VerdeHoja) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(color = VerdePrincipal)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Analizando la planta…",
                style = MaterialTheme.typography.titleMedium,
                color = VerdeBosque,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "El modelo con visión está mirando hojas, tallo y color.",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TarjetaError(mensaje: String, onReintentar: () -> Unit) {
    LeafCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Color(0xFFFFF4E5),
        borderColor = NaranjaAlerta,
    ) {
        Text("⚠️", fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "No pude analizar la foto",
            style = MaterialTheme.typography.titleMedium,
            color = VerdeBosque,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = mensaje, style = MaterialTheme.typography.bodyMedium, color = VerdeTextoSuave)
        Spacer(Modifier.height(14.dp))
        ChunkyButton(text = "Reintentar", onClick = onReintentar)
    }
}

@Composable
private fun TarjetaResultado(
    analisis: AnalisisPlanta,
    guardado: Boolean,
    foto: File?,
    onGuardar: () -> Unit,
    onOtra: () -> Unit,
) {
    val (colorEstado, textoEstado) = when (analisis.estado.lowercase()) {
        "sano" -> VerdePrincipal to "Sana"
        "estres" -> NaranjaAlerta to "Estresada"
        "plaga" -> RojoRiego to "Con plagas"
        "riego" -> Ambar to "Necesita agua"
        else -> VerdeTextoSuave to "Sin identificar"
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LeafCard(color = Color.White) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (foto != null) {
                    AsyncImage(
                        model = foto,
                        contentDescription = analisis.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, VerdeOscuro, RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "${analisis.emoji} ${analisis.nombre}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VerdeBosque,
                    )
                    if (analisis.cientifico.isNotBlank()) {
                        Text(
                            text = analisis.cientifico,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VerdeTextoSuave,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val shape = RoundedCornerShape(50)
                    Text(
                        text = textoEstado.uppercase(),
                        modifier = Modifier
                            .clip(shape)
                            .background(colorEstado, shape)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (analisis.resumen.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = analisis.resumen,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VerdeBosque,
                )
            }
        }

        if (analisis.problemas.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LeafCard(color = Color(0xFFFFF4E5), borderColor = NaranjaAlerta) {
                Text(
                    text = "Lo que le falta",
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Spacer(Modifier.height(6.dp))
                analisis.problemas.forEach {
                    Text(
                        text = "• $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeTextoSuave,
                    )
                }
            }
        }

        if (analisis.recomendaciones.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LeafCard(color = VerdeHoja) {
                Text(
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Spacer(Modifier.height(6.dp))
                analisis.recomendaciones.forEach {
                    Text(
                        text = "✅ $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeBosque,
                    )
                }
            }
        }

        val riego = analisis.riego
        if (riego != null) {
            Spacer(Modifier.height(12.dp))
            LeafCard(color = VerdeMenta) {
                Text(text = "💧 Riego", style = MaterialTheme.typography.titleMedium, color = VerdeBosque)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cada ${riego.frecuenciaDias} días · ${riego.cantidad.ifBlank { "cantidad normal" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeBosque,
                )
                if (riego.senal.isNotBlank()) {
                    Text(
                        text = "Señal: ${riego.senal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeTextoSuave,
                    )
                }
            }
        }

        if (analisis.luz.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            LeafCard(color = VerdeHoja) {
                Text(text = "☀️ Luz", style = MaterialTheme.typography.titleMedium, color = VerdeBosque)
                Text(
                    text = analisis.luz,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeTextoSuave,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (guardado) {
            Text(
                text = "✅ Guardada en Mis plantas",
                style = MaterialTheme.typography.titleMedium,
                color = VerdePrincipal,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        } else {
            ChunkyButton(text = "Guardar en mis plantas", onClick = onGuardar)
            Spacer(Modifier.height(12.dp))
        }
        ChunkyButton(
            text = "Analizar otra planta",
            onClick = onOtra,
            containerColor = Color.White,
            contentColor = VerdeBosque,
        )
    }
}
