package com.hommlie.partner.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowHomeOptionsBinding
import com.hommlie.partner.model.HomeOptionModel
import com.hommlie.partner.ui.attendence.ActAttendance
import com.hommlie.partner.ui.leaderboard.Leaderboard
import com.hommlie.partner.ui.refer.ReferEarn
import com.hommlie.partner.ui.travellog.ActTravelLogs
import com.hommlie.partner.ui.wallet.Wallet

class OptionsAdapter(
    private val context: Context,
    private val items: List<HomeOptionModel>
) : RecyclerView.Adapter<OptionsAdapter.TimelineViewHolder>() {

        inner class TimelineViewHolder(val binding: RowHomeOptionsBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
            val binding = RowHomeOptionsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TimelineViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
            val option = items[position]
            holder.binding.apply {
                tvIconname.text = option.name

                Glide.with(holder.itemView)
                    .load(option.iconUrl)
                    .placeholder(R.drawable.ic_bike)
                    .error(R.drawable.ic_bike)
                    .into(ivIcon)

                mcvItem.setOnClickListener {
                    val context = holder.itemView.context

                    when (option.id) {
                        "1" -> {
                            val intent = Intent(context, ActTravelLogs::class.java)
                            context.startActivity(intent)
                        }
                        "2" -> {
                            val intent = Intent(context, ActAttendance::class.java)
                            context.startActivity(intent)
                        }
                        "3" -> {
                            val intent = Intent(context, Wallet::class.java)
                            context.startActivity(intent)
                        }
                        "4" -> {

                        }
                        "5" -> {
                            val intent = Intent(context, Leaderboard::class.java)
                            context.startActivity(intent)
                        }
                        "6" -> {
                            val intent = Intent(context, ReferEarn::class.java)
                            context.startActivity(intent)
                        }
                        else -> {
                            // Optionally show a toast or log unhandled ID
                        }
                    }
                }


            }
        }

        override fun getItemCount(): Int = items.size
    }