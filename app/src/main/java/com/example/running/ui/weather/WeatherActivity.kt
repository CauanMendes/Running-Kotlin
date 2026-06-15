package com.example.running.ui.weather

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.databinding.ActivityWeatherBinding
import com.example.running.helper.LocationHelper
import com.example.running.helper.SensorHelper
import com.example.running.helper.WeatherApi
import com.example.running.helper.WeatherVerdict
import kotlinx.coroutines.launch

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private lateinit var sensors: SensorHelper

    private var sensorLux: Float? = null
    private var sensorTempC: Float? = null
    private var sensorPressureHpa: Float? = null

    private var apiTempC: Float? = null
    private var apiPressureHpa: Float? = null
    private var apiCloudCover: Int? = null
    private var apiIsDay: Boolean = true

    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadFromApi()
        }

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

        renderLight()
        renderTemperature()
        renderPressure()
        binding.tvVerdict.setText(R.string.weather_loading)
    }

    override fun onResume() {
        super.onResume()
        sensors.startEnvironment(
            onLight = { sensorLux = it; renderLight(); refreshVerdict() },
            onTemperature = { sensorTempC = it; renderTemperature(); refreshVerdict() },
            onPressure = { sensorPressureHpa = it; renderPressure(); refreshVerdict() }
        )

        if (!sensors.hasTemperature || !sensors.hasPressure) {
            if (LocationHelper.hasPermission(this)) {
                loadFromApi()
            } else {
                locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            refreshVerdict()
        }
    }

    override fun onPause() {
        super.onPause()
        sensors.stop()
    }

    private fun loadFromApi() {
        lifecycleScope.launch {
            val location = LocationHelper.getCurrent(this@WeatherActivity) ?: return@launch
            val snapshot = WeatherApi.fetch(location.latitude, location.longitude) ?: return@launch
            apiTempC = snapshot.temperatureC
            apiPressureHpa = snapshot.pressureHpa
            apiCloudCover = snapshot.cloudCoverPct
            apiIsDay = snapshot.isDay
            renderTemperature()
            renderPressure()
            renderLight()
            refreshVerdict()
        }
    }

    private fun renderLight() {
        val card = binding.cardLight
        val sensorValue = sensorLux
        when {
            sensorValue != null -> {
                card.tvValue.text = "%.0f %s".format(sensorValue, getString(R.string.unit_lux))
                card.tvLabel.text =
                    "${getString(R.string.weather_light)} · ${getString(R.string.weather_source_sensor)}"
            }
            apiCloudCover != null -> {
                card.tvValue.text = "${apiCloudCover}% ${getString(R.string.weather_cloud_cover)}"
                card.tvLabel.text =
                    "${getString(R.string.weather_light)} · ${getString(R.string.weather_source_api)}"
            }
            else -> {
                card.tvValue.text = getString(R.string.weather_no_sensor)
                card.tvLabel.text = getString(R.string.weather_light)
            }
        }
    }

    private fun renderTemperature() {
        val card = binding.cardTemperature
        val value = sensorTempC ?: apiTempC
        val source = when {
            sensorTempC != null -> getString(R.string.weather_source_sensor)
            apiTempC != null -> getString(R.string.weather_source_api)
            else -> getString(R.string.weather_source_unknown)
        }
        card.tvValue.text = value?.let { "%.1f %s".format(it, getString(R.string.unit_celsius)) }
            ?: getString(R.string.weather_no_sensor)
        card.tvLabel.text = "${getString(R.string.weather_temperature)} · $source"
    }

    private fun renderPressure() {
        val card = binding.cardPressure
        val value = sensorPressureHpa ?: apiPressureHpa
        val source = when {
            sensorPressureHpa != null -> getString(R.string.weather_source_sensor)
            apiPressureHpa != null -> getString(R.string.weather_source_api)
            else -> getString(R.string.weather_source_unknown)
        }
        card.tvValue.text = value?.let { "%.1f %s".format(it, getString(R.string.unit_hpa)) }
            ?: getString(R.string.weather_no_sensor)
        card.tvLabel.text = "${getString(R.string.weather_pressure)} · $source"
    }

    private fun refreshVerdict() {
        val effectiveLux = sensorLux
            ?: apiCloudCover?.let { cloud -> if (apiIsDay) (100 - cloud) * 100f else 5f }
        val effectiveTemp = sensorTempC ?: apiTempC
        val effectivePressure = sensorPressureHpa ?: apiPressureHpa

        if (effectiveLux == null && effectiveTemp == null && effectivePressure == null) {
            return
        }

        val verdict = SensorHelper.weatherVerdict(effectiveLux, effectiveTemp, effectivePressure)
        binding.tvVerdict.setText(
            when (verdict) {
                WeatherVerdict.GOOD -> R.string.weather_verdict_good
                WeatherVerdict.OKAY -> R.string.weather_verdict_okay
                WeatherVerdict.BAD -> R.string.weather_verdict_bad
            }
        )
    }
}
