package com.example.running.dao

import com.example.running.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserDao {

    private const val COLLECTION = "users"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun createUser(user: User) {
        db.collection(COLLECTION).document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): User? {
        val snapshot = db.collection(COLLECTION).document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun searchByEmail(email: String): User? {
        val snapshot = db.collection(COLLECTION)
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }
}
