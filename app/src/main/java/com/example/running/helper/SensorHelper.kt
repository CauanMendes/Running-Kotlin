package com.example.running.helper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SensorHelper(context: Context) {

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val light: Sensor? = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val temp: Sensor? = manager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
    private val pressure: Sensor? = manager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val accelerometer: Sensor? = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val hasLight: Boolean get() = light != null
    val hasTemperature: Boolean get() = temp != null
    val hasPressure: Boolean get() = pressure != null

    private val listeners = mutableListOf<SensorEventListener>()

    fun startEnvironment(
        onLight: (Float) -> Unit,
        onTemperature: (Float) -> Unit,
        onPressure: (Float) -> Unit
    ) {
        register(light) { event -> onLight(event.values[0]) }
        register(temp) { event -> onTemperature(event.values[0]) }
        register(pressure) { event -> onPressure(event.values[0]) }
    }

    fun startMotion(onMagnitude: (Double) -> Unit, onRotation: (Double) -> Unit) {
        register(accelerometer) { event ->
            val (x, y, z) = event.values
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            onMagnitude(magnitude)
        }
        register(gyroscope) { event ->
            val (x, y, z) = event.values
            val rotation = sqrt((x * x + y * y + z * z).toDouble())
            onRotation(rotation)
        }
    }

    private fun register(sensor: Sensor?, onChanged: (SensorEvent) -> Unit) {
        if (sensor == null) return
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onChanged(event)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        listeners += listener
    }

    fun stop() {
        listeners.forEach { manager.unregisterListener(it) }
        listeners.clear()
    }

    companion object {
        fun classifyMotion(magnitude: Double): MotionState {
            val net = magnitude - 9.81
            return when {
                net < 0.8 -> MotionState.IDLE
                net < 3.0 -> MotionState.WALKING
                net < 6.0 -> MotionState.RUNNING
                else -> MotionState.CYCLING
            }
        }

        fun weatherVerdict(
            lux: Float?,
            temperatureC: Float?,
            pressureHpa: Float?
        ): WeatherVerdict {
            val luxOk = lux == null || lux > 500f
            val tempOk = temperatureC == null || (temperatureC in 15f..28f)
            val pressureOk = pressureHpa == null || (pressureHpa in 1005f..1025f)
            val score = listOf(luxOk, tempOk, pressureOk).count { it }
            return when (score) {
                3 -> WeatherVerdict.GOOD
                2 -> WeatherVerdict.OKAY
                else -> WeatherVerdict.BAD
            }
        }
    }
}

enum class MotionState { IDLE, WALKING, RUNNING, CYCLING }
enum class WeatherVerdict { GOOD, OKAY, BAD }
