package com.driver.earnings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.driver.earnings.service.UberNotificationService
import com.driver.earnings.service.OverlayService

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnOverlayPermission: Button
    private lateinit var btnNotificationPermission: Button
    private lateinit var btnStartService: Button
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvNotificationStatus: TextView
    
    // Novas variáveis do histórico
    private lateinit var tvDailyProfit: TextView
    private lateinit var tvDailyTrips: TextView
    private lateinit var btnResetHistory: Button
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateDashboard() // Atualiza os valores sempre que o ecrã for aberto
    }
    
    private fun initViews() {
        btnOverlayPermission = findViewById(R.id.btn_overlay_permission)
        btnNotificationPermission = findViewById(R.id.btn_notification_permission)
        btnStartService = findViewById(R.id.btn_start_service)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        tvNotificationStatus = findViewById(R.id.tv_notification_status)
        
        tvDailyProfit = findViewById(R.id.tv_daily_profit)
        tvDailyTrips = findViewById(R.id.tv_daily_trips)
        btnResetHistory = findViewById(R.id.btn_reset_history)
        
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun setupListeners() {
        btnOverlayPermission.setOnClickListener { requestOverlayPermission() }
        btnNotificationPermission.setOnClickListener { requestNotificationPermission() }
        btnStartService.setOnClickListener { startAppServices() }
        
        btnResetHistory.setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().apply {
                putFloat("daily_total_profit", 0f)
                putInt("daily_total_trips", 0)
                apply()
            }
            updateDashboard()
            Toast.makeText(this, "Histórico limpo com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateDashboard() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val profit = prefs.getFloat("daily_total_profit", 0f)
        val trips = prefs.getInt("daily_total_trips", 0)
        
        tvDailyProfit.text = String.format("R$ %.2f", profit)
        tvDailyTrips.text = "$trips viagens registadas"
    }
    
    private fun updatePermissionStatus() {
        val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        if (canDrawOverlays) {
            tvOverlayStatus.text = "✓ Concedida"
            tvOverlayStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnOverlayPermission.isEnabled = false
        } else {
            tvOverlayStatus.text = "✗ Negada"
            tvOverlayStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnOverlayPermission.isEnabled = true
        }
        
        val notificationPermission = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        
        if (notificationPermission) {
            tvNotificationStatus.text = "✓ Concedida"
            tvNotificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnNotificationPermission.isEnabled = false
        } else {
            tvNotificationStatus.text = "✗ Negada"
            tvNotificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnNotificationPermission.isEnabled = true
        }
        
        btnStartService.isEnabled = canDrawOverlays && notificationPermission
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                overlayPermissionLauncher.launch(intent)
            }
        }
    }
    
    private fun requestNotificationPermission() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            Toast.makeText(this, "Ative o acesso às notificações para o Driver Earnings", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir as configurações", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startAppServices() {
        val overlayIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
        startService(Intent(this, UberNotificationService::class.java))
        Toast.makeText(this, "Serviços Driver Earnings ativos!", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val etConsumption = dialogView.findViewById<EditText>(R.id.et_consumption)
        val etFuelPrice = dialogView.findViewById<EditText>(R.id.et_fuel_price)
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etConsumption.setText(prefs.getFloat("car_consumption", 10f).toString())
        etFuelPrice.setText(prefs.getFloat("fuel_price", 5.50f).toString())
        
        AlertDialog.Builder(this)
            .setTitle("Configurações do Veículo")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val consumption = etConsumption.text.toString().toFloatOrNull() ?: 10f
                val fuelPrice = etFuelPrice.text.toString().toFloatOrNull() ?: 5.50f
                prefs.edit().apply {
                    putFloat("car_consumption", consumption)
                    putFloat("fuel_price", fuelPrice)
                    apply()
                }
                Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
