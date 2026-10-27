package com.ambientalia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambientalia.data.MisPlantas
import com.ambientalia.data.PlantaCatalogo
import com.ambientalia.data.PlantaUsuario
import com.ambientalia.data.PlantCatalog
import com.ambientalia.ui.components.ChunkyButton
import com.ambientalia.ui.components.DuolingoProgress
import com.ambientalia.ui.components.EntradaDuolingo
import com.ambientalia.ui.components.LeafCard
import com.ambientalia.ui.components.PlantArt
import com.ambientalia.ui.theme.Ambar
import com.ambientalia.ui.theme.NaranjaAlerta
import com.ambientalia.ui.theme.RojoRiego
import com.ambientalia.ui.theme.VerdeBosque
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeMenta
import com.ambientalia.ui.theme.VerdeOscuro
import com.ambientalia.ui.theme.VerdePrincipal
import com.ambientalia.ui.theme.VerdeTextoSuave
import kotlinx.coroutines.delay

@Composable
fun PlantsScreen() {
    var pestana by remember { mutableIntStateOf(0) }
    var detallePlanta by remember { mutableStateOf<PlantaUsuario?>(null) }
    var detalleCatalogo by remember { mutableStateOf<PlantaCatalogo?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Pestanas(
            seleccion = pestana,
            onSeleccion = { pestana = it },
        )

        Crossfade(targetState = pestana, animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow), label = "pestanas") { t ->
            if (t == 0) {
                ListaMisPlantas(
                    onDetalle = { detallePlanta = it },
                    onIrACatalogo = { pestana = 1 },
                )
            } else {
                Catalogo(onDetalle = { detalleCatalogo = it })
            }
        }
    }

    detallePlanta?.let { planta ->
        DetallePlanta(planta = planta, onCerrar = { detallePlanta = null })
    }
    detalleCatalogo?.let { catalogo ->
        DetalleCatalogo(catalogo = catalogo, onCerrar = { detalleCatalogo = null })
    }
}

// ── Pestañas ───────────────────────────────────────────────────────────────

@Composable
private fun Pestanas(seleccion: Int, onSeleccion: (Int) -> Unit) {
    val etiquetas = listOf("🪴 Mis plantas", "📚 Catálogo")
    TabRow(
        selectedTabIndex = seleccion,
        containerColor = VerdeMenta,
        contentColor = VerdePrincipal,
        indicator = { posiciones ->
            val pos = posiciones[seleccion]
            val izq by animateDpAsState(
                targetValue = pos.left + 14.dp,
                animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                label = "ind",
            )
            val ancho by animateDpAsState(
                targetValue = (pos.width - 28.dp).coerceAtLeast(24.dp),
                animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                label = "indw",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.BottomStart)
                    .offset(x = izq)
                    .width(ancho)
                    .height(5.dp)
                    .background(VerdePrincipal, RoundedCornerShape(50)),
            )
        },
    ) {
        etiquetas.forEachIndexed { i, texto ->
            val elegida = seleccion == i
            val escala by animateFloatAsState(
                targetValue = if (elegida) 1.08f else 1f,
                animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                label = "escalaPestana",
            )
            Tab(
                selected = elegida,
                onClick = { onSeleccion(i) },
                selectedContentColor = VerdeBosque,
                unselectedContentColor = VerdeTextoSuave,
                text = {
                    Text(
                        text = texto,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                        },
                    )
                },
            )
        }
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
            Spacer(Modifier.height(32.dp))
            PlantArt(
                nombre = "Maceta vacía",
                catalogId = null,
                modifier = Modifier,
                size = 140.dp,
            )
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            ChunkyButton(text = "Ver el catálogo", onClick = onIrACatalogo)
            return
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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

    var regando by remember { mutableStateOf(false) }
    LaunchedEffect(regando) {
        if (regando) {
            delay(1400)
            regando = false
        }
    }

    val escalaProgreso by animateFloatAsState(
        targetValue = if (sedienta) 1.02f else 1f,
        animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy),
        label = "progreso",
    )

    Box {
        LeafCard(color = Color.White, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(VerdeMenta)
                        .border(2.dp, VerdeOscuro, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlantArt(
                        nombre = planta.nombre,
                        catalogId = planta.catalogId,
                        foto = planta.foto,
                        size = 58.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planta.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        color = VerdeBosque,
                    )
                    Text(
                        text = if (sedienta) {
                            "💧 Necesita agua hoy"
                        } else {
                            "Riego cada ${planta.diasRiego} días"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sedienta) RojoRiego else VerdeTextoSuave,
                        fontWeight = if (sedienta) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NIVEL", style = MaterialTheme.typography.labelMedium, color = VerdeTextoSuave)
                    Text(
                        text = "${planta.nivel}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Ambar,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.graphicsLayer {
                scaleX = escalaProgreso
                scaleY = escalaProgreso
            }) {
                DuolingoProgress(
                    progress = avance.coerceAtMost(1f),
                    trackColor = Color(0xFFEFF7EE),
                    fillColor = if (sedienta) RojoRiego else VerdePrincipal,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$dias / $maximo días sin regar",
                    style = MaterialTheme.typography.labelMedium,
                    color = VerdeTextoSuave,
                )
                ChunkyButton(
                    text = "Regar 💧",
                    modifier = Modifier.width(140.dp),
                    shadowHeight = 3.dp,
                    onClick = {
                        MisPlantas.regar(planta.id)
                        regando = true
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = regando,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) +
                scaleIn(initialScale = 0.7f),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp),
        ) {
            val shape = RoundedCornerShape(50)
            Text(
                text = "💧 +1 nivel",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .background(VerdePrincipal, shape)
                    .border(2.dp, VerdeOscuro, shape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DetallePlanta(planta: PlantaUsuario, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VerdeMenta)
                        .border(2.dp, VerdeOscuro, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlantArt(nombre = planta.nombre, catalogId = planta.catalogId, foto = planta.foto, size = 50.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(planta.nombre, color = VerdeBosque, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Nivel ${planta.nivel}",
                        color = Ambar,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
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
        columns = GridCells.Adaptive(minSize = 158.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(PlantCatalog.plantas.withIndex().toList(), key = { it.value.id }) { par ->
            val planta = par.value
            EntradaDuolingo(index = par.index) {
                LeafCard(color = VerdeHoja, onClick = { onDetalle(planta) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.75f))
                            .border(2.dp, VerdeOscuro, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlantArt(nombre = planta.nombre, catalogId = planta.id, size = 86.dp)
                    }
                    Spacer(Modifier.height(8.dp))
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
}

@Composable
private fun DetalleCatalogo(catalogo: PlantaCatalogo, onCerrar: () -> Unit) {
    val yaGuardada = MisPlantas.plantas.any { it.catalogId == catalogo.id }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VerdeMenta)
                        .border(2.dp, VerdeOscuro, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlantArt(nombre = catalogo.nombre, catalogId = catalogo.id, size = 50.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(catalogo.nombre, color = VerdeBosque, style = MaterialTheme.typography.titleLarge)
                    Text(catalogo.cientifico, color = VerdeTextoSuave, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            diasRiego = catalogo.diasRiego,
                            luz = catalogo.luz,
                            ultimoRiego = System.currentTimeMillis(),
                            fechaAlta = System.currentTimeMillis(),
                        ),
                    )
                    onCerrar()
                },
                enabled = !yaGuardada,
            ) {
                Text(
                    text = if (yaGuardada) "Ya la tienes" else "Añadir a mis plantas",
                    color = if (yaGuardada) VerdeTextoSuave else VerdePrincipal,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar", color = VerdeTextoSuave) }
        },
    )
}
