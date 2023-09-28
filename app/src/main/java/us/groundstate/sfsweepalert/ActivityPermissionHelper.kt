package us.groundstate.sfsweepalert

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1000
//const val TRANSITIONS_RECEIVER_ACTION =  "com.example.sfsweepalert_transitions_receiver_action"

fun Activity.requestPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACTIVITY_RECOGNITION).not()) {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WAKE_LOCK,
            Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SCHEDULE_EXACT_ALARM),
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION
        )
    } else {
        showRationalDialog(this)
    }
}

fun Activity.isPermissionGranted(): Boolean {

    return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
        Manifest.permission.WAKE_LOCK)
            && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS))
}

private fun showRationalDialog(activity: Activity) {
    AlertDialog.Builder(activity).apply {
        setTitle(R.string.permission_rational_dialog_title)
        setMessage(R.string.permission_rational_dialog_message)
        setPositiveButton(R.string.permission_rational_dialog_positive_button_text) { _, _ ->
            ActivityCompat.requestPermissions(activity, arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_ACTIVITY_RECOGNITION
            )
        }
        setNegativeButton(R.string.permission_rational_dialog_negative_button_text){ dialog, _ ->
            dialog.dismiss()
        }
    }.run {
        create()
        show()
    }
}

private fun startAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}
class ActivityPermissionHelper