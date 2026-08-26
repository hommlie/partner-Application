package com.hommlie.partner.model

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

data class RatingOption(
    val container: LinearLayout,
    val card: MaterialCardView,
    val numberText: TextView,
    val icon: ImageView,
    val label: TextView,
    val accentColor: Int,
    val tintColor: Int,
    val ratingValue: Int,      // 1..5, sent to backend / analytics
    val ratingKey: String      // e.g. "rude", "friendly" — sent to backend
)
