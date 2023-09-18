package us.groundstate.sfsweepalert.maps

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import us.groundstate.sfsweepalert.R
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    @ApplicationContext val appContext: Context,
    private val geoClient: SFGeoClient
): ViewModel() {
    private var sweepLines: MutableList<Polyline> = ArrayList()

    fun refreshMapsSweepLines(map: GoogleMap, center: LatLng) {
//        viewModelScope.launch {
        sweepLines.forEach {
            it.remove()
        }
        sweepLines.clear()
        geoClient.addSweepData(center, map) { sortedDocs ->
            val polylines = drawPolyLinesForDocs(sortedDocs, map)
            stylePolyLines(polylines)
            sweepLines.addAll(polylines)
        }
    }

    private fun stylePolyLines(polylines: List<Polyline>) {
        val badRedColor = ContextCompat.getColor(appContext, R.color.sweep_bad_red)
        val goodGreenColor = ContextCompat.getColor(appContext, R.color.sweep_ok_green)
        polylines.forEach {
            val type = it.tag?.toString() ?: ""
            when (type) {
                "BAD" -> {
                    it.color = badRedColor
                }
                "GOOD" -> {
                    it.color = goodGreenColor
                }
            }
            it.endCap = RoundCap()
            it.jointType = JointType.ROUND
        }
    }

    private fun drawPolyLinesForDocs(docsById: Map<String, List<Pair<Int, DocumentSnapshot>>>,
                                     googleMap: GoogleMap): List<Polyline> {
        val polylines: MutableList<Polyline> = ArrayList()
        docsById.forEach { (_, lineSegs) ->
            val doc = lineSegs[0].second
            val coord_start = doc.get("coordinates_start") as List<Double>
            val coord_end = doc.get("coordinates_end") as List<Double>

            val latLngList: MutableList<LatLng> = ArrayList()
            latLngList.add(LatLng(coord_start[0], coord_start[1]))
            latLngList.add(LatLng(coord_end[0], coord_end[1]))
            lineSegs.drop(1).forEach {
                val segDoc = it.second
                val coord_end = segDoc.get("coordinates_end") as List<Double>
                latLngList.add(LatLng(coord_end[0], coord_end[1]))
            }
            val polyline = googleMap.addPolyline(
                PolylineOptions()
                    .clickable(false)
                    .add(*latLngList.toTypedArray())
            )
            polyline.tag = "BAD"
            polylines.add(polyline)

        }
        return polylines
    }
}