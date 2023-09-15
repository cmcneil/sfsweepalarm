package us.groundstate.sfsweepalert

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SweepAlertViewModel : ViewModel() {
    private val mutableCarLocation = MutableLiveData<Location>()
    val carLocation: LiveData<Location> get() = mutableCarLocation

    fun setLocation(loc: Location) {
        mutableCarLocation.value = loc
    }
}