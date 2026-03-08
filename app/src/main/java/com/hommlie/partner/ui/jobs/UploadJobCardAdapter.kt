package com.hommlie.partner.ui.jobs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowJobcardBinding
import com.hommlie.partner.model.ServiceModel

class UploadJobCardAdapter(
    private val serviceList: List<ServiceModel>,
    private val onClickPhoto: (ServiceModel, Int) -> Unit
) : RecyclerView.Adapter<UploadJobCardAdapter.UploadJobCardAdapterViewHolder>() {

    inner class UploadJobCardAdapterViewHolder(private val binding: RowJobcardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: ServiceModel) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                binding.tvCount.text = (position + 1).toString()
                binding.tvProductName.text = service.serviceName
                if (service.localImageUri != null) {
                    binding.ivJobcardImage.setImageURI(service.localImageUri)
                } else {
                    binding.ivJobcardImage.setImageResource(R.drawable.ic_photo_camera)
                }
                binding.mcvJobcard.setOnClickListener {
                    onClickPhoto(service, bindingAdapterPosition)
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadJobCardAdapterViewHolder {
        val binding = RowJobcardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UploadJobCardAdapterViewHolder(binding)
    }
    override fun onBindViewHolder(holder: UploadJobCardAdapterViewHolder, position: Int) {
        holder.bind(serviceList[position])
    }
    override fun getItemCount(): Int = serviceList.size

    fun isEverythingUploaded(): Boolean {
        return serviceList.all { it.localImageUri != null }
    }
}