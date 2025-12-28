package com.hommlie.partner.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R

class BannerIndicatorAdapter(private val count: Int) :
    RecyclerView.Adapter<BannerIndicatorAdapter.IndicatorVH>() {

    private var activePos = 0
    private var progressValue = 0

    private val activeWidth = 43   // dp
    private val inactiveWidth = 12 // dp

    inner class IndicatorVH(val progress: ProgressBar) :
        RecyclerView.ViewHolder(progress)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndicatorVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_banner_indicator, parent, false)
        return IndicatorVH(view as ProgressBar)
    }

    override fun onBindViewHolder(holder: IndicatorVH, position: Int) {

        val params = holder.progress.layoutParams
        params.width = dpToPx(
            holder.progress.context,
            if (position == activePos) activeWidth else inactiveWidth
        )
        holder.progress.layoutParams = params

        if (position == activePos) {
            holder.progress.progress = progressValue
        } else {
            holder.progress.progress = 0
        }
    }

    override fun getItemCount() = count

    fun setActive(position: Int) {
        val oldPos = activePos
        activePos = position
        progressValue = 0

        notifyItemChanged(oldPos)
        notifyItemChanged(activePos)
    }

    fun updateProgress(value: Int) {
        progressValue = value
        notifyItemChanged(activePos)
    }

    private fun dpToPx(context: android.content.Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
