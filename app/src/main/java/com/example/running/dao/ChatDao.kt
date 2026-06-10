package com.example.running.dao

import com.example.running.model.Chat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object ChatDao {

    private const val COLLECTION = "chats"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun chatIdFor(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun getOrCreateChat(
        currentUid: String,
        currentName: String,
        otherUid: String,
        otherName: String
    ): Chat {
        val id = chatIdFor(currentUid, otherUid)
        val ref = db.collection(COLLECTION).document(id)
        val snapshot = ref.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(Chat::class.java)!!.copy(id = id)
        }
        val chat = Chat(
            id = id,
            participants = listOf(currentUid, otherUid),
            participantNames = mapOf(currentUid to currentName, otherUid to otherName),
            lastMessage = "",
            lastMessageAt = System.currentTimeMillis()
        )
        ref.set(chat).await()
        return chat
    }

    fun listenForUser(uid: String, onUpdate: (List<Chat>) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION)
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val chats = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                }
                onUpdate(chats)
            }
    }

    suspend fun updateLastMessage(chatId: String, lastMessage: String) {
        db.collection(COLLECTION).document(chatId)
            .update(
                mapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
}
