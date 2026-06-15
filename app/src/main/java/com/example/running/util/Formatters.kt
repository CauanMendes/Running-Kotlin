package com.example.running.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val dateTime = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
    private val dateShort = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    fun formatDateTime(millis: Long): String = dateTime.format(Date(millis))
    fun formatDateShort(millis: Long): String = dateShort.format(Date(millis))

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) "%.2f km".format(meters / 1000.0)
        else "${meters.toInt()} m"
    }

    fun labelForType(type: String): String = when (type) {
        "run" -> "Corrida"
        "walk" -> "Caminhada"
        "bike" -> "Pedal"
        else -> type.replaceFirstChar { it.titlecase() }
    }
}
