package com.hommlie.partner.sharedviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.UploadPhoto
import com.hommlie.partner.repository.UploadImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class UploadImageViewModel  @Inject constructor(private val repository: UploadImage) : ViewModel(){

    private val _uploadImageUiState = MutableStateFlow<UIState<UploadPhoto>>(UIState.Idle)
    val uploadImageUiState: StateFlow<UIState<UploadPhoto>> = _uploadImageUiState

    fun uploadImage(image : MultipartBody.Part){
        viewModelScope.launch {
            _uploadImageUiState.value = UIState.Loading

            when(val result = repository.uploadImage(image)){
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1 && response.data != null) {
                        _uploadImageUiState.value = UIState.Success(response.data)
                    } else {
                        _uploadImageUiState.value = UIState.Error(response.message ?: "Something went wrong")
                    }
                }
                is ApiResult.Error -> {
                    _uploadImageUiState.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _uploadImageUiState.value = UIState.Error("No internet connection")
                }
                is ApiResult.UnknownError -> {
                    _uploadImageUiState.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun resetUploadImageUiState() {
        _uploadImageUiState.value = UIState.Idle
    }
}