package com.hommlie.partner.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repository: JobsRepository
) : ViewModel() {

    private val _jobsUiState = MutableStateFlow<UIState<List<NewOrderData>>>(UIState.Idle)
    val jobsUIState : StateFlow<UIState<List<NewOrderData>>> = _jobsUiState

    /*fun getNewJobs(map : HashMap<String,String>){
        viewModelScope.launch {
            _jobsUiState.value = UIState.Loading

            try {
                val response = repository.getNewJobs(map)

                if (response.status == 1 && response.data!=null){
                    _jobsUiState.value = UIState.Success(response)
                }else{
                    _jobsUiState.value = UIState.Error(response.message ?: "An error occurred")
                }

            }catch (e : Exception){
                _jobsUiState.value = UIState.Error(e.localizedMessage ?: "An error occurred")
            }
        }

    } */
//    fun resetGetNewJobs(){ _jobsUiState.value = UIState.Idle }

    fun loadPendingJobs(
        pendingMap: HashMap<String, String>,
        yesterdayMap: HashMap<String, String>,
        todayMap: HashMap<String, String>
    ) {
        viewModelScope.launch {

            loadJobsInternal(

                listOf(
                    pendingMap,
                    yesterdayMap,
                    todayMap
                )

            )

        }
    }

    fun loadAllJobs(
        pendingMap: HashMap<String, String>,
        yesterdayMap: HashMap<String, String>,
        todayMap: HashMap<String, String>,
        completedMap: HashMap<String, String>,
        incompletedMap: HashMap<String, String>
    ) {

        viewModelScope.launch {

            loadJobsInternal(

                listOf(

                    pendingMap,
                    yesterdayMap,
                    todayMap,
                    completedMap,
                    incompletedMap

                )

            )

        }
    }

    fun loadCompletedJobs(
        completedMap: HashMap<String, String>
    ) {
        viewModelScope.launch {
            loadJobsInternal(
                listOf(completedMap)
            )

        }
    }

   /* private suspend fun loadJobsInternal(
        requests: List<HashMap<String, String>>
    ) {
        _jobsUiState.value = UIState.Loading

        supervisorScope {
            val responses = requests.map { request ->
                async {
                    runCatching {
                        repository.getNewJobs(request)
                    }
                }
            }.awaitAll()

            val apiResponses = responses.mapNotNull { it.getOrNull() }

            // Session Expired
            apiResponses.firstOrNull {
                it.message.equals("User Not Found", true) ||
                        it.message.equals("Employee Not Found", true)
            }?.let {

                _jobsUiState.value = UIState.Error(it.message)
                return@supervisorScope
            }

            val successResponses = apiResponses.filter { it.status == 1 }

            if (successResponses.isEmpty()) {

                val errorMessage = apiResponses
                    .firstOrNull { it.status == 0 }
                    ?.message
                    ?: "Unable to load jobs"

                _jobsUiState.value = UIState.Error(errorMessage)
                return@supervisorScope
            }
            val finalList = successResponses
                .flatMap { it.data.orEmpty() }
                .distinctBy { it.orderId }

            _jobsUiState.value = UIState.Success(finalList)
        }
    } */

    private suspend fun loadJobsInternal(
        requests: List<HashMap<String, String>>
    ) {
        _jobsUiState.value = UIState.Loading

        supervisorScope {

            val responses = requests.map { request ->
                async {
                    try {
                        repository.getNewJobs(request)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ApiResult.UnknownError(
                            message = e.localizedMessage ?: "Unexpected error"
                        )
                    }
                }
            }.awaitAll()

            // 1. Session Expired has highest priority
            responses
                .filterIsInstance<ApiResult.Error>()
                .firstOrNull {
                    it.message.equals("User Not Found", true) ||
                            it.message.equals("Employee Not Found", true)
                }
                ?.let {
                    _jobsUiState.value = UIState.Error(it.message)
                    return@supervisorScope
                }

            // 2. Collect all successful responses
            val successResponses = responses
                .filterIsInstance<ApiResult.Success<NewOrder>>()

            // 3. If at least one API succeeded, show merged data
            if (successResponses.isNotEmpty()) {

                val finalList = successResponses
                    .flatMap { it.data.data.orEmpty() }
                    .distinctBy { it.orderId }

                _jobsUiState.value = UIState.Success(finalList)
                return@supervisorScope
            }

            // 4. No success -> decide best error to show

            // Prefer server/business error over generic network error
            responses
                .filterIsInstance<ApiResult.Error>()
                .firstOrNull()
                ?.let {
                    _jobsUiState.value = UIState.Error(it.message)
                    return@supervisorScope
                }

            // Then network error
            if (responses.any { it is ApiResult.NetworkError }) {
                _jobsUiState.value = UIState.Error("No internet connection")
                return@supervisorScope
            }

            // Then unknown error
            responses
                .filterIsInstance<ApiResult.UnknownError>()
                .firstOrNull()
                ?.let {
                    _jobsUiState.value = UIState.Error(it.message)
                    return@supervisorScope
                }

            // Fallback
            _jobsUiState.value = UIState.Error("Unable to load jobs")
        }
    }


    private val _hasOrdersUiState = MutableStateFlow<UIState<Boolean>>(UIState.Loading)
    val hasOrdersUiState: StateFlow<UIState<Boolean>> = _hasOrdersUiState

    fun checkOrders(map: HashMap<String, String>) {
        viewModelScope.launch {
            _hasOrdersUiState.value = UIState.Loading

            when (val result = repository.getNewJobs(map)) {
                is ApiResult.Success -> {

                    val response = result.data

                    if (response.status == 1 && !response.data.isNullOrEmpty()) {
                        _hasOrdersUiState.value = UIState.Success(true)
                    } else {
                        _hasOrdersUiState.value = UIState.Success(false)
                    }
                }

                is ApiResult.Error -> {
                    _hasOrdersUiState.value = UIState.Error(result.message)
                }

                is ApiResult.NetworkError -> {
                    _hasOrdersUiState.value = UIState.Error("No internet connection")
                }

                is ApiResult.UnknownError -> {
                    _hasOrdersUiState.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun resetCheckOrder(){
        _hasOrdersUiState.value = UIState.Idle
    }



}