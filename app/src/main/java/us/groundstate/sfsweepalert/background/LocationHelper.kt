package us.groundstate.sfsweepalert.background

import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng

object LocationHelper {
    fun saveLocation(sharedPreferences: SharedPreferences, loc: LatLng) {
        val lat = loc.latitude
        val lng = loc.longitude
        val editor = sharedPreferences.edit()
        editor.putLong("lat", lat.toRawBits())
        editor.putLong("lng", lng.toRawBits())
        editor.apply()
    }

    fun restoreLocation(sharedPreferences: SharedPreferences): LatLng {
        val defLat = -34.0
        val defLng = 151.0
        val lat = Double.fromBits(sharedPreferences.getLong("lat", defLat.toRawBits()))
        val lng = Double.fromBits(sharedPreferences.getLong("lng", defLng.toRawBits()))
        return LatLng(lat, lng)
    }
}