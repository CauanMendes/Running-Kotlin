package com.example.running.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.MessageDao
import com.example.running.databinding.ActivityChatRoomBinding
import com.example.running.helper.MessageNotifier
import com.example.running.model.Message
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run { finish(); return }
        val otherName = intent.getStringExtra(EXTRA_OTHER_NAME) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = otherName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val currentUid = FirebaseAuthHelper.currentUser?.uid ?: run { finish(); return }
        adapter = MessageAdapter(currentUid)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    override fun onStart() {
        super.onStart()
        MessageNotifier.openChatId = chatId
        listener = MessageDao.listen(chatId) { messages ->
            adapter.submit(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (MessageNotifier.openChatId == chatId) {
            MessageNotifier.openChatId = null
        }
        listener?.remove()
        listener = null
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        val uid = FirebaseAuthHelper.currentUser?.uid ?: return

        binding.etMessage.setText("")
        lifecycleScope.launch {
            runCatching {
                MessageDao.send(chatId, Message(senderUid = uid, text = text))
            }
        }
    }

    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_OTHER_UID = "other_uid"
        const val EXTRA_OTHER_NAME = "other_name"
    }
}
