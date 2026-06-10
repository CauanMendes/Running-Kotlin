package com.example.running.ui.weather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.running.R
import com.example.running.databinding.ActivityWeatherBinding
import com.example.running.helper.SensorHelper
import com.example.running.helper.WeatherVerdict

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private lateinit var sensors: SensorHelper

    private var lux: Float? = null
    private var temperatureC: Float? = null
    private var pressureHpa: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        sensors = SensorHelper(this)

        binding.cardLight.tvLabel.setText(R.string.weather_light)
        binding.cardTemperature.tvLabel.setText(R.string.weather_temperature)
        binding.cardPressure.tvLabel.setText(R.string.weather_pressure)

        setLight(null)
        setTemperature(null)
        setPressure(null)
    }

    override fun onResume() {
        super.onResume()
        sensors.startEnvironment(
            onLight = { setLight(it); refreshVerdict() },
            onTemperature = { setTemperature(it); refreshVerdict() },
            onPressure = { setPressure(it); refreshVerdict() }
        )
    }

    override fun onPause() {
        super.onPause()
        sensors.stop()
    }

    private fun setLight(value: Float?) {
        lux = value
        binding.cardLight.tvValue.text =
            value?.let { "%.0f %s".format(it, getString(R.string.unit_lux)) }
                ?: getString(R.string.weather_no_sensor)
    }

    private fun setTemperature(value: Float?) {
        temperatureC = value
        binding.cardTemperature.tvValue.text =
            value?.let { "%.1f %s".format(it, getString(R.string.unit_celsius)) }
                ?: getString(R.string.weather_no_sensor)
    }

    private fun setPressure(value: Float?) {
        pressureHpa = value
        binding.cardPressure.tvValue.text =
            value?.let { "%.1f %s".format(it, getString(R.string.unit_hpa)) }
                ?: getString(R.string.weather_no_sensor)
    }

    private fun refreshVerdict() {
        val verdict = SensorHelper.weatherVerdict(lux, temperatureC, pressureHpa)
        binding.tvVerdict.setText(
            when (verdict) {
                WeatherVerdict.GOOD -> R.string.weather_verdict_good
                WeatherVerdict.OKAY -> R.string.weather_verdict_okay
                WeatherVerdict.BAD -> R.string.weather_verdict_bad
            }
        )
    }
}
