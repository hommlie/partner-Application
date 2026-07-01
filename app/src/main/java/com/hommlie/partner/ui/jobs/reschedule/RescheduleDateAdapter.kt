package com.hommlie.partner.ui.jobs.reschedule

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowDateslotBinding
import com.hommlie.partner.model.SelfGeneratedDateModel

class RescheduleDateAdapter(
    private val onDateClick: (SelfGeneratedDateModel) -> Unit
) : RecyclerView.Adapter<RescheduleDateAdapter.VH>() {

    private var list = mutableListOf<SelfGeneratedDateModel>()

    private lateinit var context: Context

    private val primaryColor by lazy { ContextCompat.getColor(context, R.color.color_primary) }
    private val primaryLight by lazy { ContextCompat.getColor(context, R.color.color_primary_light) }
    private val grayTextColor by lazy { ContextCompat.getColor(context, R.color.gray_text_color) }
    private val grayBorder by lazy { ContextCompat.getColor(context, R.color.gray_border) }
    private val white by lazy { ContextCompat.getColor(context, R.color.white) }

    fun updateList(newList: List<SelfGeneratedDateModel>) {
        list = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        context = parent.context
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
            holder.binding.tvTime.setTextColor(primaryColor)
            holder.binding.tvDay.setTextColor(primaryColor)
            card.setCardBackgroundColor(primaryLight)
            card.strokeColor = primaryColor
        } else {
            holder.binding.tvTime.setTextColor(grayTextColor)
            holder.binding.tvDay.setTextColor(grayTextColor)
            card.setCardBackgroundColor(white)
            card.strokeColor = grayBorder
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