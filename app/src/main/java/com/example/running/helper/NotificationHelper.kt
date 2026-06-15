package com.example.running.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.running.R
import com.example.running.ui.chat.ChatRoomActivity

object NotificationHelper {

    private const val CHANNEL_ID = "chat_messages"
    private const val CHANNEL_NAME = "Mensagens do chat"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de novas mensagens"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showChatMessage(
        context: Context,
        chatId: String,
        otherUid: String,
        otherName: String,
        text: String
    ) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val intent = Intent(context, ChatRoomActivity::class.java).apply {
            putExtra(ChatRoomActivity.EXTRA_CHAT_ID, chatId)
            putExtra(ChatRoomActivity.EXTRA_OTHER_UID, otherUid)
            putExtra(ChatRoomActivity.EXTRA_OTHER_NAME, otherName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(otherName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(chatId.hashCode(), notif)
        }
    }
}
