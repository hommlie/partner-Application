package com.hommlie.partner.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.BankDetails
import com.hommlie.partner.model.ContactDetails
import com.hommlie.partner.model.PersonalDetails
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.UserAboutDetailsData
import com.hommlie.partner.ui.registration.RegistraionRepository
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AboutMeViewModel @Inject constructor(private val repository: RegistraionRepository,private val sharePreference: SharePreference) : ViewModel() {

    private val _isEditable = MutableStateFlow(false)
    val isEditable: StateFlow<Boolean> = _isEditable.asStateFlow()

    fun toggleEditable(b: Boolean) {
        _isEditable.value = b
    }

    private val _personalisEditable = MutableStateFlow(false)
    val personalisEditable: StateFlow<Boolean> = _personalisEditable.asStateFlow()

    private val _getFullDetailsUIState = MutableStateFlow<UIState<UserAboutDetailsData>>(UIState.Idle)
    val getFullDetailsUIState : StateFlow<UIState<UserAboutDetailsData>> = _getFullDetailsUIState

    private val _updateProfilePhotoUIState = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val updateProfilePhotoUIState : StateFlow<UIState<SingleResponse>> = _updateProfilePhotoUIState


    private val _personaldetailsUIState = MutableStateFlow<UIState<PersonalDetails>>(UIState.Idle)
    val personalDetailsUIState : StateFlow<UIState<PersonalDetails>> = _personaldetailsUIState

    private val _contactdetailsUIState = MutableStateFlow<UIState<ContactDetails>>(UIState.Idle)
    val contactDetailsUIState : StateFlow<UIState<ContactDetails>> = _contactdetailsUIState

    private val _bankdetailsUIState = MutableStateFlow<UIState<BankDetails>>(UIState.Idle)
    val bankDetailsUIState : StateFlow<UIState<BankDetails>> = _bankdetailsUIState

    fun togglepersonalEditable(b: Boolean) {
        _personalisEditable.value = b
    }

    private val _bankisEditable = MutableStateFlow(false)
    val bankisEditable: StateFlow<Boolean> = _bankisEditable.asStateFlow()

    fun toggleBankEditable(b: Boolean) {
        _bankisEditable.value = b
    }

    fun saveContactData(data: ContactDetails) {
        viewModelScope.launch {
            viewModelScope.launch {
                _contactdetailsUIState.value = UIState.Loading
                try {
                    val response = repository.saveContactDetails(data.toMap())
//                    _contactdetailsUIState.value = UIState.Success(response.data)
                    response.data?.let { details->
                        _contactdetailsUIState.value = UIState.Success(details)
                    }?: run {
                        _contactdetailsUIState.value = UIState.Error("No data received")
                    }
                } catch (e: Exception) {
                    _contactdetailsUIState.value = UIState.Error(e.message ?: "Something went wrong")
                }
            }
        }
    }

    fun savePersonalData(data: PersonalDetails) {
        viewModelScope.launch {
            _personaldetailsUIState.value = UIState.Loading
            try {

                val response = repository.savePersonalDetails(data.toMap())
//                _personaldetailsUIState.value = UIState.Success(response.data)
                response.data?.let { details ->
                    _personaldetailsUIState.value = UIState.Success(details)
                } ?: run {
                    _personaldetailsUIState.value = UIState.Error("No data received")
                }

            } catch (e: Exception) {
                _personaldetailsUIState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun saveBankData(data: BankDetails) {
        viewModelScope.launch {
            _bankdetailsUIState.value = UIState.Loading
            try {
                val response = repository.saveBankDetails(data.toMap())
//                _bankdetailsUIState.value = UIState.Success(response.data)
                response.data?.let { details ->
                    _bankdetailsUIState.value = UIState.Success(details)
                } ?: run {
                    _bankdetailsUIState.value = UIState.Error("No data received")
                }
            } catch (e: Exception) {
                _bankdetailsUIState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }


    fun resetUIPersonal(){
        _personaldetailsUIState.value = UIState.Idle
    }
    fun resetUIContact(){
        _contactdetailsUIState.value = UIState.Idle
    }
    fun resetUIBank(){
        _bankdetailsUIState.value = UIState.Idle
    }
    fun resetUIfetchAllDetails(){
        _getFullDetailsUIState.value = UIState.Idle
    }
    fun resetUIUpdateProfilePhoto(){
        _updateProfilePhotoUIState.value = UIState.Idle
    }


    fun PersonalDetails.toMap(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["salutation"] = this.salutation
        map["name"] = this.fullName?:""
        map["nationality"] = this.nationality
        map["gender"] = this.gender
        map["marital_status"] = this.maritalStatus
        map["date_of_birth"] = this.dateOfBirth
        map["user_id"] = sharePreference.getString(PrefKeys.userId)
        return map
    }

    fun ContactDetails.toMap(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["personal_email"] = this.personalEmail
        map["alternate_email"] = this.alternateEmail ?: ""
        map["alternate_mobile"] = this.alternateMobile ?: ""
        map["user_id"] = sharePreference.getString(PrefKeys.userId)
        return map
    }


    fun BankDetails.toMap(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["account_type"] = this.accountType
        map["bank_name"] = this.bankName
        map["branch_name"] = this.branchName
        map["account_holder_name"] = this.accountHolderName
        map["account_number"] = this.accountNumber
        map["ifsc_code"] = this.ifscCode
        map["user_id"] = sharePreference.getString(PrefKeys.userId)
        return map
    }



    fun fetchUserFullDetails(hashMap: HashMap<String,String>) {
        viewModelScope.launch {
            _getFullDetailsUIState.value = UIState.Loading
            try {
                val response = repository.getUserAboutDetails(hashMap)
                if (response.status == 1 && response.data != null) {
                    _getFullDetailsUIState.value = UIState.Success(response.data)
                } else {
                    _getFullDetailsUIState.value =
                        UIState.Error(response.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _getFullDetailsUIState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun updateProfilePhoto(user_id: RequestBody, profilePhoto: MultipartBody.Part?) {
        viewModelScope.launch {
            _updateProfilePhotoUIState.value = UIState.Loading
            try {
                val response = repository.updateProfilePhoto(user_id, profilePhoto)
                if (response.status == 1) {
                    _updateProfilePhotoUIState.value = UIState.Success(response)
                } else {
                    _updateProfilePhotoUIState.value =
                        UIState.Error(response.message ?: "Unknown error occurred")
                }
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    413 -> "Image too large. Please select a smaller image." 
                    else -> "Server error (${e.code()}): ${e.message()}"
                }
                _updateProfilePhotoUIState.value = UIState.Error(message)
            } catch (e: IOException) {
                _updateProfilePhotoUIState.value = UIState.Error("Network error. Please try again.")
            } catch (e: Exception) {
                _updateProfilePhotoUIState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }



}