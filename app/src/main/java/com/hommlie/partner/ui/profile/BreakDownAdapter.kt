package com.hommlie.partner.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.model.SalaryBreakDown

class BreakDownAdapter(
    private var items: List<SalaryBreakDown>
) : RecyclerView.Adapter<BreakDownAdapter.BreakDownViewHolder>() {

    inner class BreakDownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_breakdown_name)
        val tvAmount: TextView = itemView.findViewById(R.id.tv_breakdown_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakDownViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_salaybreakdown, parent, false)
        return BreakDownViewHolder(view)
    }

    override fun onBindViewHolder(holder: BreakDownViewHolder, position: Int) {
        val item = items[position]
//        holder.tvName.text = item.name
//        holder.tvAmount.text = "\u20b9 "+item.amount
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SalaryBreakDown>) {
        items = newItems
        notifyDataSetChanged()
    }
}
