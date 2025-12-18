package com.hommlie.partner.utils

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.hommlie.partner.R

fun setupToolbar(
    view: View,
    title: String,
    activity: AppCompatActivity,
    backgroundColor: Int,
    textColor: Int,
    backArrowTint : Int?=null,
) {
    val ivBack = view.findViewById<MaterialCardView>(R.id.ivBack)
    val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
    val ivBackArrow = view.findViewById<ImageView>(R.id.iv_back_arrow)

    tvTitle.text = title
    ivBack.setOnClickListener {
        activity.onBackPressedDispatcher.onBackPressed()
    }
    view.setBackgroundColor(ContextCompat.getColor(activity, backgroundColor))
    backArrowTint?.let {
        ivBackArrow.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(activity, it))
    }
    tvTitle.setTextColor(ContextCompat.getColor(activity,textColor))


}