package us.groundstate.sfsweepalert.background

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface ParkingRepository {
    val currentActivity: LiveData<String>
    val carLocation: LiveData<LatLng>
    val timeParked: LiveData<Pair<Int, Int>>
    fun setCarLocation(location: LatLng)
    fun setCurrentActivity(activity: String)
    fun setTimeParked(timeParked: Pair<Int, Int>)
}

@Singleton
class ParkingRepositoryImpl: ParkingRepository {
    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val mutableCurrentActivity = MutableLiveData<String>()
    override val currentActivity: LiveData<String> get() = mutableCurrentActivity
    private val mutableCarLocation = MutableLiveData<LatLng>()
    override val carLocation: LiveData<LatLng> get() = mutableCarLocation

    private val mutableTimeParked = MutableLiveData<Pair<Int, Int>>()
    override val timeParked: LiveData<Pair<Int, Int>> get() = mutableTimeParked

    @Inject constructor(@ApplicationContext ctx: Context) {
        appContext = ctx
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        mutableCarLocation.value = LocationHelper.restoreLocation(sharedPreferences)

        val hour = sharedPreferences.getInt("time_parked_hour", 0)
        val minute = sharedPreferences.getInt("time_parked_minute", 0)
        mutableTimeParked.value = Pair(hour, minute)
    }

    override fun setCarLocation(loc: LatLng) {
        mutableCarLocation.postValue(loc)
        LocationHelper.saveLocation(sharedPreferences, loc)
    }

    override fun setCurrentActivity(activity: String) {
        mutableCurrentActivity.postValue(activity)
    }

    override fun setTimeParked(timeParked: Pair<Int, Int>) {
        mutableTimeParked.postValue(timeParked)

        val editor = sharedPreferences.edit()
        editor.putInt("time_parked_hour", timeParked.first)
        editor.putInt("time_parked_minute", timeParked.second)
        editor.apply()
    }
}