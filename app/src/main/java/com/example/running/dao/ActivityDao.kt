package com.example.running.dao

import com.example.running.model.FitActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ActivityDao {

    private const val USERS = "users"
    private const val ACTIVITIES = "activities"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun save(uid: String, activity: FitActivity) {
        val ref = db.collection(USERS).document(uid).collection(ACTIVITIES).document()
        ref.set(activity.copy(id = ref.id)).await()
    }
}
