package com.hommlie.partner.ui.jobs

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowChemicalUsedBinding
import com.hommlie.partner.model.ChemicalItemUI

class ChemicalUsedAdapter(
    private val onItemChanged: (ChemicalItemUI) -> Unit
) : ListAdapter<ChemicalItemUI, ChemicalUsedAdapter.ChemicalVH>(ChemicalDiff) {

    inner class ChemicalVH(
        private val binding: RowChemicalUsedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChemicalItemUI) {

            binding.tvSno.text = (bindingAdapterPosition + 1).toString()
            binding.tvChemicalName.text = item.chemicalName
            binding.tvAvailableQty.text = item.availableQty
            binding.tvUnit.text = item.unit

            // Remove old watcher
            (binding.etUsedQuantity.tag as? TextWatcher)?.let {
                binding.etUsedQuantity.removeTextChangedListener(it)
            }

            // Set previous value
            binding.etUsedQuantity.setText(item.usedQty)

            // Cursor at end
            binding.etUsedQuantity.setSelection(binding.etUsedQuantity.text.length)

            val watcher = object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {

                    val value = s?.toString().orEmpty()

                    // avoid unnecessary callback
                    if (item.usedQty == value) return

                    item.usedQty = value

                    onItemChanged(item)
                }
            }

            binding.etUsedQuantity.addTextChangedListener(watcher)

            binding.etUsedQuantity.tag = watcher
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChemicalVH {

        val binding = RowChemicalUsedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChemicalVH(binding)
    }

    override fun onBindViewHolder(
        holder: ChemicalVH,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    object ChemicalDiff : DiffUtil.ItemCallback<ChemicalItemUI>() {

        override fun areItemsTheSame(
            oldItem: ChemicalItemUI,
            newItem: ChemicalItemUI
        ): Boolean {
            return oldItem.chemicalId == newItem.chemicalId
        }

        override fun areContentsTheSame(
            oldItem: ChemicalItemUI,
            newItem: ChemicalItemUI
        ): Boolean {
            return oldItem == newItem
        }
    }
}