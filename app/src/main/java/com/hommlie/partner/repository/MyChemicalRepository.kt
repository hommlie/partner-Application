package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.model.ChemicalsResponse
import com.hommlie.partner.model.SingleResponse
import retrofit2.http.Body
import javax.inject.Inject

class MyChemicalRepository @Inject constructor(
    private val apiService: ApiInterface
) {

    suspend fun getChemicalsHave(map : HashMap<String,String>): ChemicalsResponse {
        return apiService.getChemicalsHave(map)
    }

    suspend fun getNewChemicals(map : HashMap<String,String>): ChemicalsResponse {
        return apiService.getNewChemicals(map)
    }

    suspend fun verifyChemicals( map : HashMap<String,String>): SingleResponse {
        return apiService.verifyChemicals(map)
    }
}