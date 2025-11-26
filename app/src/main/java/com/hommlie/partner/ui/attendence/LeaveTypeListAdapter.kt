package com.hommlie.partner.ui.attendence

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowLeavetypesBinding
import com.hommlie.partner.model.LeaveTypeList_Data

class LeaveTypeListAdapter : RecyclerView.Adapter<LeaveTypeListAdapter.LeaveTypeListViewHolder>() {

    private val items = mutableListOf<LeaveTypeList_Data>()

    fun submitList(list: List<LeaveTypeList_Data>?) {
        items.clear()
        list?.let { items.addAll(it) }
        notifyDataSetChanged()
    }

    inner class LeaveTypeListViewHolder(val binding : RowLeavetypesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveTypeListViewHolder {
        val binding  = RowLeavetypesBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return LeaveTypeListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: LeaveTypeListViewHolder, position: Int) {
        val item = items[position]

        with(holder.binding) {

            tvLeavetypeNo.text = item.leave_type_no.toString()

            tvLeavetypeName.text = item.leave_type_name

            try {
                tvLeavetypeName.setTextColor(Color.parseColor(item.leave_type_name_color))
            } catch (e: IllegalArgumentException) {

                tvLeavetypeName.setTextColor(ContextCompat.getColor(root.context, R.color.color_344054))
            }

            Glide.with(ivLeavetypeicon.context)
                .load(item.leave_type_icon)
                .placeholder(R.drawable.ic_manage_accounr)
                .error(R.drawable.ic_manage_accounr)
                .into(ivLeavetypeicon)
        }
    }

}