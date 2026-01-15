package com.hommlie.partner.ui.jobs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.PaymentLinkResponse
import com.hommlie.partner.model.PaymentStatus
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.SingleResponseForOrderThree
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class JobDetailsViewModel @Inject constructor(
    private val repository: JobsRepository
) : ViewModel() {


    private var timerJob: Job? = null
    private var job: Job? = null

    private val _canResend = MutableStateFlow(false)
    val canResend: StateFlow<Boolean> = _canResend

    private val _enteredOtp = MutableStateFlow("")
    val enteredOtp: StateFlow<String> = _enteredOtp

    private val _qrImageUrl = MutableStateFlow("")
    val qrImageUrl : StateFlow<String> = _qrImageUrl

    private val _chequeno = MutableStateFlow("")
    val chequeno: StateFlow<String> = _chequeno

    private val _timeLeft = MutableStateFlow("00:45")
    val timeLeft: StateFlow<String> = _timeLeft

    private val _timeStart = MutableStateFlow("0")
    val timeStart: StateFlow<String> = _timeStart

    private val _startTime = MutableStateFlow("")
    val startTime: StateFlow<String> = _startTime

    private val _orderStatus = MutableLiveData<String>("")
    val orderStatus: LiveData<String> = _orderStatus

    private val _uiState = MutableStateFlow<UIState<SingleResponseForOrderThree>>(UIState.Idle)
    val uiState: StateFlow<UIState<SingleResponseForOrderThree>> = _uiState

    private val _uiStateSendOTP = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateSendOTP: StateFlow<UIState<SingleResponse>> = _uiStateSendOTP

    private val _uiStateFinishJob = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateFinishJob: StateFlow<UIState<SingleResponse>> = _uiStateFinishJob

    private val _uiStateGenerateQR = MutableStateFlow<UIState<DynamicSingleResponseWithData<PaymentLinkResponse>>>(UIState.Idle)
    val uiStateGenerateQR: StateFlow<UIState<DynamicSingleResponseWithData<PaymentLinkResponse>>> = _uiStateGenerateQR

    private val _uiStatePaymentStatus = MutableStateFlow<UIState<DynamicSingleResponseWithData<PaymentStatus>>>(UIState.Idle)
    val uiStatePaymentStatus: StateFlow<UIState<DynamicSingleResponseWithData<PaymentStatus>>> = _uiStatePaymentStatus

    private val _uiStateReferal = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateReferal: StateFlow<UIState<SingleResponse>> = _uiStateReferal

    fun startOtpTimer(totalMillis: Long = 30000L) {
        timerJob?.cancel()
        _canResend.value = false

        timerJob = viewModelScope.launch {
            val interval = 1000L
            var remaining = totalMillis

            while (remaining >= 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                _timeLeft.value = String.format("%02d:%02d", minutes, seconds)

                delay(interval)
                remaining -= interval
            }

            _canResend.value = true
            _timeLeft.value = "Resend"
        }
    }

    fun resendOtp() {
        viewModelScope.launch {
            try {
//                repository.sendOtp(phone) // suspend function
                _canResend.value = false
                startOtpTimer()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
    fun resetTimer() {
        timerJob?.cancel()
        _canResend.value = false
        _timeLeft.value = "00:45"
    }

    fun verifyOtp_changeOrderStatus(
        data: HashMap<String, RequestBody>,
        imagePart: MultipartBody.Part
    ) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            delay(800)
            try {
                val response = repository.changeorderStatus(data,imagePart)
                if (response.status==1){
                    _uiState.value = UIState.Success(response)
                }else{
                    _uiState.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }
    fun jobFinishWhenCheque(
        data: HashMap<String, RequestBody>,
        imagePart: MultipartBody.Part
    ) {
        viewModelScope.launch {
            _uiStateFinishJob.value = UIState.Loading
            delay(800)
            try {
                val response = repository.changeorderStatusWhenCheque(data,imagePart)
                if (response.status==1){
                    _uiStateFinishJob.value = UIState.Success(response)
                }else{
                    _uiStateFinishJob.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateFinishJob.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun jobFinish(
        data: HashMap<String, RequestBody>
    ) {
        viewModelScope.launch {
            _uiStateFinishJob.value = UIState.Loading
            delay(800)
            try {
                val response = repository.changeorderStatusJobDone(data)
                if (response.status==1){
                    _uiStateFinishJob.value = UIState.Success(response)
                }else{
                    _uiStateFinishJob.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateFinishJob.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetUIJobFinish(){
        _uiStateFinishJob.value = UIState.Idle
    }

    fun sentOnsiteotp(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiStateSendOTP.value = UIState.Loading
            delay(1000)
            try {
                val response = repository.sendOtp(data)
                if (response.status == 1){
                    _uiStateSendOTP.value = UIState.Success(response)
                }else{
                    _uiStateSendOTP.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateSendOTP.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun checkPaymentStatus(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiStatePaymentStatus.value = UIState.Loading
            delay(1000)
            try {
                val response = repository.chekcPamentStaus(data)
                if (response.status == 1){
                    _uiStatePaymentStatus.value = UIState.Success(response)
                }else{
                    _uiStatePaymentStatus.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStatePaymentStatus.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun generatePaymentQR(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiStateGenerateQR.value = UIState.Loading
            delay(1000)
            try {
                val response = repository.generatePaymentQr(data)
                if (response.status == 1 && response.data !=null){
                    _uiStateGenerateQR.value = UIState.Success(response)
                }else{
                    _uiStateGenerateQR.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateGenerateQR.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }
    fun submitRefferal(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiStateFinishJob.value = UIState.Loading
            delay(800)
            try {
                val response = repository.submitReferral(data)
                if (response.status==1){
                    _uiStateFinishJob.value = UIState.Success(response)
                }else{
                    _uiStateFinishJob.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _uiStateFinishJob.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }
    fun reset_submitRefferal(){
        _uiStateReferal.value = UIState.Idle
    }

    fun resetUIState() {
        _uiState.value = UIState.Idle
    }

    fun resetUIStatePaymentStatus(){
        _uiStatePaymentStatus.value = UIState.Idle
    }

    fun resetUIStateGeneratePaymentQR() {
        _uiStateGenerateQR.value = UIState.Idle
    }

    fun resetUIStatesentOnsiteotp() {
        _uiStateSendOTP.value = UIState.Idle
    }

    fun updateOrderStatus(newStatus: String) {
        _orderStatus.value = newStatus
    }


    fun updateOtp(otp: String) {
        _enteredOtp.value = otp
    }

    fun updateChequeNo(chequeNo :String){
        _chequeno.value = chequeNo
    }

    fun updateQRImage(qrImage :String){
        _qrImageUrl.value = qrImage
    }


    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }



    fun startDurationUpdater(startTime: String) {
        job?.cancel()

        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                _timeStart.value = calculateDuration(startTime) // Update UI every second
                delay(1000) // Wait 1 second before updating again
            }
        }
    }


    fun calculateDuration(startTime: String): String {
        return try {
            // Define possible time formats
            val format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // "2025-03-29 13:18:35"
            val format2 = DateTimeFormatter.ISO_INSTANT // "2025-03-29T13:18:35.000000Z"

            // Parse the server time (handling both formats)
            val serverTime = try {
                LocalDateTime.parse(startTime, format1).atZone(ZoneId.of("UTC"))
            } catch (e: DateTimeParseException) {
                Instant.parse(startTime).atZone(ZoneId.of("UTC"))
            }

            // Get the current local time
            val localZone = ZoneId.systemDefault()
            val currentTime = ZonedDateTime.now(localZone)

            // Convert server time to local timezone
            val localServerTime = serverTime.withZoneSameInstant(localZone)

            // Define the desired format
            val outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")

            // Convert localServerTime to the formatted string
//            _startTime.value = localServerTime.format(outputFormat)

            // Convert localServerTime to the formatted string
//            binding.tvStartTime.text = localServerTime.format(outputFormat)

            // Calculate the duration
            val duration =
                Duration.between(localServerTime.toLocalDateTime(), currentTime.toLocalDateTime())

            // Convert to HH:mm:ss format
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val seconds = duration.seconds % 60

            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } catch (e: DateTimeParseException) {
            "Invalid Time Format"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    fun showStartedTime(startTime: String){
         try {
            // Define possible time formats
            val format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // "2025-03-29 13:18:35"
            val format2 = DateTimeFormatter.ISO_INSTANT // "2025-03-29T13:18:35.000000Z"

            // Parse the server time (handling both formats)
            val serverTime = try {
                LocalDateTime.parse(startTime, format1).atZone(ZoneId.of("UTC"))
            } catch (e: DateTimeParseException) {
                Instant.parse(startTime).atZone(ZoneId.of("UTC"))
            }

            // Get the current local time
            val localZone = ZoneId.systemDefault()
            val currentTime = ZonedDateTime.now(localZone)

            // Convert server time to local timezone
            val localServerTime = serverTime.withZoneSameInstant(localZone)

            // Define the desired format
            val outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")

            // Convert localServerTime to the formatted string
            _startTime.value = localServerTime.format(outputFormat)

        } catch (e: DateTimeParseException) {
            "Invalid Time Format"
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }


}