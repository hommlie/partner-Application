package com.hommlie.partner.ui.registration

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.RegistrationRequest
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.WorkZones
import com.hommlie.partner.model.WorkZonesData
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.utils.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(private val repository: RegistraionRepository) : ViewModel() {


    val genderList: LiveData<List<String>> = MutableLiveData(
        listOf("Male", "Female", "Other")
    )

    private val _selectedGender = MutableLiveData<String>()
    val selectedGender: LiveData<String> get() = _selectedGender

    fun setSelectedGender(gender: String) {
        _selectedGender.value = gender
    }

    private val _selectedWorkZoneId = MutableLiveData<Int>(null)
    val selectedWorkZoneId: LiveData<Int> get() = _selectedWorkZoneId

    fun setSelectedWorkZoneId(id: Int) {
        _selectedWorkZoneId.value = id
    }

    private val _workZones = MutableStateFlow<UIState<DynamicSingleResponseWithData<List<WorkZonesData>>>>(UIState.Idle)
    val workZones : StateFlow<UIState<DynamicSingleResponseWithData<List<WorkZonesData>>>> = _workZones

    fun getWorkZones(){
        viewModelScope.launch {
            _workZones.value = UIState.Loading
            delay(1500)
            try {
                val response = repository.getWorkZones()
                if (response.status == 1){
                    _workZones.value = UIState.Success(response)
                }else{
                    _workZones.value = UIState.Error(response.message ?:"Something went wrong")
                }
            }catch (e : Exception){
                _workZones.value = UIState.Error(e.message?:"Something went wrong")
            }
        }
    }

    fun resetGetWorkZones(){
        _workZones.value = UIState.Idle
    }

    private val _registerState = MutableLiveData<UIState<SingleResponse>>()
    val registerState: LiveData<UIState<SingleResponse>> = _registerState

    fun registerUser(
        userId : String,
        name: String,
        dob: String,
        age: Int,
        gender: String,
        email: String,
        workZone: Int,
        expInYear: Float,
        profilePhoto: MultipartBody.Part?,
        document: MultipartBody.Part?
    ) {
        if (name.isBlank() || email.isBlank() || gender.isBlank()) {
            _registerState.value = UIState.Error("Required fields are missing")
            return
        }

        viewModelScope.launch {
            _registerState.value = UIState.Loading
            try {
                val response = repository.registerUser(userId,
                    name, dob, age, gender, email, workZone, expInYear, profilePhoto, document
                )
                if (response.status==1){
                    _registerState.value = UIState.Success(response)
                }else{
                    _registerState.value = UIState.Error(response.message ?: "Something went wrong")
                }

            } catch (e: Exception) {
                _registerState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetRegisterUserUi(){
        _registerState.value = UIState.Idle
    }




    private val _firstNameError = MutableLiveData<String?>()
    val firstNameError: LiveData<String?> = _firstNameError

    private val _lastNameError = MutableLiveData<String?>()
    val lastNameError: LiveData<String?> = _lastNameError

    private val _dobError = MutableLiveData<String?>()
    val dobError: LiveData<String?> = _dobError

    private val _ageError = MutableLiveData<String?>()
    val ageError: LiveData<String?> = _ageError

    private val _genderError = MutableLiveData<String?>()
    val genderError: LiveData<String?> = _genderError

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _workZoneError = MutableLiveData<String?>()
    val workZoneError: LiveData<String?> = _workZoneError

    private val _expError = MutableLiveData<String?>()
    val expError: LiveData<String?> = _expError

    fun validateFirstName(firstName: String) {
        _firstNameError.value = if (firstName.isBlank()) "First name required" else null
    }

    fun validateLastName(lastName: String) {
        _lastNameError.value = if (lastName.isBlank()) "Last name required" else null
    }

    fun validateDob(dob: String) {
        _dobError.value = if (dob.isBlank()) "Date of birth required" else null
    }

    fun validateAge(age: String) {
        _ageError.value = if (age.isBlank()) "Age required"
        else if (age.toIntOrNull() == null) "Enter valid age"
        else null
    }

    fun validateGender(gender: String) {
        _genderError.value = if (gender.isBlank()) "Gender required" else null
    }

    fun validateEmail(email: String) {
        _emailError.value = when {
            email.isBlank() -> "Email required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email"
            else -> null
        }
    }

    fun validateWorkZone(workZone: String) {
        _workZoneError.value = if (workZone.isBlank()) "Work zone required" else null
    }

    fun validateExp(exp: String) {
        _expError.value = if (exp.isBlank()) "Experience required"
        else if (exp.toFloatOrNull() == null) "Enter valid experience"
        else null
    }

    // Final check before API call
    fun validateAllFields(
        firstName: String,
        lastName: String,
        dob: String,
        age: String,
        gender: String,
        email: String,
        workZone: String,
        exp: String
    ): Boolean {
        validateFirstName(firstName)
        validateLastName(lastName)
        validateDob(dob)
        validateAge(age)
        validateGender(gender)
        validateEmail(email)
        validateWorkZone(workZone)
        validateExp(exp)

        return listOf(
            _firstNameError.value,
            _lastNameError.value,
            _dobError.value,
            _ageError.value,
            _genderError.value,
            _emailError.value,
            _workZoneError.value,
            _expError.value
        ).all { it == null }
    }


    private val _termsError = MutableLiveData<String?>()
    val termsError: LiveData<String?> = _termsError

    fun validateTerms(isChecked: Boolean) {
        _termsError.value = if (!isChecked) "Accept all terms & conditions." else null
    }



}