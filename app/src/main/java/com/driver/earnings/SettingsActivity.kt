package com.driver.earnings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etConsumption = findViewById<EditText>(R.id.et_consumption)
        val etFuelPrice = findViewById<EditText>(R.id.et_fuel_price)
        val btnSave = findViewById<Button>(R.id.btn_save)

        // Carrega valores já salvos anteriormente
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etConsumption.setText(prefs.getFloat("car_consumption", 10.0f).toString())
        etFuelPrice.setText(prefs.getFloat("fuel_price", 4.00f).toString())

        btnSave.setOnClickListener {
            val cons = etConsumption.text.toString().toFloatOrNull() ?: 10.0f
            val price = etFuelPrice.text.toString().toFloatOrNull() ?: 4.00f

            prefs.edit().apply {
                putFloat("car_consumption", cons)
                putFloat("fuel_price", price)
                apply()
            }

            Toast.makeText(this, "Configurações Salvas!", Toast.LENGTH_SHORT).show()
            finish() // Fecha a tela e volta para a principal
        }
    }
}
