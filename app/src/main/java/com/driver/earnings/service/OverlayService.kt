package com.driver.earnings.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.driver.earnings.R

class OverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "OverlayService"
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createOverlay()
        return START_STICKY
    }
    
    private fun createOverlay() {
        // Configurações do layout flutuante
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // Posição inicial
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200
        
        // Infla o layout do widget
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.widget_overlay, null)
        
        // Configura o botão de fechar
        val closeButton = overlayView.findViewById<Button>(R.id.btn_close)
        closeButton.setOnClickListener {
            stopSelf()
        }
        
        // Permite arrastar o widget
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
        
        windowManager.addView(overlayView, params)
        updateDisplay()
    }
    
    private fun updateDisplay() {
        // Recupera dados salvos
        val amount = sharedPreferences.getFloat("last_amount", 0f)
        val distance = sharedPreferences.getFloat("last_distance", 0f)
        val consumption = sharedPreferences.getFloat("car_consumption", 10f) // Valor padrão
        val fuelPrice = sharedPreferences.getFloat("fuel_price", 5.50f) // Valor padrão
        
        // Calcula lucro
        val fuelCost = (distance / consumption) * fuelPrice
        val profit = amount - fuelCost
        val profitPerKm = if (distance > 0) profit / distance else 0f
        
        // Atualiza TextViews
        overlayView.findViewById<TextView>(R.id.tv_amount).text = "Valor: R$ %.2f".format(amount)
        overlayView.findViewById<TextView>(R.id.tv_distance).text = "Distância: %.1f km".format(distance)
        overlayView.findViewById<TextView>(R.id.tv_profit).text = "Lucro: R$ %.2f".format(profit)
        overlayView.findViewById<TextView>(R.id.tv_profit_per_km).text = "R$ %.2f/km".format(profitPerKm)
        
        // Muda cor baseado no lucro por KM
        val cardView = overlayView.findViewById<androidx.cardview.widget.CardView>(R.id.card_container)
        val color = when {
            profitPerKm > 1.80f -> android.graphics.Color.parseColor("#4CAF50") // Verde
            profitPerKm > 1.10f -> android.graphics.Color.parseColor("#FFC107") // Amarelo
            else -> android.graphics.Color.parseColor("#F44336") // Vermelho
        }
        cardView.setCardBackgroundColor(color)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
