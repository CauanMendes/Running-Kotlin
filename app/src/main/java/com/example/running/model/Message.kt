package com.example.running.model

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val sentAt: Long = System.currentTimeMillis()
)
