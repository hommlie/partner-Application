package com.hommlie.partner.ui.addwork

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowBillHistoryBinding
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_ddmmmyyyy

class ExpenseHistoryAdapter :
    RecyclerView.Adapter<ExpenseHistoryAdapter.ExpenseViewHolder>() {

    private val expenseList = mutableListOf<ExpenseHistory>()

    inner class ExpenseViewHolder(val binding: RowBillHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = RowBillHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = expenseList[position]
        with(holder.binding) {
            tvDate.text = item.created_at?.toFormattedDate_ddmmmyyyy()
            tvTitle.text = item.title
            val amountValue = item.amount?.toDoubleOrNull() ?: 0.0
            tvAmount.text = "₹ %.2f".format(amountValue)
            tvStatus.text = item.status

            val context = root.context
            val colorRes = when (item.status?.lowercase()) {
                "approved" -> R.color.green
                "pending" -> R.color.orange
                else -> R.color.medium_gray
            }
            tvStatus.setTextColor(ContextCompat.getColor(context, colorRes))

            ivShowdetails.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = expenseList.size

    fun submitList(newList: List<ExpenseHistory>?) {
        expenseList.clear()
        if (!newList.isNullOrEmpty()) {
            expenseList.addAll(newList)
        }
        notifyDataSetChanged()
    }
}

