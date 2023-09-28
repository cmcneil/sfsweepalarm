package us.groundstate.sfsweepalert.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint

const val MAX_UPDATE_AGE_LOC_REQUEST = 1000L * 30

@AndroidEntryPoint
class TransitionsReceiver: BroadcastReceiver() {
//    @Inject
//    lateinit var workManager: WorkManager
//    private lateinit var fusedLocationClient: FusedLocationProviderClient

//    @Inject
//    lateinit var parkingRepository: ParkingRepository

//    private fun activityTypeToString(type: Int): String = when (type) {
//        DetectedActivity.STILL -> "STILL"
//        DetectedActivity.WALKING -> "WALKING"
//        DetectedActivity.RUNNING -> "RUNNING"
//        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
//        else -> throw IllegalArgumentException("activity $type not supported")
//    }
//
//    private fun setNotificationAlarm(context: Context) {
//        val am: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//
//        val targetCal = Calendar.getInstance()
//        val hour = sharedPreferences.getString("notification_hour", "22")
//        val minute = sharedPreferences.getString("notification_minute", "0")
//        targetCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour))
//        targetCal.set(Calendar.MINUTE, Integer.parseInt(minute))
//        val currentCal = Calendar.getInstance()
//        if (currentCal > targetCal) {
//            targetCal.add(Calendar.DATE, 1)
//        }
//
//
//        val intent = Intent(context, AlertReceiver::class.java)
//        val pi = PendingIntent.getBroadcast(context, 0, intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//        am!!.cancel(pi)
//        am.set(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pi)
//    }

//    private fun updateLocation(context: Context) {
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Toast.makeText(
//                context, "Location Permission failure!",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//        val locRequest = CurrentLocationRequest.Builder()
//            .setGranularity(Granularity.GRANULARITY_FINE)
//            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//            .setMaxUpdateAgeMillis(MAX_UPDATE_AGE_LOC_REQUEST)
//            .build()
//
//        fusedLocationClient.getCurrentLocation(locRequest, null)
//            .addOnSuccessListener { location: Location? ->
//                if (location != null) {
//                    val latlng: LatLng = LatLng(location.latitude, location.longitude)
//                    parkingRepository.setCarLocation(latlng)
//                } else {
//                    Toast.makeText(
//                        context, "Location was null!",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//    }

//    private fun updateTimeLocationSet(context: Context) {
//        val currentDateTime= LocalDateTime.now()
//        val hour = currentDateTime.hour
//        val minute = currentDateTime.minute
//        parkingRepository.setTimeParked(Pair(hour, minute))
//    }


    override fun onReceive(context: Context, intent: Intent) {
        val workManager = WorkManager.getInstance(context)
        Log.d("TransitionReceiver", "Broadcast received with intent: $intent")
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            Log.d("TransitionReceiver", "Broadcast received with result: " + result.toString())
            result?.let { it.transitionEvents
                .forEach {
//                    parkingRepository.setCurrentActivity(activityTypeToString(it.activityType))
//                    Log.d("TransitionReceiver", "Activity transition: "
//                            + activityTypeToString(it.activityType))
                    if (it.activityType == DetectedActivity.IN_VEHICLE
                        && it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
//                        updateLocation(context)
//                        updateTimeLocationSet(context)
//                        setNotificationAlarm(context)
                        val updateLocWorkRequest: WorkRequest =
                            OneTimeWorkRequestBuilder<TransitionsWorker>()
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .setInputData(workDataOf(
                                    "ACTIVITY_TYPE" to it.activityType
                                ))
                                .build()
                        workManager.enqueue(updateLocWorkRequest)
                    }
//                    setNotificationAlarm(context)
                }
            }
        }
    }
}