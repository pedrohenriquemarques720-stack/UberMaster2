package com.driver.earnings.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.driver.earnings.R
import java.util.regex.Pattern

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100 
        windowManager.addView(floatingView, params)

        // Lógica do botão Fechar
        floatingView.findViewById<TextView>(R.id.btn_close_widget).setOnClickListener {
            stopSelf() // Encerra o widget
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("uber_text") ?: ""
        if (text.isNotEmpty()) {
            calculateAndDisplay(text)
        }
        return START_STICKY
    }

    private fun calculateAndDisplay(rawText: String) {
        val tvProfit = floatingView.findViewById<TextView>(R.id.tv_widget_profit)
        val tvCost = floatingView.findViewById<TextView>(R.id.tv_widget_cost)
        
        val price = extractValue(rawText, "R\\$\\s?(\\d+[,.]\\d+)")
        val distance = extractValue(rawText, "(\\d+[,.]\\d+)\\s?km")

        if (price != null && distance != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val consumption = prefs.getFloat("car_consumption", 10f)
            val fuelPrice = prefs.getFloat("fuel_price", 5.50f)

            val cost = (distance / consumption) * fuelPrice
            val profit = price - cost

            if (profit > 10) {
                tvProfit.setTextColor(Color.parseColor("#4CAF50"))
            } else if (profit > 5) {
                tvProfit.setTextColor(Color.parseColor("#FFC107"))
            } else {
                tvProfit.setTextColor(Color.parseColor("#F44336"))
            }

            tvProfit.text = String.format("R$ %.2f", profit)
            tvCost.text = String.format("Custo: R$ %.2f", cost)

            // Guarda o valor no histórico diário
            saveToDailyHistory(profit)

        } else {
            tvProfit.text = "Erro Dados"
            tvProfit.setTextColor(Color.LTGRAY)
            tvCost.text = "Aguardando Uber..."
        }
    }

    private fun saveToDailyHistory(profit: Float) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentTotal = prefs.getFloat("daily_total_profit", 0f)
        val currentTrips = prefs.getInt("daily_total_trips", 0)
        
        prefs.edit().apply {
            putFloat("daily_total_profit", currentTotal + profit)
            putInt("daily_total_trips", currentTrips + 1)
            apply()
        }
    }

    private fun extractValue(text: String, patternStr: String): Float? {
        val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.replace(",", ".")?.toFloatOrNull()
        } else null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
