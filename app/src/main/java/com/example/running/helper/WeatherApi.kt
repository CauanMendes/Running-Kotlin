package com.example.running.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherSnapshot(
    val temperatureC: Float?,
    val pressureHpa: Float?,
    val cloudCoverPct: Int?,
    val isDay: Boolean
)

object WeatherApi {

    suspend fun fetch(latitude: Double, longitude: Double): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?" +
                            "latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m,pressure_msl,cloud_cover,is_day"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                WeatherSnapshot(
                    temperatureC = current.optDouble("temperature_2m", Double.NaN)
                        .takeIf { !it.isNaN() }?.toFloat(),
                    pressureHpa = current.optDouble("pressure_msl", Double.NaN)
                        .takeIf { !it.isNaN() }?.toFloat(),
                    cloudCoverPct = current.optInt("cloud_cover", -1).takeIf { it >= 0 },
                    isDay = current.optInt("is_day", 1) == 1
                )
            }.getOrNull()
        }
}
