package com.hommlie.partner.model

data class ChemicalItemUI(
    val chemicalId:Int,

    val chemicalName:String,

    val availableQty: String,

    val unit:String,

    val inventory_id:Int,

    var usedQty:String=""
)
