package com.hommlie.partner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.model.SimInfo

class SimNumberAdapter(
private val simList: List<SimInfo>,
private val onClick: (SimInfo) -> Unit
) : RecyclerView.Adapter<SimNumberAdapter.SimViewHolder>() {

    inner class SimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelText = itemView.findViewById<TextView>(android.R.id.text1)

        init {
            itemView.setOnClickListener {
                onClick(simList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return SimViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimViewHolder, position: Int) {
        holder.labelText.text = "${simList[position].label}: ${simList[position].number}"
    }

    override fun getItemCount(): Int = simList.size
}
