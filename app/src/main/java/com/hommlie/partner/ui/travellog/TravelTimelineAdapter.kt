package com.hommlie.partner.ui.travellog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowTravelTimlineBinding
import com.hommlie.partner.model.JobItem
import com.hommlie.partner.utils.CommonMethods
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TravelTimelineAdapter(
    private val items: List<JobItem>
) : RecyclerView.Adapter<TravelTimelineAdapter.TimelineViewHolder>() {

    inner class TimelineViewHolder(val binding: RowTravelTimlineBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = RowTravelTimlineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val job = items[position]
        holder.binding.apply {
            jobId.text = "SR ID : ${job.job_id}"
            stationName.text = job.location_name
            arrivalTime.text = "${CommonMethods.convertToIndiaTime(job.start_time?:"").substringAfter(" ")}"
            departureTime.text = "${CommonMethods.convertToIndiaTime(job.end_time?:"").substringAfter(" ")}"
            platformInfo.text = "Distance: ${job.distance_from_previous} km"

            if (items.size == 1) {
                // Only one item in the list
                lineTop.setBackgroundResource(R.drawable.bg_travel_timeline)
                icBike.visibility = View.VISIBLE
//                val params = lineTop.layoutParams
//                params.height = (holder.itemView.context).resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._30sdp)
//                lineTop.layoutParams = params
            } else if (position == 0) {
                // First item
                lineTop.setBackgroundResource(R.drawable.bg_travel_timeline)
                icBike.visibility = View.GONE
            } else if (position == items.size - 1) {
                // Last item
                lineTop.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.color_primary_light))
                icBike.visibility = View.VISIBLE
                val params = lineTop.layoutParams
                params.height = (holder.itemView.context).resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._55sdp)
                lineTop.layoutParams = params
            } else {
                // Middle items
                icBike.visibility = View.GONE
                lineTop.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.color_primary_light))
            }

        }
    }

    override fun getItemCount(): Int = items.size


}
