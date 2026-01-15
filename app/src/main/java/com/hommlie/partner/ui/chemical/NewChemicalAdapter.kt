package com.hommlie.partner.ui.chemical

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowNewChemicalBinding
import com.hommlie.partner.model.Chemical
import com.hommlie.partner.utils.CommonMethods.toFormattedDate

class NewChemicalAdapter(
    private val context: Context,
    private val actionListener: OnChemicalActionListener,
    val listener: (List<Chemical>, Boolean) -> Unit
    ) : ListAdapter<Chemical, NewChemicalAdapter.ViewHolder>(ChemicalDiffCallback()) {

    private val items = mutableListOf<Chemical>()
    private val expandedPositionSet = mutableSetOf<Int>()

    fun updateList(list: List<Chemical>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: RowNewChemicalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Chemical, position: Int) {

            binding.checkbox.visibility = if (item.isCheckboxVisible) View.VISIBLE else View.GONE
            binding.checkbox.isChecked = item.isSelected

            // Hide view_action and ll_action if selected
            if (item.isSelected) {
                binding.viewAction.visibility = View.GONE
                binding.llAction.visibility = View.GONE
            } else {
                binding.viewAction.visibility = View.VISIBLE
                binding.llAction.visibility = View.VISIBLE
            }

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

            binding.ivArrow.setOnClickListener {
                if (isExpanded) expandedPositionSet.remove(position)
                else expandedPositionSet.add(position)
                notifyItemChanged(position)
            }

           itemView.setOnLongClickListener {
               if (items.size > 1) {
                   // 1. Show checkboxes for all
                   items.forEach { it.isCheckboxVisible = true }

                   // 2. Select long-pressed item
                   items[position].isSelected = true

                   notifyDataSetChanged()

                   // Trigger the listener with updated selected list
                   val selectedItems = items.filter { it.isSelected }
//                   listener(selectedItems)
                   listener(selectedItems, selectedItems.size == items.size)

                   true
               }else{
                   false
               }
           }

            itemView.setOnClickListener {
                if (item.isCheckboxVisible) {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(position)

                    val selectedItems = items.filter { it.isSelected }
                    if (selectedItems.isEmpty()) {
                        // Hide all checkboxes
                        items.forEach { it.isCheckboxVisible = false }
                        notifyDataSetChanged()
                    }

//                    listener(selectedItems)
                    listener(selectedItems, selectedItems.size == items.size)

                }
            }


            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (binding.checkbox.isShown) {
                    item.isSelected = isChecked

                    val selectedItems = items.filter { it.isSelected }
                    if (selectedItems.isEmpty()) {
                        // Hide all checkboxes
                        items.forEach { it.isCheckboxVisible = false }
                        notifyDataSetChanged()
                    }

                    notifyItemChanged(position)
//                    listener(selectedItems)
                    listener(selectedItems, selectedItems.size == items.size)

                }
            }

            binding.mcvAcknowledge.setOnClickListener {
                actionListener.onAcknowledgeClicked(item.id)
            }
            binding.mcvReportIssue.setOnClickListener {
//                actionListener.onReportIssueClicked(item.id)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowNewChemicalBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }



    fun clearSelection() {
        items.forEach {
            it.isSelected = false
            it.isCheckboxVisible = false
        }
        notifyDataSetChanged()
    }

    fun selectAll(isChecked: Boolean): List<Chemical> {
        items.forEach {
            it.isCheckboxVisible = isChecked
            it.isSelected = isChecked
        }
        notifyDataSetChanged()
        return items.filter { it.isSelected }
    }


    fun getSelectedItems(): List<Chemical> {
        return items.filter { it.isSelected }
    }

}
