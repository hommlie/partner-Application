package com.hommlie.partner.ui.jobs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.databinding.RowServiceBinding
import com.hommlie.partner.model.ServiceModel

class ServiceDetailsAdapter(
    private val serviceList: List<ServiceModel>
) : RecyclerView.Adapter<ServiceDetailsAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(
        private val binding: RowServiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: ServiceModel) {
            binding.tvJobinfo.text = service.serviceName
            binding.tvCategory.text = service.categoryName
            binding.tvSubcategory.text = service.subcategoryName
            binding.tvUnit.text = service.variation
            binding.tvSrid.text = service.id

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = RowServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(serviceList[position])
    }

    override fun getItemCount(): Int = serviceList.size
}
