package us.groundstate.sfsweepalert

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    val currentActivity: LiveData<String>
    val carLocation: LiveData<Location>
    val timeParked: LiveData<Pair<Int, Int>>
    fun setCarLocation(location: Location)
    fun setCurrentActivity(activity: String)
    fun setTimeParked(timeParked: Pair<Int, Int>)
}

@Singleton
class LocationRepositoryImpl: LocationRepository{
    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val mutableCurrentActivity = MutableLiveData<String>()
    override val currentActivity: LiveData<String> get() = mutableCurrentActivity
    private val mutableCarLocation = MutableLiveData<Location>()
    override val carLocation: LiveData<Location> get() = mutableCarLocation

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

    override fun setCarLocation(loc: Location) {
        mutableCarLocation.value = loc
        LocationHelper.saveLocation(sharedPreferences, loc)
    }

    override fun setCurrentActivity(activity: String) {
        mutableCurrentActivity.value = activity
    }

    override fun setTimeParked(timeParked: Pair<Int, Int>) {
        mutableTimeParked.value = timeParked

        val editor = sharedPreferences.edit()
        editor.putInt("time_parked_hour", timeParked.first)
        editor.putInt("time_parked_minute", timeParked.second)
        editor.apply()
    }
}