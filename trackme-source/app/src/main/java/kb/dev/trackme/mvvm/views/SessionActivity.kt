package kb.dev.trackme.mvvm.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.SupportMapFragment
import kb.dev.trackme.*
import kb.dev.trackme.databinding.ActivitySessionBinding
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import kb.dev.trackme.services.LocationUpdatesService
import kb.dev.trackme.utils.SharePreferenceUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


@ExperimentalCoroutinesApi
@FlowPreview
class SessionActivity : AppCompatActivity() {
    private val viewModel: SessionViewModel by viewModel()
    private val sharedPreferences: SharePreferenceUtils by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBindingData()
    }

    override fun onStart() {
        super.onStart()
        getLocationPermission()
        setupMap()
        setupInteraction()
    }

    private fun setupBindingData() {
        val binding: ActivitySessionBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_session)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sharedPreferences.saveGrantPermissionStatus(true)
            startUpdateLocationService()
        } else {
            sharedPreferences.saveGrantPermissionStatus(false)
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            googleMap?.let { viewModel.onAttachMap(this, it) }
        }

        val mapFragmentToSave = supportFragmentManager
            .findFragmentById(R.id.mapToSave) as? SupportMapFragment
        mapFragmentToSave?.getMapAsync { googleMap ->
            googleMap?.let { viewModel.onAttachMapToSave(this, it) }
        }
    }

    private fun setupInteraction() {
        findViewById<ImageView>(R.id.imvAction).setOnClickListener {
            viewModel.onActionButtonClicked()
            checkPauseLocationUpdate()
        }

        findViewById<ImageView>(R.id.imvResume).setOnClickListener {
            viewModel.onResumeButtonClicked()
            requestLocationUpdateService(EXTRA_REQUEST_RESUME_SESSION)
        }

        viewModel.getSaveSessionCompleteEvent().observe(this, {
            requestLocationUpdateService(EXTRA_REQUEST_COMPLETE_SESSION)
            startActivity(Intent(this, SessionsHistoryActivity::class.java))
            finish()
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocationPermission()
                }
            }
        }
    }


    private fun startUpdateLocationService() {
        val lastSessionState = sharedPreferences.getLastSessionSate()
        val extra =
            if ((lastSessionState == null) || (lastSessionState == SessionState.COMPLETE.toString())) {
                EXTRA_REQUEST_START_SESSION
            } else null

        requestLocationUpdateService(extra)
    }

    private fun checkPauseLocationUpdate() {
        val lastSessionState = sharedPreferences.getLastSessionSate()
        if (lastSessionState == SessionState.ACTIVE.toString()) {
            requestLocationUpdateService(EXTRA_REQUEST_PAUSE_SESSION)
        }
    }

    private fun requestLocationUpdateService(extra: String?) {
        Intent(this, LocationUpdatesService::class.java).also { intent ->
            extra?.let {
                intent.putExtra("type", extra)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }
}