package com.hommlie.partner.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hommlie.partner.databinding.RowBannerBinding

class BannerAdapter(private val originalList: List<String>) :
    RecyclerView.Adapter<BannerAdapter.BannerVH>() {

    private val list = originalList + originalList[0] //  fake last

    inner class BannerVH(val binding: RowBannerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerVH {
        val binding = RowBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerVH(binding)
    }

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        Glide.with(holder.itemView)
            .load(list[position])
            .into(holder.binding.ivBanner)
    }

    override fun getItemCount() = list.size
}

