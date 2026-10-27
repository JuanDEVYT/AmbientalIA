package com.ambientalia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.ambientalia.ai.AiClient
import com.ambientalia.ai.ChatMessage
import com.ambientalia.ai.Prompt
import com.ambientalia.data.Prefs
import com.ambientalia.ui.components.ChunkyButton
import com.ambientalia.ui.components.EcoBanner
import com.ambientalia.ui.components.EntradaDuolingo
import com.ambientalia.ui.components.LeafCard
import com.ambientalia.ui.components.PensandoTexto
import com.ambientalia.ui.theme.VerdeBorde
import com.ambientalia.ui.theme.VerdeBosque
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeOscuro
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class MensajeChat(
    val rol: String,
    val texto: String,
    val cargando: Boolean = false,
    val error: Boolean = false,
)

class ChatViewModel : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val mensajes = mutableStateListOf<MensajeChat>()
    var enviando by mutableStateOf(false)
        private set

    private var job: Job? = null

    init {
        val crudo = Prefs.chatJson
        if (crudo.isNotBlank()) {
            runCatching {
                json.decodeFromString(ListSerializer(MensajeChat.serializer()), crudo)
            }.getOrDefault(emptyList()).forEach { mensajes.add(it) }
        }
    }

    fun enviar(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || enviando) return

        mensajes.add(MensajeChat("user", limpio))
        val indice = mensajes.size
        mensajes.add(MensajeChat("assistant", "", cargando = true))
        enviarIndice(indice)
    }

    /** Reintenta el último mensaje tras un error. */
    fun reintentar() {
        val ultimoUsuario = mensajes.indexOfLast { it.rol == "user" }
        if (ultimoUsuario < 0 || enviando) return
        val indice = mensajes.indexOfFirst { it.rol == "assistant" && it.error }
        if (indice < 0) return
        mensajes[indice] = MensajeChat("assistant", "", cargando = true)
        enviarIndice(indice)
    }

    private fun enviarIndice(indice: Int) {
        if (Prefs.apiKey.isBlank()) {
            mensajes[indice] = MensajeChat(
                "assistant",
                "Aún no hay API key. Ve a Ajustes ⚙️ y pega la key de tu JuanAI para poder chatear.",
                error = true,
            )
            return
        }
        enviando = true
        val historial = historial()
        job = viewModelScope.launch {
            val parcial = StringBuilder()
            try {
                val completo = AiClient.chatStreaming(historial, Prefs.modelo) { trozo ->
                    parcial.append(trozo)
                    if (indice < mensajes.size) {
                        mensajes[indice] = mensajes[indice].copy(texto = parcial.toString())
                    }
                }
                val textoFinal = completo.ifBlank { parcial.toString() }
                mensajes[indice] = MensajeChat(
                    "assistant",
                    textoFinal.ifBlank { "El modelo no devolvió texto. Prueba con otro modelo en Ajustes." },
                )
            } catch (e: Exception) {
                mensajes[indice] = MensajeChat(
                    "assistant",
                    parcial.toString().ifBlank {
                        e.message?.take(160) ?: "No pude conectar con JuanAI."
                    },
                    error = parcial.isEmpty(),
                )
            } finally {
                enviando = false
                guardar()
            }
        }
    }

    private fun historial(): List<ChatMessage> {
        val sistema = ChatMessage("system", JsonPrimitive(Prompt.SYSTEM))
        val visibles = mensajes
            .filter { (it.rol == "user" || it.rol == "assistant") && it.texto.isNotBlank() && !it.error }
            .takeLast(Prompt.HISTORIAL_MAXIMO)
        return listOf(sistema) + visibles.map { ChatMessage(it.rol, JsonPrimitive(it.texto)) }
    }

    private fun guardar() {
        runCatching {
            Prefs.chatJson = json.encodeToString(
                ListSerializer(MensajeChat.serializer()),
                mensajes.takeLast(40).toList(),
            )
        }
    }

    fun limpiar() {
        mensajes.clear()
        guardar()
    }
}

