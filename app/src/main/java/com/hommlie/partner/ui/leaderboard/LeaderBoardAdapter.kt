package com.hommlie.partner.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowLeaderbaordBinding
import com.hommlie.partner.model.AdvanceRequestList
import com.hommlie.partner.model.LeaderBoardData
import com.hommlie.partner.utils.CommonMethods.toCapwords
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_yyyymmdd

class LeaderBoardAdapter: RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardAdapterViewHolder>() {

    private val requestList = mutableListOf<LeaderBoardData>()

    inner class LeaderBoardAdapterViewHolder(val binding: RowLeaderbaordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderBoardAdapterViewHolder {
        val binding = RowLeaderbaordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LeaderBoardAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderBoardAdapterViewHolder, position: Int) {
        val item = requestList[position]

        with(holder.binding) {
            tvRank.text = item.rank.toString()
            tvPoints.text = item.total_coins.toString()+" pts"
            tvName.text = item.emp_name.toString()?.toCapwords()

            val context = root.context

            if (item.is_current_user == true){
                mcvCard.setCardBackgroundColor(ContextCompat.getColor(context,R.color.color_536724))
            }else{
                mcvCard.setCardBackgroundColor(ContextCompat.getColor(context,R.color.color_252728))
            }
        }
    }


    override fun getItemCount(): Int = requestList.size

    fun submitList(newList: List<LeaderBoardData>?) {
        requestList.clear()
        if (!newList.isNullOrEmpty()) {
            requestList.addAll(newList)
        }
        notifyDataSetChanged()
    }

}