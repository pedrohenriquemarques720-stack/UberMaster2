package com.driver.earnings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.driver.earnings.service.UberNotificationService
import com.driver.earnings.service.OverlayService

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnOverlayPermission: Button
    private lateinit var btnNotificationPermission: Button
    private lateinit var btnStartService: Button
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvNotificationStatus: TextView
    
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
        updatePermissionStatus()
    }
    
    private fun initViews() {
        btnOverlayPermission = findViewById(R.id.btn_overlay_permission)
        btnNotificationPermission = findViewById(R.id.btn_notification_permission)
        btnStartService = findViewById(R.id.btn_start_service)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        tvNotificationStatus = findViewById(R.id.tv_notification_status)
        
        // Botão para configurar consumo do carro
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun setupListeners() {
        btnOverlayPermission.setOnClickListener {
            requestOverlayPermission()
        }
        
        btnNotificationPermission.setOnClickListener {
            requestNotificationPermission()
        }
        
        btnStartService.setOnClickListener {
            startOverlayService()
        }
    }
    
    private fun updatePermissionStatus() {
        // Status da permissão de overlay
        if (Settings.canDrawOverlays(this)) {
            tvOverlayStatus.text = "✓ Concedida"
            tvOverlayStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            btnOverlayPermission.isEnabled = false
        } else {
            tvOverlayStatus.text = "✗ Negada"
            tvOverlayStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            btnOverlayPermission.isEnabled = true
        }
        
        // Status da permissão de notificação
        val notificationPermission = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        
        if (notificationPermission) {
            tvNotificationStatus.text = "✓ Concedida"
            tvNotificationStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            btnNotificationPermission.isEnabled = false
        } else {
            tvNotificationStatus.text = "✗ Negada"
            tvNotificationStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            btnNotificationPermission.isEnabled = true
        }
        
        // Habilita botão de iniciar serviço apenas se ambas permissões estiverem concedidas
        btnStartService.isEnabled = Settings.canDrawOverlays(this) && 
                                    notificationPermission
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }
    
    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Ative o acesso às notificações para o Driver Earnings",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun startOverlayService() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Widget flutuante ativado!", Toast.LENGTH_SHORT).show()
            finish() // Fecha a MainActivity
        } else {
            Toast.makeText(this, "Permissão de overlay necessária", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val etConsumption = dialogView.findViewById<EditText>(R.id.et_consumption)
        val etFuelPrice = dialogView.findViewById<EditText>(R.id.et_fuel_price)
        
        // Carrega valores atuais
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etConsumption.setText(prefs.getFloat("car_consumption", 10f).toString())
        etFuelPrice.setText(prefs.getFloat("fuel_price", 5.50f).toString())
        
        AlertDialog.Builder(this)
            .setTitle("Configurações do Veículo")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val consumption = etConsumption.text.toString().toFloatOrNull() ?: 10f
                val fuelPrice = etFuelPrice.text.toString().toFloatOrNull() ?: 5.50f
                
                UberNotificationService.saveCarSettings(this, consumption, fuelPrice)
                Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
