package com.ambientalia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ambientalia.data.MisPlantas
import com.ambientalia.data.PlantaCatalogo
import com.ambientalia.data.PlantaUsuario
import com.ambientalia.data.PlantCatalog
import com.ambientalia.ui.components.ChunkyButton
import com.ambientalia.ui.components.DuolingoProgress
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

@Composable
fun PlantsScreen() {
    var pestana by remember { mutableIntStateOf(0) }
    var detallePlanta by remember { mutableStateOf<PlantaUsuario?>(null) }
    var detalleCatalogo by remember { mutableStateOf<PlantaCatalogo?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestana, containerColor = VerdeMenta) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text("Mis plantas", fontWeight = FontWeight.Bold) },
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text("Catálogo", fontWeight = FontWeight.Bold) },
            )
        }

        if (pestana == 0) {
            ListaMisPlantas(
                onDetalle = { detallePlanta = it },
                onIrACatalogo = { pestana = 1 },
            )
        } else {
            Catalogo(onDetalle = { detalleCatalogo = it })
        }
    }

    detallePlanta?.let { planta ->
        DetallePlanta(planta = planta, onCerrar = { detallePlanta = null })
    }
    detalleCatalogo?.let { catalogo ->
        DetalleCatalogo(catalogo = catalogo, onCerrar = { detalleCatalogo = null })
    }
}

// ── Mis plantas ────────────────────────────────────────────────────────────

@Composable
private fun ListaMisPlantas(onDetalle: (PlantaUsuario) -> Unit, onIrACatalogo: () -> Unit) {
    val lista = MisPlantas.plantas

    if (lista.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text("🪴", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Todavía no tienes plantas",
                style = MaterialTheme.typography.headlineSmall,
                color = VerdeBosque,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Escanea una con la cámara o añádela desde el catálogo.",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeTextoSuave,
            )
            Spacer(Modifier.height(18.dp))
            ChunkyButton(text = "Ver el catálogo", onClick = onIrACatalogo)
            return
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lista, key = { it.id }) { planta ->
            TarjetaPlanta(planta = planta, onClick = { onDetalle(planta) })
        }
    }
}

@Composable
private fun TarjetaPlanta(planta: PlantaUsuario, onClick: () -> Unit) {
    val dias = MisPlantas.diasSinRegar(planta)
    val maximo = planta.diasRiego.coerceAtLeast(1)
    val avance = dias.toFloat() / maximo
    val sedienta = dias >= planta.diasRiego

    LeafCard(color = Color.White, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VerdeHoja)
                    .border(2.dp, VerdeOscuro, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(planta.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Text(
                    text = if (sedienta) "💧 Necesita agua hoy" else "Nivel ${planta.nivel} · riego cada ${planta.diasRiego} días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sedienta) RojoRiego else VerdeTextoSuave,
                    fontWeight = if (sedienta) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Text("Lv.${planta.nivel}", style = MaterialTheme.typography.labelLarge, color = Ambar)
        }

        Spacer(Modifier.height(12.dp))
        DuolingoProgress(
            progress = avance.coerceAtMost(1f),
            trackColor = Color(0xFFEFF7EE),
            fillColor = if (sedienta) RojoRiego else VerdePrincipal,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$dias / $maximo días sin regar",
            style = MaterialTheme.typography.labelMedium,
            color = VerdeTextoSuave,
        )
    }
}

@Composable
private fun DetallePlanta(planta: PlantaUsuario, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = {
            Text(
                text = "${planta.emoji} ${planta.nombre}",
                color = VerdeBosque,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (planta.cientifico.isNotBlank()) {
                    Text(planta.cientifico, color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (planta.resumen.isNotBlank()) {
                    Text(planta.resumen, color = VerdeBosque, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                }
                if (planta.problemas.isNotEmpty()) {
                    Text("Lo que le falta", color = VerdeBosque, style = MaterialTheme.typography.titleMedium)
                    planta.problemas.forEach {
                        Text("• $it", color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (planta.recomendaciones.isNotEmpty()) {
                    Text("Recomendaciones", color = VerdeBosque, style = MaterialTheme.typography.titleMedium)
                    planta.recomendaciones.forEach {
                        Text("✅ $it", color = VerdeBosque, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (planta.luz.isNotBlank()) {
                    Text("☀️ ${planta.luz}", color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                }
                if (planta.senalRiego.isNotBlank()) {
                    Text("💧 ${planta.senalRiego}", color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { MisPlantas.regar(planta.id); onCerrar() }) {
                Text("Regar 💧", color = VerdePrincipal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { MisPlantas.eliminar(planta.id); onCerrar() }) {
                Text("Eliminar", color = RojoRiego)
            }
        },
    )
}

// ── Catálogo ───────────────────────────────────────────────────────────────

@Composable
private fun Catalogo(onDetalle: (PlantaCatalogo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(PlantCatalog.plantas, key = { it.id }) { planta ->
            LeafCard(color = VerdeHoja, onClick = { onDetalle(planta) }) {
                Text(planta.emoji, fontSize = 34.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = planta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeBosque,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "💧 cada ${planta.diasRiego} días",
                    style = MaterialTheme.typography.labelMedium,
                    color = VerdeTextoSuave,
                )
                Text(
                    text = "☀️ ${planta.luz}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeTextoSuave,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun DetalleCatalogo(catalogo: PlantaCatalogo, onCerrar: () -> Unit) {
    val yaAguardada = MisPlantas.plantas.any { it.catalogId == catalogo.id }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = {
            Text(
                text = "${catalogo.emoji} ${catalogo.nombre}",
                color = VerdeBosque,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(catalogo.cientifico, color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "💧 Riego cada ${catalogo.diasRiego} días · ☀️ ${catalogo.luz}",
                    color = VerdeBosque,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Dificultad: ${catalogo.dificultad}",
                    color = VerdeTextoSuave,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text("Cuidados", color = VerdeBosque, style = MaterialTheme.typography.titleMedium)
                catalogo.cuidados.forEach {
                    Text("• $it", color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(10.dp))
                LeafCard(color = VerdeMenta) {
                    Text(catalogo.dato, color = VerdeBosque, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    MisPlantas.agregar(
                        PlantaUsuario(
                            id = "c_${catalogo.id}_${System.currentTimeMillis()}",
                            catalogId = catalogo.id,
                            nombre = catalogo.nombre,
                            cientifico = catalogo.cientifico,
                            emoji = catalogo.emoji,
                            diasRiego = catalogo.diasRiego,
                            luz = catalogo.luz,
                            ultimoRiego = System.currentTimeMillis(),
                            fechaAlta = System.currentTimeMillis(),
                        ),
                    )
                    onCerrar()
                },
                enabled = !yaAguardada,
            ) {
                Text(
                    text = if (yaAguardada) "Ya la tienes" else "Añadir a mis plantas",
                    color = if (yaAguardada) VerdeTextoSuave else VerdePrincipal,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar", color = VerdeTextoSuave) }
        },
    )
}
