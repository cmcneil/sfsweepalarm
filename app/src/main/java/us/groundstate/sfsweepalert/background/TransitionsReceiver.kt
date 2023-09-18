package us.groundstate.sfsweepalert.background

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.util.Calendar
import javax.inject.Inject

const val MAX_UPDATE_AGE_LOC_REQUEST = 1000L * 30

@AndroidEntryPoint
class TransitionsReceiver: BroadcastReceiver() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var locationRepository: LocationRepository

    private fun activityTypeToString(type: Int): String = when (type) {
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.RUNNING -> "RUNNING"
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        else -> throw IllegalArgumentException("activity $type not supported")
    }

    private fun setNotificationAlarm(context: Context) {
        val am: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val targetCal = Calendar.getInstance()
        val hour = sharedPreferences.getString("notification_hour", "22")
        val minute = sharedPreferences.getString("notification_minute", "0")
        targetCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour))
        targetCal.set(Calendar.MINUTE, Integer.parseInt(minute))
        val currentCal = Calendar.getInstance()
        if (currentCal > targetCal) {
            targetCal.add(Calendar.DATE, 1)
        }


        val intent = Intent(context, AlertReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am!!.cancel(pi)
        am.set(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pi)
    }

    private fun updateLocation(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context, "Location Permission failure!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val locRequest = CurrentLocationRequest.Builder()
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_UPDATE_AGE_LOC_REQUEST)
            .build()

        fusedLocationClient.getCurrentLocation(locRequest, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    locationRepository.setCarLocation(location)
//                    locationRepository.saveLocation(context, location)
                } else {
                    Toast.makeText(
                        context, "Location was null!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateTimeLocationSet(context: Context) {
        val currentDateTime= LocalDateTime.now()
        val hour = currentDateTime.hour
        val minute = currentDateTime.minute
        locationRepository.setTimeParked(Pair(hour, minute))
    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TransitionReceiver", "Broadcast received with intent: $intent")
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            Log.d("TransitionReceiver", "Broadcast received with result: " + result.toString())
            result?.let { it.transitionEvents
                .forEach {
                    locationRepository.setCurrentActivity(activityTypeToString(it.activityType))
                    Log.d("TransitionReceiver", "Activity transition: "
                            + activityTypeToString(it.activityType))
                    if (it.activityType == DetectedActivity.IN_VEHICLE
                        && it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                        updateLocation(context)
                        updateTimeLocationSet(context)
                        setNotificationAlarm(context)
                    }
//                    setNotificationAlarm(context)
                }
            }
        }
    }
}