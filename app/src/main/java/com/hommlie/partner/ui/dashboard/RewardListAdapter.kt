package com.hommlie.partner.ui.dashboard

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowRewardBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.model.RewardItem

class RewardListAdapter(private val onRedeemClick: (RewardItem) -> Unit) : ListAdapter<RewardItem, RewardListAdapter.RewardViewHolder>(DiffCallback()) {

    private var userCoinBalance = 0

    inner class RewardViewHolder(
        private val binding: RowRewardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RewardItem, position: Int, userCoinBalance: Int) = with(binding) {

            // -------- Category Header Logic --------
//            val shouldShowHeader = position == 0 || getItem(position - 1).rewardType != item.rewardType
//            tvRewardtype.visibility = if (shouldShowHeader) View.VISIBLE else View.GONE
//            tvRewardtype.text = item.rewardType

            // -------- Item Data --------
            tvProductname.text = item.productName
            tvDescription.text = item.description
            tvRequiredcoin.text = "Min required coin "+item.requiredCoin

            Glide.with(ivImage)
                .load(item.imageRes)
                .into(ivImage)

            // -------- Lock logic based on coin balance --------
            val itemStock = item.stockQuantity
            val isActive = item.isActive
            val unlocked = userCoinBalance >= item.requiredCoin

            when {
                isActive != 1 -> {
                    setButton("Locked", R.color.white, R.color.bk_dark_card)
                    mcvBtn.isEnabled = true
                }

                itemStock <= 0 -> {
                    setButton("No Stock", R.color.red_logout, R.color.white)
                    mcvBtn.isEnabled = true
                }

                !unlocked -> {
                    setButton("Low Coins", R.color.white, R.color.bk_dark_card)
                    mcvBtn.isEnabled = false
                }

                else -> {
                    setButton("Redeem", R.color.color_primary, R.color.white)
                    mcvBtn.isEnabled = true
                }
            }

            mcvBtn.setOnClickListener {
                onRedeemClick(item)
            }

        }
        private fun setButton(text: String, bgColor: Int, textColor: Int) {
            binding.mcvBtn.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(itemView.context, bgColor)))
            binding.tvBtnstatus.setTextColor(ContextCompat.getColor(itemView.context, textColor))
            binding.tvBtnstatus.text = text
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val binding = RowRewardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RewardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(getItem(position), position, userCoinBalance)
    }

    fun submitRewardList(list: List<RewardItem>) {
        submitList(list)
    }

    class DiffCallback : DiffUtil.ItemCallback<RewardItem>() {
        override fun areItemsTheSame(oldItem: RewardItem, newItem: RewardItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RewardItem, newItem: RewardItem): Boolean {
            return oldItem == newItem
        }
    }
    fun updateUserCoinBalance(balance: Int) {
        userCoinBalance = balance
        notifyDataSetChanged()
    }


}
