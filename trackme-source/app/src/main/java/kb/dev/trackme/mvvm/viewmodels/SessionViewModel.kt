package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import kb.dev.trackme.SessionState
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.repositories.SessionRepository
import kb.dev.trackme.utils.getVelocity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlin.math.max

@ExperimentalCoroutinesApi
@FlowPreview
class SessionViewModel(
    private val repository: SessionRepository,
    private val mapManager: MapManager
) :
    ViewModel(), SessionViewModelState {
    val sessionState = MutableLiveData(SessionState.ACTIVE)
    val distance = MutableLiveData(0.0)//metter
    val duration = MutableLiveData(0.0)//millisecond
    var velocityCount = 0
    var totalVelocity = 0.0
    val avgSpeed = MutableLiveData(0.0)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            sessionState.asFlow().debounce(1000).collectLatest { state: SessionState? ->
                if (state == SessionState.COMPLETE) {
                    saveSession()
                    releaseCurrentSession()
                } else if(state == SessionState.ACTIVE){
                    startTimer()
                }
            }
        }

        viewModelScope.launch {
            duration.asFlow().collect {
                updateVelocity()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            startTimer()
        }
    }


    private fun releaseCurrentSession() {
    }

    private fun updateVelocity() {
        val duration = duration.value ?: 0.0
        val distance = distance.value ?: 0.0
        totalVelocity += getVelocity(duration, distance)
        velocityCount++
        updateAvgSpeed()
    }

    private fun updateAvgSpeed() {
        val currentAvgSpeed = totalVelocity / velocityCount
        avgSpeed.postValue(currentAvgSpeed)
    }

    private suspend fun startTimer() {
        val timerIntervalInMills = 500L
        if (sessionState.value == SessionState.ACTIVE) {
            val updateDuration = (duration.value ?: 0.0) + timerIntervalInMills
            duration.postValue(updateDuration)
            updateVelocity()
            delay(timerIntervalInMills)
            startTimer()
        }
    }

    private fun saveSession() {
        val distance = distance.value ?: 0.0
        val duration = duration.value ?: 0.0
        val avgVelocity = totalVelocity / max(velocityCount,1)
        repository.saveNewSession(distance, duration, avgVelocity)
    }

    override fun onAttachMap(activity: Activity, googleMap: GoogleMap) {
        mapManager.attachMap(activity, googleMap)
    }

    override fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mapManager.updateDatePermissionResult(activity, requestCode, permissions, grantResults)
    }

    override fun onSaveMapState(outState: Bundle) {
        mapManager.saveMapState(outState)

    }

    override fun onRestoreMapState(savedInstanceState: Bundle) {
        mapManager.restoreMapState(savedInstanceState)
    }

    override fun onActionButtonClicked() {
        val updateState = when (sessionState.value) {
            SessionState.ACTIVE -> SessionState.PAUSE
            SessionState.PAUSE -> SessionState.COMPLETE
            else -> SessionState.COMPLETE
        }
        updateSessionState(updateState)
    }

    override fun onResumeButtonClicked() {
        updateSessionState(SessionState.ACTIVE)
    }

    private fun updateSessionState(currentState: SessionState?) {
        sessionState.postValue(
            currentState
        )
    }
}