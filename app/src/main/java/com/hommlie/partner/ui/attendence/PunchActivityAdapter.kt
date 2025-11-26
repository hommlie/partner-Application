package com.hommlie.partner.ui.attendence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowPunchlogsBinding
import com.hommlie.partner.model.PunchSession
import com.hommlie.partner.utils.CommonMethods.formatToIST
import com.hommlie.partner.utils.CommonMethods.safeParseInstant
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class PunchActivityAdapter : RecyclerView.Adapter<PunchActivityAdapter.PunchActivityViewHolder>() {


    private val items = mutableListOf<PunchSession>()

    fun submitList(list: List<PunchSession>?) {
        items.clear()
        list?.let { items.addAll(it) }
        notifyDataSetChanged()
    }

    inner class PunchActivityViewHolder(val binding : RowPunchlogsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchActivityViewHolder {
        val binding  = RowPunchlogsBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PunchActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PunchActivityViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {

            tvPunchtime.text = formatToIST(item.punchIn)
            tvPunchtimeout.text = formatToIST(item.punchOut)

            // Last item → Hide break
            if (position == items.lastIndex) {
                clBreak.visibility = View.GONE
            } else {
                val nextSession = items[position + 1]
                val breakTime = calculateBreak(item, nextSession)

                if (breakTime != null) {
                    clBreak.visibility = View.VISIBLE
                    tvBreak.text = "Break Time : $breakTime"
                } else {
                    clBreak.visibility = View.VISIBLE
                    tvBreak.text = "Break Time : --|--"
                }
            }


        }
    }

    override fun getItemCount(): Int {
        return items.size
    }


    private fun calculateBreak(current: PunchSession, next: PunchSession): String? {
        // Break exists only if current punch_out and next punch_in are valid
        if (current.punchOut.isNullOrEmpty() || next.punchIn.isEmpty()) return null

        return try {
            val punchOutTime = safeParseInstant(current.punchOut)
            val nextPunchInTime = safeParseInstant(next.punchIn)

            val breakSeconds = Duration.between(punchOutTime, nextPunchInTime).seconds

            if (breakSeconds > 0) {
                val hours = breakSeconds / 3600
                val minutes = (breakSeconds % 3600) / 60
                val seconds = breakSeconds % 60

                String.format("%02d:%02d", hours, minutes, seconds)
            } else null
        } catch (e: Exception) {
            null
        }
    }


}