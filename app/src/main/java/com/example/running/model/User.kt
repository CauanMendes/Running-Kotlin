package com.example.running.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
