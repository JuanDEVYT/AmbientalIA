package com.ambientalia.ai

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Cliente de JuanAI / 9router: API compatible con OpenAI.
 *
 * Base URL: https://studio-9router.onrender.com/v1
 *  - GET  /models           → lista de modelos ("proveedor/modelo")
 *  - POST /chat/completions → chat, streaming SSE y visión
 *
 * Reglas del documento `Usar-9router-en-otras-apps.md`:
 *  - `Authorization: Bearer <key>` en todas las llamadas.
 *  - Reintento automático de 503 / 429 con espera creciente (1s, 2s, 4s)
 *    y respeto de `Retry-After` (Render despierta el servicio al principio).
 *  - El texto final está en `choices[0].message.content`.
 *  - Con `stream: true` los trozos llegan en `delta.content`.
 */
object AiClient {

    /** URL base de tu JuanAI en Render (termina en /v1). */
    const val BASE_URL = "studio-9router.onrender.com/v1"

    /** Se fija desde Ajustes al arrancar la app. */
    @Volatile
    var apiKey: String = ""

    private const val MAX_INTENTOS = 4

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    class HttpException(val code: Int, override val message: String) : Exception(message)

    // ── Red ────────────────────────────────────────────────────────────────

    private fun builder(path: String, body: String? = null): Request.Builder {
        val url = if (path.startsWith("http")) path else "https://$BASE_URL$path"
        val b = Request.Builder().url(url)
        if (apiKey.isNotBlank()) b.header("Authorization", "Bearer $apiKey")
        if (body != null) b.post(body.toRequestBody(JSON_MEDIA))
        return b
    }

    private fun Response.bodyOrThrow(): String {
        val text = body?.string().orEmpty()
        if (!isSuccessful) {
            val detalle = text.take(300)
            throw HttpException(code, "HTTP $code $detalle")
        }
        if (text.isBlank()) throw HttpException(code, "Respuesta vacía del servidor")
        return text
    }

    /** Reintenta 503/429/5xx e IOException con espera 1s → 2s → 4s. */
    private suspend fun <T> reintentando(block: suspend () -> T): T {
        var espera = 1_000L
        var ultimo: Throwable? = null
        repeat(MAX_INTENTOS) { intento ->
            try {
                return block()
            } catch (e: HttpException) {
                val reintentable = e.code == 429 || e.code == 503 || e.code >= 500
                if (!reintentable) throw e
                ultimo = e
            } catch (e: IOException) {
                ultimo = e
            }
            if (intento < MAX_INTENTOS - 1) {
                delay(espera)
                espera *= 2
            }
        }
        throw ultimo ?: IOException("No se pudo conectar con JuanAI")
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isCancelled) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resumeWith(Result.success(response)) else response.close()
                }
            },
        )
        cont.invokeOnCancellation { cancel() }
    }

    // ── API ────────────────────────────────────────────────────────────────

    /** GET /models — nombres exactos "proveedor/modelo". */
    suspend fun listarModelos(): List<String> = reintentando {
        val req = builder("/models").build()
        http.newCall(req).await().use { resp ->
            val texto = resp.bodyOrThrow()
            json.decodeFromString<ModelList>(texto).data.map { it.id }.sorted()
        }
    }

    /** POST /chat/completions sin streaming (respuestas cortas, escáner). */
    suspend fun chat(mensajes: List<ChatMessage>, modelo: String): String = reintentando {
        val payload = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = modelo, messages = mensajes, stream = false, temperature = 0.7),
        )
        val req = builder("/chat/completions", payload).build()
        http.newCall(req).await().use { resp ->
            val texto = resp.bodyOrThrow()
            json.decodeFromString<ChatResponse>(texto)
                .choices.firstOrNull()?.message?.content
                ?: throw HttpException(200, "El modelo no devolvió contenido")
        }
    }

    /**
     * POST /chat/completions con `stream: true`.
     * Va soltando cada trozo por [onDelta] y devuelve el texto completo.
     * Si el endpoint no acepta streaming, cae a la versión normal.
     */
    suspend fun chatStreaming(
        mensajes: List<ChatMessage>,
        modelo: String,
        onDelta: (String) -> Unit,
    ): String = reintentando {
        val payload = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(model = modelo, messages = mensajes, stream = true, temperature = 0.7),
        )
        val req = builder("/chat/completions", payload).build()
        http.newCall(req).await().use { resp ->
            if (!resp.isSuccessful) {
                val error = resp.body?.string().orEmpty()
                // Algunos proveedores no aceptan streaming: lo intentamos sin él.
                if (resp.code == 400 || resp.code == 404) {
                    return@use chat(mensajes, modelo)
                }
                throw HttpException(resp.code, "HTTP ${resp.code} ${error.take(300)}")
            }
            leerSSE(resp, onDelta)
        }
    }

    private fun leerSSE(resp: Response, onDelta: (String) -> Unit): String {
        val source = resp.body?.source() ?: return ""
        val sb = StringBuilder()
        while (true) {
            val linea = source.readUtf8Line() ?: break
            if (linea.isBlank()) continue
            if (!linea.startsWith("data:")) continue
            val datos = linea.removePrefix("data:").trim()
            if (datos == "[DONE]") break
            val chunk = runCatching { json.decodeFromString<StreamChunk>(datos) }.getOrNull() ?: continue
            val delta = chunk.choices.firstOrNull()?.delta ?: continue
            val trozo = delta.content
            if (!trozo.isNullOrEmpty()) {
                sb.append(trozo)
                // El "pensamiento" de modelos razonadores (reasoning_content) no se muestra.
                onDelta(trozo)
            }
        }
        return sb.toString()
    }

    /** Imagen en base64 → diagnóstico JSON de la planta. */
    suspend fun escanearPlanta(
        imagenBase64: String,
        contexto: String,
        modelo: String,
    ): String {
        val partes = listOf(
            ContentPart(type = "text", text = Prompt.escanerPrompt(contexto)),
            ContentPart(
                type = "image_url",
                imageUrl = ImageUrl("data:image/jpeg;base64,$imagenBase64"),
            ),
        )
        val mensaje = ChatMessage(
            role = "user",
            content = json.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(ContentPart.serializer()),
                partes,
            ),
        )
        val sistema = ChatMessage(role = "system", content = json.encodeToJsonElement(
            kotlinx.serialization.builtins.String.serializer(),
            Prompt.SYSTEM,
        ))
        return chat(listOf(sistema, mensaje), modelo)
    }
}
