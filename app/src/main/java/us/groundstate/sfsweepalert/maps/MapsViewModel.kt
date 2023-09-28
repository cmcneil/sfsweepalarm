package us.groundstate.sfsweepalert.maps

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import us.groundstate.sfsweepalert.R
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.sqrt

fun Polyline.addInfoWindow(map: GoogleMap, title: String, message: String): Marker? {
    val pointsOnLine = this.points.size
    val infoLatLng = this.points[(pointsOnLine / 2)]
    val invisibleMarker =
        BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    val marker = map.addMarker(
        MarkerOptions()
            .position(infoLatLng)
            .title(title)
            .snippet(message)
            .alpha(0f)
            .icon(invisibleMarker)
            .anchor(0f, 0f)
    )
    marker?.showInfoWindow()
    return marker
}

const val LAT_DELTA = 0.000009
const val LNG_DELTA = 0.00001

@HiltViewModel
class MapsViewModel @Inject constructor(
    @ApplicationContext val appContext: Context,
    private val geoClient: SFGeoClient
): ViewModel() {
    private var sweepLines: MutableMap<String, Pair<Polyline, DocumentSnapshot>> = HashMap()
    private var infoMarker: Marker? = null

    // Approximately 1-2 meter shifts.
    private val directionShifts: Map<String, LatLng> = mapOf(
        "North" to LatLng(LAT_DELTA, 0.0),
        "South" to LatLng(-LAT_DELTA, 0.0),
        "East" to LatLng(0.0, LNG_DELTA),
        "West" to LatLng(0.0, -LNG_DELTA),
        "NorthWest" to LatLng(LAT_DELTA / sqrt(2.0), -LNG_DELTA / sqrt(2.0)),
        "NorthEast" to LatLng(LAT_DELTA / sqrt(2.0), LNG_DELTA / sqrt(2.0)),
        "SouthWest" to LatLng(-LAT_DELTA / sqrt(2.0), -LNG_DELTA / sqrt(2.0)),
        "SouthEast" to LatLng(-LAT_DELTA / sqrt(2.0), LNG_DELTA / sqrt(2.0)),
    ).withDefault {_ -> LatLng(0.0, 0.0)}

    fun refreshMapsSweepLines(map: GoogleMap, center: LatLng) {
        sweepLines.forEach {
            it.value.first.remove()
        }
        sweepLines.clear()
        geoClient.addSweepData(center, map) { sortedDocs ->
            val polylines = drawPolyLinesForDocs(sortedDocs, map)
            stylePolyLines(polylines)
            addPolyLineClickable(polylines, map)
        }
    }

    fun addPolyLineClickable(polylines: List<Polyline>, googleMap: GoogleMap) {
        for (polyline in polylines) {
            polyline.isClickable = true
            googleMap.setOnPolylineClickListener { poly ->
                infoMarker?.remove()
                val title = sweepLines[poly.id]!!.second.get("fullname") as String
                val toHour = sweepLines[poly.id]!!.second.get("tohour") as String
                val fromHour = sweepLines[poly.id]!!.second.get("fromhour") as String
                val infoMarker = poly.addInfoWindow(googleMap, title,
                    "$fromHour:00 to $toHour:00")
            }
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
            val doc: DocumentSnapshot = lineSegs[0].second
            val blockside = doc.get("blockside")
            var shift: LatLng = LatLng(0.0, 0.0)
            if (blockside != null) {
                shift = directionShifts[blockside as String]!!
            }
            val sla = shift.latitude
            val slo = shift.longitude
            val coord_start = doc.get("coordinates_start") as List<Double>
            val coord_end = doc.get("coordinates_end") as List<Double>

            val latLngList: MutableList<LatLng> = ArrayList()
            latLngList.add(LatLng(coord_start[0] + sla, coord_start[1] + slo))
            latLngList.add(LatLng(coord_end[0] + sla, coord_end[1] + slo))
            lineSegs.drop(1).forEach {
                val segDoc = it.second
                val coord_end = segDoc.get("coordinates_end") as List<Double>
                latLngList.add(LatLng(coord_end[0] + sla, coord_end[1] + slo))
            }
            val polyline = googleMap.addPolyline(
                PolylineOptions()
                    .clickable(false)
                    .add(*latLngList.toTypedArray())
            )
            if (isSweepTomorrow(doc)) {
                polyline.tag = "BAD"
            } else {
                polyline.tag = "GOOD"
            }

            polylines.add(polyline)
            sweepLines[polyline.id] = Pair(polyline, doc)

        }
        return polylines
    }

    private fun isSweepTomorrow(segDoc: DocumentSnapshot): Boolean {
        val now: LocalDate = LocalDate.now()
        val tomorrow = now.plusDays(1)
        val weekdayCode = (segDoc.get("weekday") as String).uppercase().take(3)
        val tomorrowCode = tomorrow.dayOfWeek.name.uppercase().take(3)
        val tomorrowWeek = tomorrow.dayOfMonth / 7
        val weekSweep = Integer.parseInt(segDoc.get("week$tomorrowWeek") as String)
        if (weekdayCode == tomorrowCode
            && weekSweep == 1) {
            return true
        }
        return false
    }
}