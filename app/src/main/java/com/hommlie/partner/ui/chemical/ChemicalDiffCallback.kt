package com.hommlie.partner.ui.chemical

import androidx.recyclerview.widget.DiffUtil
import com.hommlie.partner.model.Chemical

class ChemicalDiffCallback : DiffUtil.ItemCallback<Chemical>() {
    override fun areItemsTheSame(oldItem: Chemical, newItem: Chemical): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Chemical, newItem: Chemical): Boolean {
        return oldItem == newItem
    }
}
