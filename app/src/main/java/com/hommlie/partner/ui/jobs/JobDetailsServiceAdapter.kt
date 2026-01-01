package com.hommlie.partner.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowServiceJobdetailBinding
import com.hommlie.partner.model.ServiceModel

class JobDetailsServiceAdapter(private val serviceList: List<ServiceModel>
) : RecyclerView.Adapter<JobDetailsServiceAdapter.JobDetailsServiceAdapterViewHolder>() {

    inner class JobDetailsServiceAdapterViewHolder(
        private val binding: RowServiceJobdetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: ServiceModel) {
            binding.tvJobinfo.text = service.serviceName
            binding.tvCategory.text = service.categoryName
            binding.tvServiceType.text = service.service_type
            binding.tvServiceplan.text = service.attribute
            binding.tvUnit.text = service.variation
            binding.tvDuration.text = service.duration
            binding.tvSrid.text = service.id
            // serial number
            binding.tvNo.text = (adapterPosition + 1).toString()
            //  last item check
            if (adapterPosition == serviceList.size - 1) {
                binding.viewDash.visibility = View.GONE
            } else {
                binding.viewDash.visibility = View.VISIBLE
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobDetailsServiceAdapterViewHolder {
        val binding = RowServiceJobdetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JobDetailsServiceAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobDetailsServiceAdapterViewHolder, position: Int) {
        holder.bind(serviceList[position])
    }

    override fun getItemCount(): Int = serviceList.size
}