package com.hommlie.partner.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.CmsPageResponse
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyPolicyViewModel @Inject constructor(private val homeRepository: HomeRepository, private val authRepository: AuthRepository) : ViewModel() {

    private val _cmsDataState = MutableStateFlow<UIState<CmsPageResponse>>(UIState.Idle)
    val cmsDataState: StateFlow<UIState<CmsPageResponse>> = _cmsDataState

    fun fetchCmsData() {
        viewModelScope.launch {
            _cmsDataState.value = UIState.Loading
            try {
                val response = homeRepository.getCms()
                _cmsDataState.value = UIState.Success(response)
            } catch (e: Exception) {
                _cmsDataState.value = UIState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetUICMSDataState(){
        _cmsDataState.value= UIState.Idle
    }

}