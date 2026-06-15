package com.example.running.dao

import com.example.running.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserDao {

    private const val COLLECTION = "users"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun createUser(user: User) {
        val normalized = user.copy(email = user.email.trim().lowercase())
        db.collection(COLLECTION).document(user.uid).set(normalized).await()
    }

    suspend fun getUser(uid: String): User? {
        val snapshot = db.collection(COLLECTION).document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun searchByEmail(email: String): User? {
        val query = email.trim().lowercase()
        val snapshot = db.collection(COLLECTION)
            .whereEqualTo("email", query)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    /**
     * Garante que o documento do usuário existe no Firestore. Chamado após login
     * e cadastro para cobrir casos onde a escrita anterior falhou.
     */
    suspend fun ensureUser(firebaseUser: FirebaseUser, displayName: String? = null) {
        val name = displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.displayName
            ?: firebaseUser.email?.substringBefore('@')
            ?: ""
        val email = firebaseUser.email?.trim()?.lowercase() ?: ""
        val existing = runCatching { getUser(firebaseUser.uid) }.getOrNull()
        if (existing == null) {
            createUser(
                User(uid = firebaseUser.uid, displayName = name, email = email)
            )
        } else if (existing.email != email || (name.isNotBlank() && existing.displayName != name)) {
            createUser(existing.copy(displayName = name, email = email))
        }
    }
}
