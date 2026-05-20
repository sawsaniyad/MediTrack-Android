package com.meditrack.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meditrack.app.databinding.ItemMedicationBinding
import com.meditrack.app.models.Medication

/**
 * MedicationAdapter - RecyclerView adapter לרשימת תרופות
 * אחראית: סוסאן
 */
class MedicationAdapter(
    private val onEdit: (Medication) -> Unit,
    private val onDelete: (Medication) -> Unit
) : ListAdapter<Medication, MedicationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(med: Medication) {
            binding.textMedName.text = med.name
            binding.textMedDosage.text = med.dosage
            binding.textMedNotes.text = med.notes.ifEmpty { "אין הערות" }

            binding.btnEdit.setOnClickListener { onEdit(med) }
            binding.btnDelete.setOnClickListener { onDelete(med) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Medication>() {
        override fun areItemsTheSame(oldItem: Medication, newItem: Medication) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Medication, newItem: Medication) = oldItem == newItem
    }
}
