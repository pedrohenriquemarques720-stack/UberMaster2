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

        // Posiciona no topo, centralizado horizontalmente
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100 // Distância do topo da tela

        windowManager.addView(floatingView, params)
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
        
        // RegEx para extrair valores (R$ e KM) do texto da Uber
        val price = extractValue(rawText, "R\\$\\s?(\\d+[,.]\\d+)")
        val distance = extractValue(rawText, "(\\d+[,.]\\d+)\\s?km")

        if (price != null && distance != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val consumption = prefs.getFloat("car_consumption", 10f)
            val fuelPrice = prefs.getFloat("fuel_price", 5.50f)

            // A FÓRMULA: Lucro = Preço - ((Distância / Consumo) * Preço Combustível)
            val cost = (distance / consumption) * fuelPrice
            val profit = price - cost

            // Feedback Visual no Lucro
            if (profit > 10) {
                tvProfit.setTextColor(Color.parseColor("#4CAF50")) // Verde Boa
            } else if (profit > 5) {
                tvProfit.setTextColor(Color.parseColor("#FFC107")) // Amarelo Alerta
            } else {
                tvProfit.setTextColor(Color.parseColor("#F44336")) // Vermelho Ruim
            }

            tvProfit.text = String.format("R$ %.2f", profit)
            tvCost.text = String.format("Custo: R$ %.2f", cost)
        } else {
            // Se falhar a extração, mostra erro visualmente
            tvProfit.text = "Erro Dados"
            tvProfit.setTextColor(Color.LTGRAY)
            tvCost.text = "Aguardando Uber..."
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
