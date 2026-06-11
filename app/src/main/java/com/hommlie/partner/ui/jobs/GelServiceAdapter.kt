package com.hommlie.partner.ui.jobs

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowGelServicesBinding
import com.hommlie.partner.model.DateModel
import com.hommlie.partner.model.ScheduleService
import com.hommlie.partner.model.SelectedGelService
import com.hommlie.partner.model.Service
import com.hommlie.partner.model.ServiceModel
import com.hommlie.partner.model.TimeSlot

class GelServiceAdapter(
    private val serviceList: List<Service>,
    private val timeSlots: List<TimeSlot>
) : RecyclerView.Adapter<GelServiceAdapter.GelServiceAdapterViewHolder>() {

    private val selections = mutableMapOf<Int, SelectedGelService>()

    init {

        Log.e("GEL_TEST", "Adapter Size = ${serviceList.size}")

        serviceList.forEach {
            Log.e("GEL_TEST", "Service = ${it.productName}")
        }

        serviceList.forEach {
            selections[it.id ?: 0] =
                SelectedGelService(orderId = it.id ?: 0)
        }
    }

    inner class GelServiceAdapterViewHolder(
        private val binding: RowGelServicesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: Service) {

            try {

                Log.e("GEL_TEST", "START ${service.productName}")

                binding.tvChemicalName.text = service.productName

                binding.tvDueDate.text = formatDateForUi(service.expectedServiceDate)

                binding.tvDaysLeft.text = getDaysLeftText(service.gelServiceIn)

                binding.tvAllowedDates.text =
                    formatAllowedWindow(
                        service.minDate,
                        service.maxDate
                    )

                Log.e("GEL_TEST", "DATE DONE")

                val dateAdapter = DateAdapter {

                    selections[service.id ?: 0]?.selectedDate =
                        it.date
                }

                Log.e("GEL_TEST", "DATE ADAPTER CREATED")

                binding.rvDate.layoutManager =
                    LinearLayoutManager(
                        binding.root.context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )

                binding.rvDate.adapter = dateAdapter

                dateAdapter.updateList(
                    generateDateList(
                        service.minDate,
                        service.maxDate
                    )
                )

                Log.e("GEL_TEST", "DATE ADAPTER DONE")

                val timeAdapter = TimeAdapter {

                    selections[service.id ?: 0]?.selectedTimeSlotId =
                        it.id ?: 0
                }

                binding.rvTime.layoutManager =
                    GridLayoutManager(
                        binding.root.context,
                        3
                    )

                binding.rvTime.adapter = timeAdapter

                timeAdapter.updateList(timeSlots)

                binding.tvSchedule.setOnClickListener {
                    binding.llFulldetail.visibility =
                        if (binding.llFulldetail.isVisible) View.GONE else View.VISIBLE
                }

                Log.e("GEL_TEST", "TIME ADAPTER DONE")

            } catch (e: Exception) {

                Log.e("GEL_TEST", "BIND CRASH", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GelServiceAdapterViewHolder {
        val binding = RowGelServicesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GelServiceAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GelServiceAdapterViewHolder, position: Int) {
        Log.d("GEL_TEST", "onBindViewHolder $position")
        holder.bind(serviceList[position])
    }

    override fun getItemCount(): Int = serviceList.size

    private fun generateDateList(
        minDate: String?,
        maxDate: String?
    ): List<DateModel> {

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        val startDate = inputFormat.parse(minDate ?: return emptyList())
        val endDate = inputFormat.parse(maxDate ?: return emptyList())

        val calendar = Calendar.getInstance()
        calendar.time = startDate

        val list = mutableListOf<DateModel>()

        while (!calendar.time.after(endDate)) {

            val date = calendar.time

            list.add(
                DateModel(
                    date = inputFormat.format(date),
                    display = SimpleDateFormat("dd", Locale.ENGLISH).format(date),
                    day = SimpleDateFormat("EEE", Locale.ENGLISH).format(date),
                    isSelected = false //list.isEmpty()
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return list
    }

    fun getScheduleRequest(): List<ScheduleService> {

        return selections.values.mapNotNull {

            val date = it.selectedDate
            val slot = it.selectedTimeSlotId

            if (date != null && slot != null) {

                ScheduleService(
                    orderId = it.orderId,
                    desiredDate = date,
                    desiredTimeslot = slot
                )

            } else {
                null
            }
        }
    }

    private fun formatDateForUi(date: String?): String {

        if (date.isNullOrEmpty()) return ""

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val output = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)

            output.format(input.parse(date)!!)
        } catch (e: Exception) {
            date
        }
    }

    private fun getDaysLeftText(days: Int?): String {

        return when (days ?: 0) {
            0 -> "Today"
            1 -> "1 day"
            else -> "${days ?: 0} days"
        }
    }

    private fun formatAllowedWindow(
        minDate: String?,
        maxDate: String?
    ): String {

        if (minDate.isNullOrEmpty() || maxDate.isNullOrEmpty())
            return ""

        return try {

            val input =
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            val start = input.parse(minDate)!!
            val end = input.parse(maxDate)!!

            val startFormat =
                SimpleDateFormat("MMMM dd", Locale.ENGLISH)

            val endFormat =
                SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)

            "${startFormat.format(start)} - ${endFormat.format(end)}"

        } catch (e: Exception) {
            "$minDate - $maxDate"
        }
    }
}