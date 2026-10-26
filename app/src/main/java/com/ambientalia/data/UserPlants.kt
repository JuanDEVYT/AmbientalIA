package com.ambientalia.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

@Serializable
data class PlantaUsuario(
    val id: String,
    val catalogId: String? = null,
    val nombre: String,
    val cientifico: String = "",
    val emoji: String = "🌿",
    val foto: String? = null,
    val diasRiego: Int = 7,
    val luz: String = "",
    val estado: String = "sano",
    val resumen: String = "",
    val problemas: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList(),
    val senalRiego: String = "",
    val ultimoRiego: Long = 0L,
    val fechaAlta: Long = 0L,
    val nivel: Int = 1,
)

/**
 * Colección de plantas guardadas por el usuario (local, sin cuenta).
 * Se mantiene en memoria con estado observable para Compose y se persiste
 * en preferencias como JSON.
 */
object MisPlantas {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val plantas: SnapshotStateList<PlantaUsuario> = mutableStateListOf()

    fun cargar() {
        val crudo = Prefs.plantasJson
        if (crudo.isBlank()) return
        val lista = runCatching {
            json.decodeFromString(ListSerializer(PlantaUsuario.serializer()), crudo)
        }.getOrDefault(emptyList())
        plantas.clear()
        plantas.addAll(lista)
    }

    private fun persistir() {
        Prefs.plantasJson = json.encodeToString(
            ListSerializer(PlantaUsuario.serializer()),
            plantas.toList(),
        )
    }

    fun agregar(p: PlantaUsuario) {
        plantas.removeAll { it.id == p.id }
        plantas.add(0, p)
        persistir()
    }

    fun eliminar(id: String) {
        plantas.removeAll { it.id == id }
        persistir()
    }

    fun porId(id: String): PlantaUsuario? = plantas.firstOrNull { it.id == id }

    fun regar(id: String) {
        val i = plantas.indexOfFirst { it.id == id }
        if (i < 0) return
        val actual = plantas[i]
        val nuevoNivel = if (diasSinRegar(actual) >= actual.diasRiego) {
            (actual.nivel + 1).coerceAtMost(20)
        } else {
            actual.nivel
        }
        plantas[i] = actual.copy(ultimoRiego = System.currentTimeMillis(), nivel = nuevoNivel)
        persistir()
    }

    /** Días completos sin regar. */
    fun diasSinRegar(p: PlantaUsuario): Int {
        if (p.ultimoRiego <= 0L) return 0
        val ms = System.currentTimeMillis() - p.ultimoRiego
        return (ms / 86_400_000L).toInt()
    }

    fun necesitaRiego(p: PlantaUsuario): Boolean = diasSinRegar(p) >= p.diasRiego

    /** Texto corto para la notificación diaria. */
    fun resumenNotificacion(): String {
        val sedientas = plantas.filter { necesitaRiego(it) }
        return when {
            plantas.isEmpty() -> "Toca para ver el cuidado de tus plantas."
            sedientas.isEmpty() -> "Todas tus plantas están bien. ${plantas.size} en total."
            sedientas.size == 1 -> "${sedientas[0].emoji} ${sedientas[0].nombre} necesita agua hoy."
            else -> sedientas.take(3).joinToString(" ") { it.emoji + " " + it.nombre } +
                " necesitan agua hoy."
        }
    }
}
