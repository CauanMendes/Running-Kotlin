package com.example.running.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.running.databinding.ItemMessageReceivedBinding
import com.example.running.databinding.ItemMessageSentBinding
import com.example.running.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUid: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submit(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderUid == currentUid) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            SentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        val time = timeFormat.format(Date(msg.sentAt))
        when (holder) {
            is SentVH -> {
                holder.binding.tvText.text = msg.text
                holder.binding.tvTime.text = time
            }
            is ReceivedVH -> {
                holder.binding.tvText.text = msg.text
                holder.binding.tvTime.text = time
            }
        }
    }

    override fun getItemCount() = items.size

    class SentVH(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedVH(val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }
}
