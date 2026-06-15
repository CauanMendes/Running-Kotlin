package com.example.running.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.running.R
import com.example.running.databinding.ItemActivityBinding
import com.example.running.model.FitActivity
import com.example.running.util.Base64Converter
import com.example.running.util.Formatters

class HistoryAdapter(
    private val onClick: (FitActivity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val items = mutableListOf<FitActivity>()

    fun submit(list: List<FitActivity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FitActivity) {
            binding.tvType.text = Formatters.labelForType(item.type)
            binding.tvDate.text = Formatters.formatDateTime(item.startedAt)
            binding.tvStats.text = buildString {
                append(Formatters.formatDuration(item.durationSec))
                if (item.distanceMeters > 0) {
                    append(" • ")
                    append(Formatters.formatDistance(item.distanceMeters))
                }
                append(" • ")
                append("${item.kcal.toInt()} kcal")
            }

            binding.ivTypeIcon.setImageResource(iconFor(item.type))

            if (item.imageBase64.isNotBlank()) {
                runCatching { Base64Converter.stringToBitmap(item.imageBase64) }
                    .onSuccess { bitmap ->
                        binding.ivThumb.setImageBitmap(bitmap)
                        binding.ivThumb.visibility = View.VISIBLE
                    }
                    .onFailure { binding.ivThumb.visibility = View.GONE }
            } else {
                binding.ivThumb.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun iconFor(type: String): Int = when (type) {
            "run" -> R.drawable.ic_play_circle
            "walk" -> R.drawable.ic_leaf
            "bike" -> R.drawable.ic_share
            else -> R.drawable.ic_play_circle
        }
    }
}
