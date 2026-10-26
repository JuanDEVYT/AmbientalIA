package com.ambientalia

import android.app.Application
import com.ambientalia.data.MisPlantas
import com.ambientalia.data.Prefs
import com.ambientalia.notify.WaterReminder

class AmbientalIaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        MisPlantas.cargar()
        WaterReminder.crearCanal(this)
        WaterReminder.programar(this)
    }
}
