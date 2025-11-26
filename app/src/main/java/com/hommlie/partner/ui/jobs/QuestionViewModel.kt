package com.hommlie.partner.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.OrderQuestions
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.repository.JobsRepository
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val sharePreference: SharePreference,
    private val repository: JobsRepository
)  : ViewModel(){

    private val _uiState = MutableStateFlow<UIState<OrderQuestions>>(UIState.Idle)
    val uiState: StateFlow<UIState<OrderQuestions>> = _uiState

    private val _uiStateSubmitAnswr = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateSubmitAnswr: StateFlow<UIState<SingleResponse>> = _uiStateSubmitAnswr



    fun callApiforQuestions(hashMap: HashMap<String, String>, questionfor: String) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            delay(800)
            try {
                val response = repository.getQuestions(hashMap)

                if (response.status == 1){
                    _uiState.value = UIState.Success(response)
                }else{
                    _uiState.value = UIState.Error(response.message ?: "Something went wrong")
                }

            }catch (e : Exception){
                _uiState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun submitAnswers(
        params: Map<String, RequestBody>,
        images: List<MultipartBody.Part>
    ) {
        viewModelScope.launch {
            _uiStateSubmitAnswr.value = UIState.Loading
            delay(800)
            try {
                val response = repository.submitAnswer(params, images)
                if (response.status ==1){
                    _uiStateSubmitAnswr.value = UIState.Success(response)
                }else{
                    _uiStateSubmitAnswr.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateSubmitAnswr.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }




    fun resetUIState() {
        _uiState.value = UIState.Idle
    }

    fun resetUISubmitAnswer() {
        _uiStateSubmitAnswr.value = UIState.Idle
    }
}