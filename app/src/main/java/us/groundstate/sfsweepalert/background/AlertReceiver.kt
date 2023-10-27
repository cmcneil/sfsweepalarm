package us.groundstate.sfsweepalert.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.AndroidEntryPoint
import us.groundstate.sfsweepalert.R
import us.groundstate.sfsweepalert.maps.SFGeoClient
import javax.inject.Inject

const val CHANNEL_ID = "100"

@AndroidEntryPoint
class AlertReceiver : BroadcastReceiver() {
    @Inject
    lateinit var parkingRepository: ParkingRepository

    @Inject
    lateinit var geoClient: SFGeoClient

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val doNotifs = sharedPreferences.getBoolean("alarm_on", true)
        if(!doNotifs) {
            return
        }
        val name = "SF Sweep Alert Channel Name"
        val descriptionText = "sweep alert Channel description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager? =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)

        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        var builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SF Street Sweeper Alarm")
            .setContentText("Check your car! You are at risk of street sweeping!")
            .setPriority(NotificationCompat.PRIORITY_MAX)

        notificationManager?.notify(100, builder.build())
    }

    fun getParkingInfo(context: Context) {
        val loc: LatLng = parkingRepository.carLocation.value!!
        geoClient.findClosest(loc, {lrSweep: Pair<DocumentSnapshot?, DocumentSnapshot?> ->
            val leftDoc = lrSweep.first
            val rightDoc = lrSweep.second
            //TODO: Fill out
        })
    }
}