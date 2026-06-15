package com.example.running.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.ActivityDao
import com.example.running.databinding.ActivityHistoryBinding
import com.google.firebase.firestore.ListenerRegistration

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = HistoryAdapter { item ->
            startActivity(
                Intent(this, HistoryDetailActivity::class.java)
                    .putExtra(HistoryDetailActivity.EXTRA_ID, item.id)
            )
        }
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuthHelper.currentUser?.uid ?: run { finish(); return }
        listener = ActivityDao.listenForUser(uid) { activities ->
            adapter.submit(activities)
            binding.tvEmpty.visibility = if (activities.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }
}
