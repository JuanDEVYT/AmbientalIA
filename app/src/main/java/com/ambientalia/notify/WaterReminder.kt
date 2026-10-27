package com.ambientalia.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ambientalia.MainActivity
import com.ambientalia.R
import com.ambientalia.data.MisPlantas
import com.ambientalia.data.Prefs
import com.ambientalia.ui.theme.VerdePrincipal
import java.util.Calendar

/**
 * Recordatorio diario de riego, 100 % local: sin servidor, sin cuentas.
 *
 * - `AlarmManager` dispara a la hora exacta elegida en Ajustes.
 * - Si el sistema no permite alarmas exactas (Android 12+), se usa una alarma
 *   inexacta: suena aproximadamente a la hora, sin pedir permiso extra.
 * - Tras cada disparo se reprograma sola para el día siguiente.
 * - `BootReceiver` la vuelve a poner en marcha tras reiniciar el móvil.
 */
object WaterReminder {

    private const val CANAL = "riego"
    private const val CODIGO = 1001
    private const val CODIGO_NOTIF = 2002

    fun crearCanal(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            CANAL,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(canal)
    }

    /** Reprograma la alarma del día elegido. */
    fun programar(context: Context) {
        val app = context.applicationContext
        val alarmas = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendiente(app)

        if (!Prefs.recordatorioActivo) {
            alarmas.cancel(pi)
            pi.cancel()
            return
        }

        val momento = proximaOcurrencia(Prefs.recordatorioHora, Prefs.recordatorioMinuto)
        val exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()
        if (exacta) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, pi)
        } else {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, pi)
        }
    }

    fun cancelar(context: Context) {
        val app = context.applicationContext
        val alarmas = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendiente(app)
        alarmas.cancel(pi)
        pi.cancel()
    }

    private fun pendiente(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            CODIGO,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun proximaOcurrencia(hora: Int, minuto: Int): Long {
        val ahora = Calendar.getInstance()
        val proxima = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (proxima.timeInMillis <= ahora.timeInMillis) proxima.add(Calendar.DAY_OF_MONTH, 1)
        return proxima.timeInMillis
    }

    fun alarmaExactaDisponible(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmas = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmas.canScheduleExactAlarms()
    }

    /** Abre la pantalla del sistema "Alarmas y recordatorios" de la app. */
    fun pedirAlarmaExacta(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}"),
            )
            runCatching { context.startActivity(intent) }
        }
    }

    /**
     * Llega como alarma: notificación de alta prioridad + ventana superpuesta
     * encima de otras apps (si se concedió el permiso de superposición).
     */
    fun alarma(context: Context) {
        notificar(context)
        AlarmaOverlay.mostrar(context)
    }

    /** Muestra la notificación de riego (la dispara la alarma y el botón de prueba). */
    fun notificar(context: Context) {
        val app = context.applicationContext
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return

        val texto = MisPlantas.resumenNotificacion()
        val abrir = Intent(app, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINO, "plantas")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            app,
            CODIGO_NOTIF,
            abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion = NotificationCompat.Builder(app, CANAL)
            .setSmallIcon(R.drawable.ic_notif_leaf)
            .setColor(VerdePrincipal.toArgb())
            .setContentTitle(app.getString(R.string.notif_title))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        runCatching { NotificationManagerCompat.from(app).notify(CODIGO_NOTIF, notificacion) }
    }
}

/** Dispara la notificación y reprograma la del día siguiente. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WaterReminder.alarma(context)
        WaterReminder.programar(context)
    }
}

/** Vuelve a poner la alarma en marcha tras un reinicio del móvil. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action ?: return
        if (accion == Intent.ACTION_BOOT_COMPLETED || accion == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WaterReminder.programar(context)
        }
    }
}
