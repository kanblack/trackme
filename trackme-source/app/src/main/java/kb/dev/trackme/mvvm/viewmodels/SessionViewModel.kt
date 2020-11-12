package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import kb.dev.trackme.SessionState
import kb.dev.trackme.SingleLiveEvent
import kb.dev.trackme.database.Session
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.mvvm.views.SessionActivity
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
    val distance = mapManager.getDistance()
    val duration = MutableLiveData(0.0)//millisecond
    val avgSpeed = MutableLiveData(0.0)
    private val saveSessionCompleteEvent = SingleLiveEvent<Boolean>()
    private val isSavingSession = MutableLiveData(false)
    fun getSaveSessionCompleteEvent(): SingleLiveEvent<Boolean> {
        return saveSessionCompleteEvent
    }
    private var velocityCount = 0
    private var totalVelocity = 0.0
    private var isTimerRunning = false

    init {
        viewModelScope.launch(Dispatchers.Default) {
            sessionState.asFlow().collect { state: SessionState? ->
                when (state) {
                    SessionState.COMPLETE -> {
                        isSavingSession.postValue(true)
                        saveSession()
                        releaseCurrentSession()
                        isSavingSession.postValue(false)
                    }
                    SessionState.ACTIVE -> {
                        requestLocationUpdate()
                        startTimer()
                    }
                    SessionState.PAUSE -> {
                        requestStopLocationUpdate()
                    }
                }
            }
        }
    }

    private fun requestStopLocationUpdate() {
        mapManager.stopUpdateLocation()
    }

    private fun requestLocationUpdate() {
        viewModelScope.launch(Dispatchers.Main) {
            mapManager.requestLocationUpdate()
        }
    }


    private fun releaseCurrentSession() {
        saveSessionCompleteEvent.postValue(true)
    }

    private fun updateVelocity() {
        val duration = duration.value ?: 0.0
        val distance = distance.value ?: 0.0
        totalVelocity += getVelocity(duration, distance)
        if (totalVelocity > 0) {
            velocityCount++
        }
        updateAvgSpeed()
    }

    private fun updateAvgSpeed() {

        val currentAvgSpeed = totalVelocity / if(velocityCount == 0) 1 else velocityCount
        avgSpeed.postValue(currentAvgSpeed)
    }

    private suspend fun startTimer() {
        if (isTimerRunning) {
            return
        }
        isTimerRunning = true
        val timerIntervalInMills = 500L
        while (sessionState.value == SessionState.ACTIVE) {
            val updateDuration = (duration.value ?: 0.0) + timerIntervalInMills
            duration.postValue(updateDuration)
            updateVelocity()
            delay(timerIntervalInMills)
        }
        isTimerRunning = false
    }

    private suspend fun saveSession() {
        val distance = distance.value ?: 0.0
        val duration = duration.value ?: 0.0
        val avgVelocity = totalVelocity / max(velocityCount, 1)
            repository.saveNewSession(
                Session(
                    avgSpeed = avgVelocity,
                    createdAt = System.currentTimeMillis(),
                    distance = distance.toLong(),
                    duration = duration.toLong(),
                    route = mapManager.getRoute()
                )
            )
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
            else -> SessionState.PAUSE
        }
        updateSessionState(updateState)
    }

    override fun onResumeButtonClicked() {
        if(sessionState.value != SessionState.ACTIVE) {
            updateSessionState(SessionState.ACTIVE)
        }
    }

    private fun updateSessionState(currentState: SessionState?) {
        sessionState.postValue(
            currentState
        )
    }

    override fun onActivityResume() {
        mapManager.requestLocationUpdate()
    }

    override fun onAttachMapToSave(activity: Activity, googleMap: GoogleMap) {
        mapManager.attachMapToSave(activity, googleMap)
    }
}