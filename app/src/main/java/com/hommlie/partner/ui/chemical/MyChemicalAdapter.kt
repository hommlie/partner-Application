package com.hommlie.partner.ui.chemical

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowChemicalBinding
import com.hommlie.partner.model.Chemical
import com.hommlie.partner.utils.CommonMethods.toFormattedDate

class MyChemicalAdapter : RecyclerView.Adapter<MyChemicalAdapter.ViewHolder>() {

    private val items = mutableListOf<Chemical>()
    private val expandedPositionSet = mutableSetOf<Int>()

    fun submitList(list: List<Chemical>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: RowChemicalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Chemical, position: Int) {

            binding.tvSno.text = "${position + 1}."
            binding.tvCategory.text = item.category
            binding.tvChemicalName.text = item.subCategory
            binding.tvQuantity.text = "${item.quantity} ${item.type}"
            binding.tvBatchno.text = "${item.batch_number?:"-"}"
            binding.tvGiven.text = item.createdAt.toFormattedDate()
            binding.tvExpiry.text = item.updatedAt.toFormattedDate()
            binding.tvRemarks.text = "Use in well-ventilated areas"

            val isExpanded = expandedPositionSet.contains(position)
            binding.llFulldetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivArrow.rotation = if (isExpanded) 180f else 0f

            binding.mcvRoot.setOnClickListener {
                if (isExpanded) expandedPositionSet.remove(position)
                else expandedPositionSet.add(position)
                notifyItemChanged(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowChemicalBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }
}
