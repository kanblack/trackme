package kb.dev.trackme.mvvm.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kb.dev.trackme.R
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class SessionActivity : AppCompatActivity(), OnMapReadyCallback {
    private val viewModel: SessionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            viewModel.onRestoreMapState(savedInstanceState)
        }

        setContentView(R.layout.activity_session)
        setupMap()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.onSaveMapState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let { viewModel.onAttachMap(this, it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        viewModel.onRequestPermissionsResult(this,requestCode, permissions, grantResults)
    }
}