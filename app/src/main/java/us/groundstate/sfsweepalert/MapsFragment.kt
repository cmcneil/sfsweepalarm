package us.groundstate.sfsweepalert

import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import us.groundstate.sfsweepalert.R


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MapsFragment : Fragment() {

    @Inject
    lateinit var locationRepository: LocationRepository

//    private val viewModel: SweepAlertViewModel by viewModels()
//    private lateinit var viewModel: MapsFragmentViewModel
    private var marker: Marker? = null

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        val initLoc: Location? = locationRepository.carLocation.value
        if(initLoc != null) {
            val initLatlng = LatLng(initLoc.latitude, initLoc.longitude)
            marker = googleMap.addMarker(
                MarkerOptions().position(initLatlng).title("Car Location")
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(initLatlng))
        }

        val mapObserver = Observer<Location> { newLoc: Location ->
            val latlng = LatLng(newLoc.latitude, newLoc.longitude)
            marker?.remove()
            marker = googleMap.addMarker(MarkerOptions().position(latlng).title("Car Location"))
            val camPosition = CameraPosition.Builder()
                .zoom(15F)
                .target(latlng)
                .build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition))
        }
        locationRepository.carLocation.observe(this, mapObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        viewModel = ViewModelProvider(this).get(MapsFragmentViewModel::class.java)
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
}