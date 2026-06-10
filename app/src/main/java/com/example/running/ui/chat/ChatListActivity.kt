package com.example.running.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.ChatDao
import com.example.running.dao.UserDao
import com.example.running.databinding.ActivityChatListBinding
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ChatListAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val currentUid = FirebaseAuthHelper.currentUser?.uid ?: run { finish(); return }

        adapter = ChatListAdapter(currentUid) { chat ->
            val otherUid = chat.participants.firstOrNull { it != currentUid } ?: return@ChatListAdapter
            val otherName = chat.participantNames[otherUid] ?: ""
            openChatRoom(chat.id, otherUid, otherName)
        }
        binding.recyclerChats.layoutManager = LinearLayoutManager(this)
        binding.recyclerChats.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddFriendDialog() }
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuthHelper.currentUser?.uid ?: return
        listener = ChatDao.listenForUser(uid) { chats ->
            adapter.submit(chats)
            binding.tvEmpty.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun showAddFriendDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.add_friend_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_friend_title)
            .setView(input)
            .setPositiveButton(R.string.add_friend_btn) { _, _ ->
                addFriend(input.text.toString().trim())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addFriend(email: String) {
        if (email.isBlank()) return
        val current = FirebaseAuthHelper.currentUser ?: return
        if (email.equals(current.email, ignoreCase = true)) {
            toast(R.string.add_friend_self); return
        }

        lifecycleScope.launch {
            runCatching {
                val other = UserDao.searchByEmail(email)
                    ?: error(getString(R.string.add_friend_not_found))
                val me = UserDao.getUser(current.uid)
                val myName = me?.displayName?.ifBlank { current.email ?: "" } ?: ""
                ChatDao.getOrCreateChat(
                    currentUid = current.uid,
                    currentName = myName,
                    otherUid = other.uid,
                    otherName = other.displayName.ifBlank { other.email }
                )
            }.onSuccess { chat ->
                val otherUid = chat.participants.first { it != current.uid }
                val otherName = chat.participantNames[otherUid] ?: ""
                openChatRoom(chat.id, otherUid, otherName)
            }.onFailure { e ->
                toast(e.message ?: "Erro")
            }
        }
    }

    private fun openChatRoom(chatId: String, otherUid: String, otherName: String) {
        startActivity(
            Intent(this, ChatRoomActivity::class.java)
                .putExtra(ChatRoomActivity.EXTRA_CHAT_ID, chatId)
                .putExtra(ChatRoomActivity.EXTRA_OTHER_UID, otherUid)
                .putExtra(ChatRoomActivity.EXTRA_OTHER_NAME, otherName)
        )
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
