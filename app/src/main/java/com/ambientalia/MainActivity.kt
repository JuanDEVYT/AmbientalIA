package com.ambientalia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ambientalia.ui.screens.ChatScreen
import com.ambientalia.ui.screens.PlantsScreen
import com.ambientalia.ui.screens.ScanScreen
import com.ambientalia.ui.screens.SettingsScreen
import com.ambientalia.ui.theme.AmbientalIaTheme
import com.ambientalia.ui.theme.VerdeFondo
import com.ambientalia.ui.theme.VerdeHoja

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DESTINO = "destino"
    }

    private var destinoPendiente by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destinoPendiente = intent?.getStringExtra(EXTRA_DESTINO)
        setContent {
            AmbientalIaTheme {
                AmbientalRoot(destinoPendiente = destinoPendiente)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
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
            NavigationBar(containerColor = VerdeFondo) {
                val seleccion = DESTINOS.indexOfFirst { it.ruta == nav.currentDestination?.route }
                DESTINOS.forEachIndexed { i, destino ->
                    NavigationBarItem(
                        selected = i == seleccion,
                        onClick = {
                            nav.navigate(destino.ruta) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                        label = { Text(destino.etiqueta) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = VerdeHoja,
                        ),
                    )
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
