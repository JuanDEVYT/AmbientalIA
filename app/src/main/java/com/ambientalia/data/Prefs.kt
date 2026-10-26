package com.ambientalia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ambientalia.ai.AiClient

/**
 * Ajustes de la app.
 *
 * La API key se guarda cifrada con EncryptedSharedPreferences (Keystore del
 * dispositivo). Si el cifrado no está disponible en el equipo, se cae a
 * preferencias normales en lugar de romper la app.
 */
object Prefs {

    private const val ARCHIVO = "ambientalia_prefs"
    private const val ARCHIVO_SECRETO = "ambientalia_secret"
    private const val ARCHIVO_PLANO = "ambientalia_secret_fallback"

    // Valores por defecto: modelo con visión para el escáner.
    const val MODELO_DEFECTO = "ag/gemini-3-pro"
    const val VISION_DEFECTO = "ag/gemini-3-pro"

    private lateinit var sp: SharedPreferences
    private lateinit var seguro: SharedPreferences

    fun init(context: Context) {
        val app = context.applicationContext
        sp = app.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        seguro = cifrado(app)
        if (AiClient.apiKey.isBlank()) AiClient.apiKey = apiKey
    }

    private fun cifrado(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ARCHIVO_SECRETO,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        context.getSharedPreferences(ARCHIVO_PLANO, Context.MODE_PRIVATE)
    }

    private fun SharedPreferences.str(k: String, def: String = "") = getString(k, def) ?: def

    // ── API / IA ───────────────────────────────────────────────────────────

    var apiKey: String
        get() = seguro.str("api_key")
        set(value) {
            seguro.edit().putString("api_key", value.trim()).apply()
            AiClient.apiKey = value.trim()
        }

    var modelo: String
        get() = sp.str("modelo", MODELO_DEFECTO)
        set(value) = sp.edit().putString("modelo", value).apply()

    var modeloVision: String
        get() = sp.str("modelo_vision", VISION_DEFECTO)
        set(value) = sp.edit().putString("modelo_vision", value).apply()

    var modelosConocidos: String
        get() = sp.str("modelos", "")
        set(value) = sp.edit().putString("modelos", value).apply()

    // ── Recordatorio de riego ──────────────────────────────────────────────

    var recordatorioActivo: Boolean
        get() = sp.getBoolean("recordatorio", true)
        set(value) = sp.edit().putBoolean("recordatorio", value).apply()

    var recordatorioHora: Int
        get() = sp.getInt("hora", 9)
        set(value) = sp.edit().putInt("hora", value).apply()

    var recordatorioMinuto: Int
        get() = sp.getInt("minuto", 0)
        set(value) = sp.edit().putInt("minuto", value).apply()

    var plantasJson: String
        get() = sp.str("plantas")
        set(value) = sp.edit().putString("plantas", value).apply()

    /** Sesión del chat: para no mandar el historial entero (gasta menos). */
    var chatJson: String
        get() = sp.str("chat")
        set(value) = sp.edit().putString("chat", value).apply()
}
