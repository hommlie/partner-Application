package com.hommlie.partner.ui.chemical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.Chemical
import com.hommlie.partner.model.ChemicalsResponse
import com.hommlie.partner.model.VerifyOtp
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.repository.MyChemicalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyChemicalViewModel @Inject constructor(
    private val repository: MyChemicalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<ChemicalsResponse>>(UIState.Idle)
    val uiState: StateFlow<UIState<ChemicalsResponse>> = _uiState

    private val _allChemicals = MutableStateFlow<List<Chemical>>(emptyList())
    private val _filteredChemicals = MutableStateFlow<List<Chemical>>(emptyList())
    val filteredChemicals: StateFlow<List<Chemical>> = _filteredChemicals

    fun fetchChemicals(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            try {
                val response = repository.getChemicalsHave(data)
                if (response.status == 1 && response.data != null) {
                    _allChemicals.value = response.data
                    _filteredChemicals.value = response.data
                    _uiState.value = UIState.Success(response)
                } else {
                    _uiState.value = UIState.Error(response.message ?: "Empty data")
                }
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.localizedMessage ?: "Error occurred")
            }
        }
    }

    fun filter(query: String) {
        val cleanQuery = query.trim()
        _filteredChemicals.value = if (cleanQuery.isEmpty()) {
            _allChemicals.value
        } else {
            _allChemicals.value.filter {
                it.subCategory.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    fun resetUIState() {
        _uiState.value = UIState.Idle
    }
}
