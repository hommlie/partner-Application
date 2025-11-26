package com.hommlie.partner.utils

import android.view.View
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
    textColor: Int
) {
    val ivBack = view.findViewById<MaterialCardView>(R.id.ivBack)
    val tvTitle = view.findViewById<TextView>(R.id.tvTitle)

    tvTitle.text = title
    ivBack.setOnClickListener {
        activity.onBackPressedDispatcher.onBackPressed()
    }
    view.setBackgroundColor(ContextCompat.getColor(activity, backgroundColor))
    tvTitle.setTextColor(ContextCompat.getColor(activity,textColor))

}