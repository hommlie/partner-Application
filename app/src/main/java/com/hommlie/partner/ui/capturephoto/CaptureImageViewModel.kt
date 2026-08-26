package com.hommlie.partner.ui.capturephoto

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CaptureImageViewModel @Inject constructor(
    private val photoStorage: PhotoStorage,
    private val photoWatermarkProcessor: PhotoWatermarkProcessor
) : ViewModel() {

    private val _captureState = MutableStateFlow<UIState<Uri>>(UIState.Idle)
    val captureState: StateFlow<UIState<Uri>> = _captureState.asStateFlow()

    fun processAndSavePhoto(
        photoFile: File,
        metadata: String,
        fileName: String
    ) {
        viewModelScope.launch {

            _captureState.value = UIState.Loading

            var bitmap: Bitmap? = null

            try {

                bitmap = photoWatermarkProcessor.process(
                    photoFile = photoFile,
                    metadata = metadata
                )

                val uri = photoStorage.savePhoto(
                    bitmap = bitmap,
                    fileName = fileName
                )

                _captureState.value =
                    UIState.Success(uri)

            } catch (e: Exception) {

                Log.e(
                    "CaptureImageVM",
                    "Failed to process/save photo",
                    e
                )

                _captureState.value =
                    UIState.Error(
                        e.message ?: "Failed to save photo"
                    )

            } finally {

                bitmap?.recycle()

                photoFile.delete()
            }
        }
    }

    fun resetCaptureState() {
        _captureState.value = UIState.Idle
    }
}