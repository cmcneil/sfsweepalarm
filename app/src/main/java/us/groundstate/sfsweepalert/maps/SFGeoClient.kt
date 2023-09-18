package us.groundstate.sfsweepalert.maps

import android.util.Log
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

interface SFGeoClient {
    fun addSweepData(latlng: LatLng, googleMap: GoogleMap,
                     callback: (Map<String, List<Pair<Int, DocumentSnapshot>>>) -> Unit)
}

@Singleton
class SFGeoClientImpl: SFGeoClient {
    val db = Firebase.firestore
    val radius = 500.0 // meters
    @Inject
    constructor()

    override fun addSweepData(latLng: LatLng, googleMap: GoogleMap,
                              callback: (Map<String, List<Pair<Int, DocumentSnapshot>>>) -> Unit) {
        val center = GeoLocation(latLng.latitude, latLng.longitude)

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = db.collection("sweep_segments")
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }
        Log.d("SFGeoClient", "Querying bounds: " + bounds[0].startHash.toString() + ", " + bounds[0].endHash.toString())


        val doneTask = Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    Log.d("SFGeoClient", "Returned task " + snap.toString())
                    for (doc in snap!!.documents) {
                        val lat = doc.getDouble("avg_lat")!!
                        val lng = doc.getDouble("avg_lng")!!

                        val avg_loc = GeoLocation(lat, lng)
                        val distanceinM = GeoFireUtils.getDistanceBetween(avg_loc, center)
                        if (distanceinM <= radius) {
                            matchingDocs.add(doc)
                        }
                    }
                }
                val sortedDocs = sortSegments(matchingDocs)
                callback(sortedDocs)
            }
    }

    fun sortSegments(docs: MutableList<DocumentSnapshot>)
        : MutableMap<String, MutableList<Pair<Int, DocumentSnapshot>>> {
        val docsByCode: MutableMap<String, MutableList<Pair<Int, DocumentSnapshot>>> = HashMap()
        for (doc in docs) {
            val line_id = (doc.get("line_id") as Long).toString()
            val seg_str = doc.get("seg_id") as String
            val seg_id = seg_str.split(":")[1].toInt()

            if (!docsByCode.containsKey(line_id)) {
                docsByCode[line_id] = ArrayList()
            }
            docsByCode[line_id]!!.add(Pair(seg_id, doc))
        }
        docsByCode.forEach { (_, v) -> v.sortBy { it.first } }
        return docsByCode
    }
}