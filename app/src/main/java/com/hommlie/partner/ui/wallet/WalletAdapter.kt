package com.hommlie.partner.ui.wallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowWalletBinding
import com.hommlie.partner.model.WalletItem

class WalletAdapter(
    private var items: List<WalletItem>
) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    inner class WalletViewHolder(val binding: RowWalletBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = RowWalletBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvAmount.text = item.amount
        holder.binding.tvDate.text = item.date

        // Credit -> Green | Debit -> Red
        val color = if (item.isCredit) {
            Color.parseColor("#2E7D32") // Dark Green
        } else {
            Color.parseColor("#C62828") // Dark Red
        }
        holder.binding.tvAmount.setTextColor(color)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<WalletItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

