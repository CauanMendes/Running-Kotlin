package com.example.running.model

data class TrackPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val t: Long = 0L
)

data class FitActivity(
    val id: String = "",
    val type: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val durationSec: Long = 0L,
    val kcal: Double = 0.0,
    val detectedType: String = "",
    val avgAccel: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val trajectory: List<TrackPoint> = emptyList(),
    val imageBase64: String = ""
)
