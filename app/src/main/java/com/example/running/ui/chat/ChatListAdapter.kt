package com.example.running.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.running.databinding.ItemChatBinding
import com.example.running.model.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val currentUid: String,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatVH>() {

    private val items = mutableListOf<Chat>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submit(list: List<Chat>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatVH(binding)
    }

    override fun onBindViewHolder(holder: ChatVH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ChatVH(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            val otherUid = chat.participants.firstOrNull { it != currentUid }
            val otherName = otherUid?.let { chat.participantNames[it] } ?: "Amigo"
            binding.tvName.text = otherName
            binding.tvLastMessage.text = chat.lastMessage.ifBlank { "Sem mensagens" }
            binding.tvTime.text = if (chat.lastMessageAt > 0) {
                timeFormat.format(Date(chat.lastMessageAt))
            } else ""
            binding.root.setOnClickListener { onClick(chat) }
        }
    }
}