private val SUGERENCIAS = listOf(
    "¿Cuánta agua necesita mi suculenta?",
    "¿Por qué se amarillentan mis hojas?",
    "¿Qué planta va bien con poca luz?",
    "Dame una rutina de riego semanal",
)

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val lista = rememberLazyListState()

    LaunchedEffect(viewModel.mensajes.size) {
        if (viewModel.mensajes.isNotEmpty()) {
            lista.animateScrollToItem(viewModel.mensajes.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EcoBanner(modifier = Modifier.padding(top = 8.dp))

        if (Prefs.apiKey.isBlank()) {
            LeafCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "Falta tu API key de JuanAI",
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pégala en Ajustes ⚙️ para activar el chat. Solo se guarda cifrada en tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeTextoSuave,
                )
            }
        }

        LazyColumn(
            state = lista,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.mensajes.isEmpty()) {
                item { Bienvenida() }
                item { ChipsSugerencias { viewModel.enviar(it) } }
            }
            items(viewModel.mensajes.size) { i ->
                EntradaDuolingo(index = i) {
                    Burbuja(viewModel.mensajes[i], onReintentar = { viewModel.reintentar() })
                }
            }
        }

        CajaDeTexto(
            onSend = { viewModel.enviar(it) },
            bloqueado = viewModel.enviando,
        )
    }
}

@Composable
private fun Bienvenida() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🌱", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "¡Hola! Soy AmbientalIA",
            style = MaterialTheme.typography.headlineSmall,
            color = VerdeBosque,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pregúntame por tus plantas o escanea una con la cámara para que la analice.",
            style = MaterialTheme.typography.bodyMedium,
            color = VerdeTextoSuave,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun ChipsSugerencias(onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SUGERENCIAS.forEach { texto ->
            val shape = RoundedCornerShape(50)
            Text(
                text = texto,
                modifier = Modifier
                    .clip(shape)
                    .background(VerdeHoja, shape)
                    .border(2.dp, VerdeOscuro, shape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { onClick(texto) },
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeBosque,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Burbuja(mensaje: MensajeChat, onReintentar: () -> Unit) {
    val esUsuario = mensaje.rol == "user"
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!esUsuario) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(VerdePrincipal),
                contentAlignment = Alignment.Center,
            ) { Text("🌿", fontSize = 16.sp) }
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.widthIn(max = 300.dp)) {
            val fondo = if (esUsuario) VerdePrincipal else VerdeHoja
            val borde = if (esUsuario) VerdeOscuro else VerdeBorde
            val textoColor = if (esUsuario) Color.White else VerdeBosque

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(fondo, shape)
                    .border(2.dp, borde, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (mensaje.cargando && mensaje.texto.isBlank()) {
                    PensandoTexto()
                } else {
                    Text(
                        text = mensaje.texto,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textoColor,
                    )
                }
            }

            if (mensaje.error && !mensaje.cargando) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚠️ " + if (mensaje.texto.isBlank()) "No pude responder." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeTextoSuave,
                    )
                    Spacer(Modifier.width(8.dp))
                    ChunkyButton(
                        text = "Reintentar",
                        onClick = onReintentar,
                        modifier = Modifier.width(140.dp),
                        shadowHeight = 3.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CajaDeTexto(onSend: (String) -> Unit, bloqueado: Boolean) {
    var texto by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val shape = RoundedCornerShape(18.dp)
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .border(2.dp, VerdeOscuro, shape),
            placeholder = { Text("Pregúntame por tus plantas…", color = VerdeTextoSuave) },
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = VerdePrincipal,
                unfocusedBorderColor = VerdeOscuro,
                cursorColor = VerdePrincipal,
                focusedTextColor = VerdeBosque,
                unfocusedTextColor = VerdeBosque,
            ),
        )
        ChunkyButton(
            text = "Enviar ➤",
            onClick = {
                if (texto.isNotBlank()) {
                    onSend(texto)
                    texto = ""
                }
            },
            modifier = Modifier.width(118.dp),
            enabled = texto.isNotBlank() && !bloqueado,
            shadowHeight = 4.dp,
        )
    }
}
