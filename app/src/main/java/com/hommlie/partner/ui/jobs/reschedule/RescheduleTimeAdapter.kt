package com.hommlie.partner.ui.jobs.reschedule

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowTimeslotBinding
import com.hommlie.partner.model.TimeSlot

class RescheduleTimeAdapter (private val selectedTime : (TimeSlot) -> Unit) : RecyclerView.Adapter<RescheduleTimeAdapter.VH>() {

    private var list = mutableListOf<TimeSlot>()

    private lateinit var context: Context

    private val primaryColor by lazy { ContextCompat.getColor(context, R.color.color_primary) }
    private val primaryLight by lazy { ContextCompat.getColor(context, R.color.color_primary_light) }
    private val blackColor by lazy { ContextCompat.getColor(context, R.color.Blackcolor) }
    private val grayBorder by lazy { ContextCompat.getColor(context, R.color.gray_border) }

    private val mediumFont by lazy { ResourcesCompat.getFont(context, R.font.inter_medium) }
    private val regularFont by lazy { ResourcesCompat.getFont(context, R.font.inter_regular) }

    fun updateList(newList: List<TimeSlot>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        context = parent.context
        val binding = RowTimeslotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = list.size

    inner class VH(val binding: RowTimeslotBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        holder.binding.tvTime.text = item.label

        val card = holder.binding.mcvRoot

        val tvTime = holder.binding.tvTime

        when {
            !item.isEnabled -> {
                card.alpha = 0.4f
                card.setCardBackgroundColor(Color.LTGRAY)
                card.strokeColor = grayBorder
                tvTime.setTextColor(blackColor)
                holder.itemView.isClickable = false
            }

            item.isSelected -> {
                card.alpha = 1f
                card.setCardBackgroundColor(primaryLight)
                card.strokeColor = primaryColor

                tvTime.setTextColor(primaryColor)

                tvTime.typeface = mediumFont
            }

            else -> {
                card.alpha = 1f
                card.setCardBackgroundColor(Color.WHITE)
                card.strokeColor = grayBorder

                tvTime.setTextColor(blackColor)

                tvTime.typeface = regularFont
            }
        }

        holder.itemView.setOnClickListener {

            if (!item.isEnabled) return@setOnClickListener

            list.forEach { it.isSelected = false }
            item.isSelected = true
            selectedTime(item)
            notifyDataSetChanged()
        }
    }
}