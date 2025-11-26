package com.hommlie.partner.ui.jobs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hommlie.partner.R
import com.hommlie.partner.databinding.RowJobsBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.utils.CommonMethods
import java.net.URLEncoder

class NewJobsAdapter(private val onCheckOrders: (NewOrderData) -> Unit,
private val onClick_raiseHelp: (NewOrderData) -> Unit)
: RecyclerView.Adapter<NewJobsAdapter.NewJobsViewHolder>() {


    private val items = mutableListOf<NewOrderData>()

    fun submitList(list: List<NewOrderData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class NewJobsViewHolder(val binding : RowJobsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewJobsViewHolder {
        val binding = RowJobsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false
            )
        return NewJobsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NewJobsViewHolder, position: Int) {
        val job = items[position]

        holder.binding.apply {

            if (job.payment_type =="1") {
                var price = job.price
                if (price != null) {
                    if (!price.contains(".")) {
                        price = price + ".00"
                    }
                    tvOrderCost.text = price
                } else {
                    tvOrderCost.text = "ask to admin"
                }
            }else{
                tvOrderCost.text = "Paid"
            }

            tvAddress.text=job.address?:"-"
            tvName.text=job.name?:"-"
            tvJobinfo.text=job.serviceName?:"-"
            tvServiceType.text=job.attribute?:"-"
            tvCategory.text=job.categoryName?:"-"
            tvSubcategory.text=job.subcategoryName?:"-"
            tvUnit.text= "${job.variation}"     //"${job.quantity?.toFloat()?.toInt()?:"-"} ${job.quantityType?:""}"
            tvServicetime.text="Time : ${job.desiredTime?:"-"} | ${job.desiredDate?:"-"}"
            tvOrderno.text=job.orderId.toString()

            if (job.orderStatus == "4") {
                holder.itemView.isEnabled = false
                cl2.visibility = View.GONE
                viewNav.visibility = View.GONE
                tvTrackOrder.visibility = View.INVISIBLE
                tvHelp.visibility = View.GONE
                ivStamp.visibility = View.VISIBLE
                trackCard.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.parrotgreen)
            } else {
                holder.itemView.isEnabled = true
                cl2.visibility = View.VISIBLE
                viewNav.visibility = View.VISIBLE
                tvHelp.visibility = View.VISIBLE
                tvTrackOrder.visibility = View.VISIBLE
                ivStamp.visibility = View.GONE
                trackCard.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.job_stroke_color)
            }

            tvHelp.setOnClickListener {
                onClick_raiseHelp(job)
            }


            holder.itemView.setOnClickListener {
                CommonMethods.showConfirmationDialog(
                    holder.itemView.context,
                    "Confirmation !...",
                    "Order Id     : ${job.orderId}\nOrder No.  : ${job.orderNo}\nAre you sure you want to start this service.",
                    false,
                    true
                ) {
//                    val gson = Gson()
//                    val json = gson.toJson(job)
//                    val intent = Intent(holder.itemView.context , JobDetails::class.java)
//                    intent.putExtra("job_data",json)
//                    (holder.itemView.context).startActivity(intent)
                    onCheckOrders(job)
                }

            }

            llCall.setOnClickListener {
                openDialPad(holder.itemView.context,job.mobile)
            }

           llWhatsapp.setOnClickListener {
                openWhatsApp(holder.itemView.context,job.mobile,"Hii "+job.name+" Trackiffy technician coming, are you available now??")
           }

            llShare.setOnClickListener {
                val context = holder.itemView.context
                val link = job.address_lat_lng?.trim()

                if (!link.isNullOrEmpty() && link != ",") {
                    when {
                        link.contains("maps.app.goo.gl") -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            context.startActivity(intent)
                        }

                        Regex("@([-\\d.]+),([-\\d.]+)").containsMatchIn(link) -> {
                            val locationPattern = Regex("@([-\\d.]+),([-\\d.]+)")
                            val match = locationPattern.find(link)
                            if (match != null) {
                                val latitude = match.groupValues[1]
                                val longitude = match.groupValues[2]
                                openNavigation(context, latitude, longitude)
                            }
                        }

                        Regex("^[-\\d.]+,\\s*[-\\d.]+\$").matches(link) -> {
                            val parts = link.split(",")
                            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                val latitude = parts[0].trim()
                                val longitude = parts[1].trim()
                                openNavigation(context, latitude, longitude)
                            } else {
                                Toast.makeText(context, "Invalid location coordinates", Toast.LENGTH_SHORT).show()
                            }
                        }

                        else -> {
                            try {
                                val uri = Uri.parse(link)
                                if (uri.scheme != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Invalid link format", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open map link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Address link is not available", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun openDialPad(context: Context, phoneNum: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNum")
        context.startActivity(intent)
    }

    fun openWhatsApp(context: Context, phoneNum: String, defaultMessage: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$phoneNum&text=${URLEncoder.encode(defaultMessage, "UTF-8")}"
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle exceptions, e.g., if WhatsApp is not installed on the device
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNavigation(context: Context, latitude: String, longitude: String) {
        val destination = "$latitude,$longitude"
        val gmmIntentUri = Uri.parse("google.navigation:q=$destination&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }


}