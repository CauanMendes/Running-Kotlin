package com.example.running.helper

import android.content.Context
import com.example.running.dao.ChatDao
import com.example.running.dao.MessageDao
import com.google.firebase.firestore.ListenerRegistration

/**
 * Listener global de novas mensagens. Quando recebe uma mensagem de outro usuário
 * em qualquer chat do usuário atual, dispara uma notificação local.
 *
 * Ignora mensagens enviadas pelo próprio usuário e mensagens do chat aberto no momento.
 */
object MessageNotifier {

    private var chatsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val firstSnapshotByChat = mutableSetOf<String>()

    /** Id do chat aberto no momento — não recebe notificação. */
    @Volatile
    var openChatId: String? = null

    fun start(context: Context, currentUid: String) {
        if (chatsListener != null) return

        chatsListener = ChatDao.listenForUser(currentUid) { chats ->
            val seen = mutableSetOf<String>()
            for (chat in chats) {
                seen.add(chat.id)
                if (!messageListeners.containsKey(chat.id)) {
                    val otherUid = chat.participants.firstOrNull { it != currentUid } ?: continue
                    val otherName = chat.participantNames[otherUid] ?: ""
                    attachMessageListener(context, currentUid, chat.id, otherUid, otherName)
                }
            }
            messageListeners.keys.minus(seen).forEach { id ->
                messageListeners.remove(id)?.remove()
                firstSnapshotByChat.remove(id)
            }
        }
    }

    private fun attachMessageListener(
        context: Context,
        currentUid: String,
        chatId: String,
        otherUid: String,
        otherName: String
    ) {
        val listener = MessageDao.listen(chatId) { messages ->
            if (firstSnapshotByChat.add(chatId)) {
                return@listen
            }
            val last = messages.lastOrNull() ?: return@listen
            if (last.senderUid == currentUid) return@listen
            if (chatId == openChatId) return@listen
            NotificationHelper.showChatMessage(
                context = context,
                chatId = chatId,
                otherUid = otherUid,
                otherName = otherName.ifBlank { "Amigo" },
                text = last.text
            )
        }
        messageListeners[chatId] = listener
    }

    fun stop() {
        chatsListener?.remove()
        chatsListener = null
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        firstSnapshotByChat.clear()
        openChatId = null
    }
}
