package com.driver.earnings.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class UberNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        // Verifica se a notificação é da Uber
        if (sbn?.packageName == "com.ubercab.driver") {
            val title = sbn.notification.extras.getString("android.title")
            val text = sbn.notification.extras.getString("android.text")
            
            // Envia os dados para o Widget Flutuante (OverlayService)
            val intent = Intent(this, OverlayService::class.java)
            intent.putExtra("uber_title", title)
            intent.putExtra("uber_text", text)
            startService(intent)
        }
    }

    companion object {
        // Função para salvar as configurações de consumo que você usa na MainActivity
        fun saveCarSettings(context: Context, consumption: Float, fuelPrice: Float) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().apply {
                putFloat("car_consumption", consumption)
                putFloat("fuel_price", fuelPrice)
                apply()
            }
        }
    }
}
