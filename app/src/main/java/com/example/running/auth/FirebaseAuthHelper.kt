package com.example.running.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object FirebaseAuthHelper {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
        return result.user ?: error("Usuário não encontrado")
    }

    suspend fun signUp(email: String, password: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
        val user = result.user ?: error("Falha ao criar usuário")
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
        return user
    }

    fun signOut() {
        auth.signOut()
    }
}
