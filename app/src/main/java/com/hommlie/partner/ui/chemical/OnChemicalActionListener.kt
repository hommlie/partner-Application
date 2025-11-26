package com.hommlie.partner.ui.chemical

interface OnChemicalActionListener {

    fun onAcknowledgeClicked(chemicalId: Int)
    fun onReportIssueClicked(chemicalId: Int)

}