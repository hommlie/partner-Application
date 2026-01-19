package com.hommlie.partner.ui.wallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowBillHistoryBinding
import com.hommlie.partner.databinding.RowWalletBinding
import com.hommlie.partner.model.AdvanceRequestList
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.RedeemedData

class WalletAdapter(private val onClick: (RedeemedData) -> Unit) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    private val items = mutableListOf<RedeemedData>()

    inner class WalletViewHolder(val binding: RowBillHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = RowBillHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvTitle.text = item.itemName.toString()
        holder.binding.tvAmount.text = item.coinsRedeemed
        holder.binding.tvDate.text = item.createdAt

        val colorRes = when (item.statusLabel.lowercase()) {
            "approved" -> R.color.color_249370
            "pending"  -> R.color.orange
            "rejected" -> R.color.red_logout
            else       -> R.color.medium_gray
        }

        holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))
        holder.binding.tvStatus.text = item.statusLabel

        holder.binding.ivShowdetails.setOnClickListener {
            onClick(item)
        }
    }
    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<RedeemedData>?) {
        items.clear()
        if (!newList.isNullOrEmpty()) {
            items.addAll(newList)
        }
        notifyDataSetChanged()
    }
}

