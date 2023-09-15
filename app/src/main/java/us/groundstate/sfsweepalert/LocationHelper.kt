package us.groundstate.sfsweepalert

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.preference.PreferenceManager

object LocationHelper {
    fun saveLocation(sharedPreferences: SharedPreferences, loc: Location) {
        val lat = loc.latitude
        val lng = loc.longitude
        val editor = sharedPreferences.edit()
        editor.putLong("lat", lat.toRawBits())
        editor.putLong("lng", lng.toRawBits())
        editor.apply()
    }

    fun restoreLocation(sharedPreferences: SharedPreferences): Location{
        val defLat = -34.0
        val defLng = 151.0
        val lat = Double.fromBits(sharedPreferences.getLong("lat", defLat.toRawBits()))
        val lng = Double.fromBits(sharedPreferences.getLong("lng", defLng.toRawBits()))
        val restoredLoc = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = lat
            longitude = lng
        }
        return restoredLoc
    }
}