package kb.dev.trackme.mvvm.views

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kb.dev.trackme.R
import kb.dev.trackme.databinding.ActivitySessionBinding
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.viewmodel.ext.android.viewModel

@ExperimentalCoroutinesApi
@FlowPreview
class SessionActivity : AppCompatActivity(), OnMapReadyCallback {
    private val viewModel: SessionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            viewModel.onRestoreMapState(savedInstanceState)
        }

        setupBindingData()
        setupMap()
        setupInteraction()
    }

    private fun setupInteraction() {
        findViewById<ImageView>(R.id.imvAction).setOnClickListener {
            viewModel.onActionButtonClicked()
        }

        findViewById<ImageView>(R.id.imvResume).setOnClickListener {
            viewModel.onResumeButtonClicked()
        }
    }

    private fun setupBindingData() {
        val binding: ActivitySessionBinding = DataBindingUtil.setContentView(this, R.layout.activity_session)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
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