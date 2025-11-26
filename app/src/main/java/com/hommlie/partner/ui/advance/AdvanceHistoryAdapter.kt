package com.hommlie.partner.ui.advance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowBillHistoryBinding
import com.hommlie.partner.model.AdvanceRequestList
import com.hommlie.partner.utils.CommonMethods.toCapwords
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_yyyymmdd

class AdvanceHistoryAdapter(private val onClick: (AdvanceRequestList) -> Unit) : RecyclerView.Adapter<AdvanceHistoryAdapter.ExpenseViewHolder>() {

    private val requestList = mutableListOf<AdvanceRequestList>()

    inner class ExpenseViewHolder(val binding: RowBillHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = RowBillHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = requestList[position]

        with(holder.binding) {
            tvDate.text = item.requestDate?.toFormattedDate_yyyymmdd()
            tvTitle.text = item.reason?.toCapwords().orEmpty()
            tvStatus.text = item.status.orEmpty()

            val context = root.context

            val requestedAmount = item.requestedAmount?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val approvedAmount = item.approvedAmount?.replace(",", "")?.toDoubleOrNull() ?: 0.0

            val (amountText, colorRes) = when (item.status?.lowercase()) {
                "approved" -> "₹ %.2f".format(approvedAmount) to R.color.color_249370
                "pending" -> "₹ %.2f".format(requestedAmount) to R.color.orange
                "rejected" -> "₹ %.2f".format(requestedAmount) to R.color.red_logout
                else -> "₹ %.2f".format(requestedAmount) to R.color.medium_gray
            }

            tvAmount.text = amountText
            tvStatus.setTextColor(ContextCompat.getColor(context, colorRes))

            ivShowdetails.setOnClickListener {
                onClick(item)
            }

        }
    }


    override fun getItemCount(): Int = requestList.size

    fun submitList(newList: List<AdvanceRequestList>?) {
        requestList.clear()
        if (!newList.isNullOrEmpty()) {
            requestList.addAll(newList)
        }
        notifyDataSetChanged()
    }

}