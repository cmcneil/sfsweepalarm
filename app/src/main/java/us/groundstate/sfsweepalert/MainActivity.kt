package us.groundstate.sfsweepalert


import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import us.groundstate.sfsweepalert.databinding.ActivityMainBinding
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import us.groundstate.sfsweepalert.background.LocationRepository
import us.groundstate.sfsweepalert.background.TransitionsReceiver
import javax.inject.Inject

private const val TRANSITION_PENDING_INTENT_REQUEST_CODE = 200
const val TRANSITIONS_RECEIVER_ACTION = "us.groundstate.sfsweepalert_transitions_receiver_action"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var activityTransitionList = mutableListOf<ActivityTransition>()
    private var isTrackingStarted = false

    @Inject
    lateinit var locationRepository: LocationRepository

    init {
        // List of activity transitions to track.
        activityTransitionList += ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        activityTransitionList += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
        activityTransitionList += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
        activityTransitionList += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
        activityTransitionList += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
            sendFakeActivityTransitionEvent()
        }

        if(isPermissionGranted()) {
            Toast.makeText(this@MainActivity, "Activity Tracking is enabled!",
                Toast.LENGTH_SHORT).show()
            requestActivityTransitionUpdates(activityTransitionList)
            isTrackingStarted = true
        } else {
            requestPermission()
        }

        val currentActivityTV = findViewById<TextView>(R.id.current_activity_status)
        currentActivityTV.text = (getString(R.string.current_activity_preamble))
        val timeParkedObserver = Observer<Pair<Int, Int>> {timeParked ->
            currentActivityTV.text = (getString(R.string.current_activity_preamble)
                    + timeParked.first.toString() + ":" + timeParked.second.toString())
        }
        locationRepository.timeParked.observe(this, timeParkedObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.action_mapsFragment_to_settingsFragment)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION
            && permissions.contains(Manifest.permission.ACTIVITY_RECOGNITION)
            && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("permission_result", "permission granted")
            requestActivityTransitionUpdates(activityTransitionList)
            isTrackingStarted = true
        } else {
            Log.d("NO_PERMISSION", "everything is fucked")
        }
    }

    private fun sendFakeActivityTransitionEvent() {
        // name your intended recipient class
        val intent = Intent(this, TransitionsReceiver::class.java)
        val events: ArrayList<ActivityTransitionEvent> = arrayListOf()

        // create fake events
        events.add(
            ActivityTransitionEvent(
                DetectedActivity.IN_VEHICLE,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
                SystemClock.elapsedRealtimeNanos()
            )
        )

        // finally, serialize and send
        val result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(
            result,
            intent,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )
        sendBroadcast(intent)
        Log.d("FakeBroadcast", "Fake broadcast sent!" + intent.toString())
    }

    private fun requestActivityTransitionUpdates(activityTransitionList: List<ActivityTransition>) {
        val intent = Intent(this, TransitionsReceiver::class.java)
        val transitionsPendingIntent =
            PendingIntent.getBroadcast(this@MainActivity,
                0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val request = ActivityTransitionRequest(activityTransitionList)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
            return
        } else {
            val task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(
                request, transitionsPendingIntent)
            task.run {
                addOnSuccessListener {
                    Log.d("TransitionUpdate", getString(R.string.transition_update_request_success))
                }
                addOnFailureListener {
                    Log.d("TransitionUpdate", getString(R.string.transition_update_request_failed))
                }
            }
        }
    }
}