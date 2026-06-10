package com.example.running.model

data class FitActivity(
    val id: String = "",
    val type: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val durationSec: Long = 0L,
    val kcal: Double = 0.0,
    val detectedType: String = "",
    val avgAccel: Double = 0.0
)
