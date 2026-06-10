package com.example.running.dao

import com.example.running.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object MessageDao {

    private const val CHATS = "chats"
    private const val MESSAGES = "messages"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun send(chatId: String, message: Message) {
        val ref = db.collection(CHATS).document(chatId).collection(MESSAGES).document()
        val toSave = message.copy(id = ref.id)
        ref.set(toSave).await()
        ChatDao.updateLastMessage(chatId, message.text)
    }

    fun listen(chatId: String, onUpdate: (List<Message>) -> Unit): ListenerRegistration {
        return db.collection(CHATS).document(chatId).collection(MESSAGES)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                onUpdate(messages)
            }
    }
}
