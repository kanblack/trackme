package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import android.os.Bundle
import com.google.android.gms.maps.GoogleMap
import kb.dev.trackme.mvvm.views.SessionActivity

interface SessionViewModelState {
    fun onAttachMap(activity: Activity, googleMap: GoogleMap)
    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    )

    fun onSaveMapState(outState: Bundle)

    fun onRestoreMapState(savedInstanceState: Bundle)

    fun onActionButtonClicked()

    fun onResumeButtonClicked()
    fun onActivityResume()
    fun onAttachMapToSave(activity: Activity, googleMap: GoogleMap)
}