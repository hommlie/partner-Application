package com.hommlie.partner.model

data class RewardItem(
    val id: String,
    val rewardType: String,
    val productName: String,
    val description: String,
    val worthText: String,
    val isLocked: Boolean,
    val imageRes: String,
    val requiredCoin: Int,
)

