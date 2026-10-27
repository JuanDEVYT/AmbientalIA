package com.ambientalia.notify

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ambientalia.MainActivity
import com.ambientalia.R
import com.ambientalia.data.MisPlantas

/**
 * Alarma de riego "superpuesta": una ventana TYPE_APPLICATION_OVERLAY que se
 * dibuja ENCIMA de otras aplicaciones, como una alarma, y despierta la pantalla.
 *
 * Requiere el permiso especial de superposiciones (Settings.canDrawOverlays);
 * sin él no se muestra y manda solo la notificación normal.
 * Solo hay una instancia: volver a llamar sustituye la anterior, y se cierra
 * sola a los 60 s por si nadie la toca.
 */
object AlarmaOverlay {

    private const val AUTO_CIERRE_MS = 60_000L

    private val hilo = Handler(Looper.getMainLooper())
    private var vista: View? = null
    private var ventana: WindowManager? = null
    private val autoCierre = Runnable { cerrar() }

    /** ¿Tiene la app permiso para dibujar sobre otras apps? */
    fun disponible(contexto: Context): Boolean = Settings.canDrawOverlays(contexto)

    /** Muestra la alarma encima de lo que haya en pantalla. No hace nada sin permiso. */
    fun mostrar(contexto: Context) {
        if (!disponible(contexto)) return
        val app = contexto.applicationContext

        // Pantalla encendida, como una alarma de verdad (requiere WAKE_LOCK).
        // Se usa un wake lock público con ACQUIRE_CAUSES_WAKEUP porque
        // PowerManager.wakeUp() no forma parte del SDK público.
        val energia = app.getSystemService(PowerManager::class.java)
        if (energia != null && !energia.isInteractive) {
            runCatching {
                @Suppress("DEPRECATION")
                val bloqueo = energia.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ambientalia:alarma",
                )
                bloqueo.setReferenceCounted(false)
                bloqueo.acquire(10_000L)
            }
        }

        hilo.post {
            val gestor = app.getSystemService(WindowManager::class.java) ?: return@post
            cerrar()

            val densidad = app.resources.displayMetrics.density
            fun dp(v: Float) = (v * densidad).toInt()

            val tarjeta = LinearLayout(app).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = GradientDrawable().apply {
                    cornerRadius = 28f * densidad
                    setColor(android.graphics.Color.WHITE)
                    setStroke((3 * densidad).toInt(), 0xFF43A302.toInt())
                }
                val p = dp(24f)
                setPadding(p, p, p, p)
            }

            tarjeta.addView(
                ImageView(app).apply {
                    setImageResource(R.drawable.planta_default)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(96f), dp(96f)).apply {
                        gravity = Gravity.CENTER
                        bottomMargin = dp(12f)
                    }
                },
            )

            tarjeta.addView(
                TextView(app).apply {
                    text = "¡Hora de regar!"
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFF175B1A.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                },
            )

            tarjeta.addView(
                TextView(app).apply {
                    text = MisPlantas.resumenNotificacion()
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(0xFF4B7A4E.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(10f)
                        bottomMargin = dp(18f)
                    }
                },
            )

            fun boton(texto: String, fondo: Int, textoColor: Int, borde: Int) = Button(app).apply {
                text = texto
                setAllCaps(false)
                setTypeface(null, Typeface.BOLD)
                setTextColor(textoColor)
                background = GradientDrawable().apply {
                    cornerRadius = 16f * densidad
                    setColor(fondo)
                    if (borde != 0) setStroke((2 * densidad).toInt(), borde)
                }
                minWidth = 0
                setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            }

            val botonCerrar = boton(
                "Cerrar",
                android.graphics.Color.WHITE,
                0xFF43A302.toInt(),
                0xFF43A302.toInt(),
            ).apply { setOnClickListener { cerrar() } }

            val botonAbrir = boton(
                "Abrir y regar",
                0xFF58CC02.toInt(),
                android.graphics.Color.WHITE,
                0,
            ).apply {
                setOnClickListener {
                    cerrar()
                    abrirApp(app)
                }
            }

            tarjeta.addView(
                LinearLayout(app).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    addView(
                        botonCerrar,
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        ).apply { marginEnd = dp(10f) },
                    )
                    addView(
                        botonAbrir,
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        ),
                    )
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(6f) },
            )

            val marco = FrameLayout(app).apply {
                setBackgroundColor(android.graphics.Color.argb(190, 6, 28, 10))
                addView(
                    tarjeta,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER,
                    ).apply {
                        leftMargin = dp(24f)
                        rightMargin = dp(24f)
                    },
                )
            }

            val parametros = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                android.graphics.PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.CENTER }

            runCatching {
                gestor.addView(marco, parametros)
                vista = marco
                ventana = gestor
                hilo.removeCallbacks(autoCierre)
                hilo.postDelayed(autoCierre, AUTO_CIERRE_MS)
            }
        }
    }

    /** Quita la alarma de pantalla si estaba visible. Seguro de llamar siempre. */
    fun cerrar() {
        hilo.removeCallbacks(autoCierre)
        val marco = vista ?: return
        vista = null
        val gestor = ventana
        ventana = null
        runCatching { gestor?.removeViewImmediate(marco) }
    }

    private fun abrirApp(contexto: Context) {
        val intent = Intent(contexto, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINO, "plantas")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { contexto.startActivity(intent) }
    }
}
