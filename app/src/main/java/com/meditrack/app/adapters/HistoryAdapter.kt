package com.meditrack.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meditrack.app.databinding.ItemHistoryBinding
import com.meditrack.app.models.IntakeLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryAdapter - RecyclerView adapter להיסטוריית נטילות
 * אחראית: רע'ד
 */
class HistoryAdapter : ListAdapter<IntakeLog, HistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: IntakeLog) {
            binding.textScheduledTime.text = "מתוכנן: ${dateFormat.format(Date(log.scheduledTime))}"
            binding.textStatus.text = when (log.status) {
                IntakeLog.Status.TAKEN -> "✅ נלקח"
                IntakeLog.Status.PENDING -> "⏳ ממתין"
                IntakeLog.Status.SKIPPED -> "❌ דולג"
                IntakeLog.Status.SNOOZED -> "⏰ נדחה"
            }
            if (log.takenTime > 0) {
                binding.textTakenTime.text = "נלקח: ${dateFormat.format(Date(log.takenTime))}"
            } else {
                binding.textTakenTime.text = ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<IntakeLog>() {
        override fun areItemsTheSame(oldItem: IntakeLog, newItem: IntakeLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: IntakeLog, newItem: IntakeLog) = oldItem == newItem
    }
}
