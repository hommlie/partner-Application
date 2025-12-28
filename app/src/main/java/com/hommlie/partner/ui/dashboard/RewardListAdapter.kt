package com.hommlie.partner.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hommlie.partner.databinding.RowRewardBinding
import com.hommlie.partner.model.RewardItem

class RewardListAdapter :
    ListAdapter<RewardItem, RewardListAdapter.RewardViewHolder>(DiffCallback()) {

    inner class RewardViewHolder(
        private val binding: RowRewardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RewardItem, position: Int) = with(binding) {

            // -------- Category Header Logic --------
            val shouldShowHeader = position == 0 ||
                    getItem(position - 1).rewardType != item.rewardType

            tvRewardtype.visibility = if (shouldShowHeader) View.VISIBLE else View.GONE
            tvRewardtype.text = item.rewardType

            // -------- Item Data --------
            tvProductname.text = item.productName
            tvDescription.text = item.description
            tvWorth.text = item.worthText

            Glide.with(ivImage)
                .load(item.imageRes)
                .into(ivImage)

            llUnlock.visibility = if (item.isLocked) View.VISIBLE else View.GONE
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
        holder.bind(getItem(position), position)
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
}
