package kb.dev.trackme.mvvm.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.SupportMapFragment
import kb.dev.trackme.R
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class SessionActivity : AppCompatActivity() {
    private val viewModel: SessionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)
        setupMap()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(viewModel)
    }
}