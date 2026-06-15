package com.example.running.dao

import com.example.running.model.FitActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object ActivityDao {

    private const val USERS = "users"
    private const val ACTIVITIES = "activities"
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun save(uid: String, activity: FitActivity): String {
        val ref = db.collection(USERS).document(uid).collection(ACTIVITIES).document()
        ref.set(activity.copy(id = ref.id)).await()
        return ref.id
    }

    fun listenForUser(uid: String, onUpdate: (List<FitActivity>) -> Unit): ListenerRegistration {
        return db.collection(USERS).document(uid).collection(ACTIVITIES)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FitActivity::class.java)?.copy(id = doc.id)
                }
                onUpdate(list)
            }
    }

    suspend fun get(uid: String, activityId: String): FitActivity? {
        val doc = db.collection(USERS).document(uid).collection(ACTIVITIES)
            .document(activityId).get().await()
        return doc.toObject(FitActivity::class.java)?.copy(id = doc.id)
    }
}
