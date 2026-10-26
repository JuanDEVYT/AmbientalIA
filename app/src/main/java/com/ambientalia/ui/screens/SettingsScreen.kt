package com.ambientalia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ambientalia.R
import com.ambientalia.ai.AiClient
import com.ambientalia.data.Prefs
import com.ambientalia.notify.WaterReminder
import com.ambientalia.ui.components.ChunkyButton
import com.ambientalia.ui.components.EcoBanner
import com.ambientalia.ui.components.LeafCard
import com.ambientalia.ui.theme.VerdeBosque
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeMenta
import com.ambientalia.ui.theme.VerdeOscuro
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(Prefs.apiKey) }
    var claveVisible by remember { mutableStateOf(false) }
    var modelos by remember {
        mutableStateOf(
            Prefs.modelosConocidos.split(",").map { it.trim() }.filter { it.isNotBlank() },
        )
    }
    var cargandoModelos by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    var activo by remember { mutableStateOf(Prefs.recordatorioActivo) }
    var hora by remember { mutableIntStateOf(Prefs.recordatorioHora) }
    var minuto by remember { mutableIntStateOf(Prefs.recordatorioMinuto) }
    var editandoHora by remember { mutableStateOf(false) }

    fun cargarModelos() {
        if (cargandoModelos) return
        cargandoModelos = true
        scope.launch {
            try {
                AiClient.apiKey = apiKey.ifBlank { Prefs.apiKey }
                val lista = AiClient.listarModelos()
                modelos = lista
                Prefs.modelosConocidos = lista.joinToString(",")
                aviso = if (lista.isEmpty()) "El servidor no devolvió modelos." else "Modelos cargados: ${lista.size}"
            } catch (e: Exception) {
                aviso = e.message?.take(150) ?: "No pude cargar los modelos."
            } finally {
                cargandoModelos = false
            }
        }
    }

    LaunchedEffect(Unit) { if (modelos.isEmpty()) cargarModelos() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // ── Banner ecológico ───────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        EcoBanner(modifier = Modifier.padding(horizontal = 16.dp))

        SectionCard(titulo = stringResource(R.string.eco_title)) {
            Text(
                text = stringResource(R.string.eco_body),
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
        }

        // ── Conexión con JuanAI ────────────────────────────────────────────
        SectionCard(titulo = "Conexión con JuanAI") {
            Text(
                text = "Base URL: https://${AiClient.BASE_URL}",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
            Spacer(Modifier.height(10.dp))

            val shape = RoundedCornerShape(16.dp)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key (sk-…)") },
                singleLine = true,
                visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { claveVisible = !claveVisible }) {
                        Text(if (claveVisible) "Ocultar" else "Ver", color = VerdePrincipal)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = VerdePrincipal,
                    unfocusedBorderColor = VerdeOscuro,
                    cursorColor = VerdePrincipal,
                    focusedTextColor = VerdeBosque,
                    unfocusedTextColor = VerdeBosque,
                ),
                shape = shape,
            )
            Spacer(Modifier.height(12.dp))
            ChunkyButton(
                text = "Guardar clave",
                onClick = {
                    Prefs.apiKey = apiKey
                    aviso = "Clave guardada cifrada en este teléfono ✅"
                    cargarModelos()
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Se guarda cifrada (Keystore del móvil) y solo se envía a tu JuanAI. Si la compartes, usa una key dedicada para esta app.",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
        }

        // ── Modelos ────────────────────────────────────────────────────────
        SectionCard(titulo = "Modelos de IA") {
            SelectorModelo(
                titulo = stringResource(R.string.settings_model),
                valor = Prefs.modelo,
                opciones = modelos,
                onElegir = { Prefs.modelo = it },
            )
            Spacer(Modifier.height(12.dp))
            SelectorModelo(
                titulo = stringResource(R.string.settings_vision_model),
                valor = Prefs.modeloVision,
                opciones = modelos,
                onElegir = { Prefs.modeloVision = it },
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyButton(
                    text = if (cargandoModelos) "Cargando…" else "Actualizar lista",
                    onClick = { cargarModelos() },
                    modifier = Modifier.weight(1f),
                    enabled = !cargandoModelos,
                )
            }
            aviso?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = VerdeTextoSuave)
            }
        }

        // ── Recordatorio de riego ──────────────────────────────────────────
        SectionCard(titulo = "Recordatorio de riego 🪴") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Avisarme cada día",
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Switch(
                    checked = activo,
                    onCheckedChange = {
                        activo = it
                        Prefs.recordatorioActivo = it
                        WaterReminder.programar(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = VerdePrincipal,
                        checkedThumbColor = Color.White,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hora: %02d:%02d".format(hora, minuto),
                style = MaterialTheme.typography.bodyLarge,
                color = VerdeBosque,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    text = "Cambiar hora",
                    onClick = { editandoHora = true },
                    modifier = Modifier.weight(1f),
                    enabled = activo,
                )
                ChunkyButton(
                    text = "Probar aviso",
                    onClick = { WaterReminder.notificar(context) },
                    modifier = Modifier.weight(1f),
                    containerColor = Color.White,
                    contentColor = VerdeBosque,
                )
            }

            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Spacer(Modifier.height(8.dp))
                val permiso = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                ChunkyButton(
                    text = "Permitir notificaciones",
                    onClick = { permiso.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    containerColor = Color.White,
                    contentColor = VerdeBosque,
                )
            }

            if (!WaterReminder.alarmaExactaDisponible(context)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Android no deja usar alarmas exactas: el aviso llegará un poco más tarde.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeTextoSuave,
                )
                Spacer(Modifier.height(8.dp))
                ChunkyButton(
                    text = "Permitir alarmas exactas",
                    onClick = { WaterReminder.pedirAlarmaExacta(context) },
                    containerColor = Color.White,
                    contentColor = VerdeBosque,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Todo es local: no hay servidor de notificaciones ni cuenta de usuario.",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
        }
    }

    if (editandoHora) {
        val estado = rememberTimePickerState(initialHour = hora, initialMinute = minuto, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editandoHora = false },
            containerColor = Color.White,
            title = { Text("Hora del recordatorio", color = VerdeBosque) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hora = estado.hour
                        minuto = estado.minute
                        Prefs.recordatorioHora = hora
                        Prefs.recordatorioMinuto = minuto
                        Prefs.recordatorioActivo = true
                        activo = true
                        WaterReminder.programar(context)
                        editandoHora = false
                    },
                ) { Text("Guardar", color = VerdePrincipal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editandoHora = false }) {
                    Text("Cancelar", color = VerdeTextoSuave)
                }
            },
        )
    }
}

@Composable
private fun SectionCard(titulo: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = VerdeBosque,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LeafCard(color = VerdeHoja) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorModelo(
    titulo: String,
    valor: String,
    opciones: List<String>,
    onElegir: (String) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val lista = if (opciones.contains(valor) || opciones.isEmpty()) opciones else listOf(valor) + opciones

    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
            label = { Text(titulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = VerdePrincipal,
                unfocusedBorderColor = VerdeOscuro,
                focusedTextColor = VerdeBosque,
                unfocusedTextColor = VerdeBosque,
            ),
            shape = RoundedCornerShape(16.dp),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (lista.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin modelos todavía: pulsa «Actualizar lista»") },
                    onClick = { abierto = false },
                )
            }
            lista.forEach { modelo ->
                DropdownMenuItem(
                    text = { Text(modelo) },
                    onClick = {
                        onElegir(modelo)
                        abierto = false
                    },
                )
            }
        }
    }
}
