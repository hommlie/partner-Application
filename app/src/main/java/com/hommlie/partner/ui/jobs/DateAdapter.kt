package com.hommlie.partner.ui.jobs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowDateslotBinding
import com.hommlie.partner.model.DateModel

class DateAdapter(
    private val onDateClick: (DateModel) -> Unit
) : RecyclerView.Adapter<DateAdapter.VH>() {

    private var list = mutableListOf<DateModel>()

    fun updateList(newList: List<DateModel>) {
        list = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowDateslotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = list.size

    inner class VH(val binding: RowDateslotBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        holder.binding.tvTime.text = item.display
        holder.binding.tvDay.text = item.day

        holder.binding.tvDay.visibility = if (item.display=="Today"||item.display=="Tomorrow") View.GONE else View.VISIBLE

        val card = holder.binding.mcvRoot

        if (item.isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.color_primary_light))
            card.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.color_primary)
        } else {
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.gray_border)
        }

        holder.binding.view.visibility = if (position==0) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {

            list.forEach { it.isSelected = false }
            item.isSelected = true
            notifyDataSetChanged()

            onDateClick(item)
        }
    }
}