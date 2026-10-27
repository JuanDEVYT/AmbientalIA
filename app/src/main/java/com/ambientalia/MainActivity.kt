package com.ambientalia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ambientalia.notify.AlarmaOverlay
import com.ambientalia.ui.screens.ChatScreen
import com.ambientalia.ui.screens.IntroScreen
import com.ambientalia.ui.screens.PlantsScreen
import com.ambientalia.ui.screens.ScanScreen
import com.ambientalia.ui.screens.SettingsScreen
import com.ambientalia.ui.theme.AmbientalIaTheme
import com.ambientalia.ui.theme.VerdeFondo
import com.ambientalia.ui.theme.VerdeHoja
import com.ambientalia.ui.theme.VerdeTextoSuave

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DESTINO = "destino"
    }

    private var destinoPendiente by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmaOverlay.cerrar()
        destinoPendiente = intent?.getStringExtra(EXTRA_DESTINO)
        setContent {
            AmbientalIaTheme {
                AmbientalRoot(destinoPendiente = destinoPendiente)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        AlarmaOverlay.cerrar()
        destinoPendiente = intent.getStringExtra(EXTRA_DESTINO)
    }
}

private data class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val DESTINOS = listOf(
    Destino("chat", "Chat", Icons.Filled.Chat),
    Destino("escanear", "Escanear", Icons.Filled.CameraAlt),
    Destino("plantas", "Plantas", Icons.Filled.LocalFlorist),
    Destino("ajustes", "Ajustes", Icons.Filled.Settings),
)

@Composable
fun AmbientalRoot(destinoPendiente: String?) {
    // Intro de bienvenida (regadera): se muestra una sola vez al arrancar.
    var introVisible by rememberSaveable { mutableStateOf(true) }
    if (introVisible) {
        IntroScreen(onTerminado = { introVisible = false })
        return
    }

    val nav = rememberNavController()
    var inicioElegido by remember { mutableStateOf(false) }

    // Arranca en "Mis plantas" si la app se abrió desde la notificación.
    LaunchedEffect(destinoPendiente) {
        if (!inicioElegido) {
            inicioElegido = true
            if (destinoPendiente == "plantas") nav.navigate("plantas")
        } else if (destinoPendiente == "plantas") {
            nav.navigate("plantas")
        }
    }

    // Permiso de notificaciones (Android 13+)
    val contexto = LocalContext.current
    val permisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = VerdeFondo,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VerdeFondo),
            ) {
                // Créditos siempre visibles encima de la barra de navegación.
                Text(
                    text = stringResource(R.string.creditos),
                    style = MaterialTheme.typography.labelLarge,
                    color = VerdeTextoSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                )
                NavigationBar(containerColor = VerdeFondo) {
                    val seleccion = DESTINOS.indexOfFirst { it.ruta == nav.currentDestination?.route }
                    DESTINOS.forEachIndexed { i, destino ->
                        val activa = i == seleccion
                        val escala by animateFloatAsState(
                            targetValue = if (activa) 1.2f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "nav$i",
                        )
                        NavigationBarItem(
                            selected = activa,
                            onClick = {
                                nav.navigate(destino.ruta) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destino.icono,
                                    contentDescription = destino.etiqueta,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = escala
                                        scaleY = escala
                                    },
                                )
                            },
                            label = {
                                Text(
                                    text = destino.etiqueta,
                                    fontWeight = if (activa) FontWeight.ExtraBold else FontWeight.SemiBold,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = VerdeHoja,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "chat",
            modifier = Modifier.padding(padding),
        ) {
            composable("chat") { ChatScreen() }
            composable("escanear") { ScanScreen() }
            composable("plantas") { PlantsScreen() }
            composable("ajustes") { SettingsScreen() }
        }
    }
}
