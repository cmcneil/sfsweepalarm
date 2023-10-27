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

    fun findClosest(
        latLng: LatLng,
        callback: (Pair<DocumentSnapshot?, DocumentSnapshot?>) -> Unit
    )
}

@Singleton
class SFGeoClientImpl: SFGeoClient {
    val db = Firebase.firestore
    val radius = 350.0 // meters
    val closeRadius = 50.0 // meters
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
//                Debug.startMethodTracing("query_returned")
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
//                Debug.stopMethodTracing()
            }
    }

    override fun findClosest(latLng: LatLng,
                             callback: (Pair<DocumentSnapshot?, DocumentSnapshot?>) -> Unit) {
        val center = GeoLocation(latLng.latitude, latLng.longitude)

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, closeRadius)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = db.collection("sweep_segments")
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        val doneTask = Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
//                var minDist: Double = closeRadius
                var closestDist: Double = closeRadius
//                var closestDoc: Map<Int, DocumentSnapshot> = HashMap()
                var closeCNN: String? = null
                var lsideDoc: DocumentSnapshot? = null
                var rsideDoc: DocumentSnapshot? = null
                var closestDocs: Map<String, DocumentSnapshot?> = mapOf(
                    "L" to null,
                    "R" to null
                )
//                val closestDocs = MutableList<DocumentSnapshot> = ArrayList()
//                var lrClosestStreet: Pair<DocumentSnapshot?, DocumentSnapshot?> = Pair(null, null)
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        val lat = doc.getDouble("avg_lat")!!
                        val lng = doc.getDouble("avg_lng")!!
                        val cnn = doc.getString("cnn")!!
                        val leftright = doc.getString("cnnrightleft")!!

                        val avgLoc = GeoLocation(lat, lng)
                        val distanceinM = GeoFireUtils.getDistanceBetween(avgLoc, center)
                        if (distanceinM < closestDist) {
                            closeCNN = cnn
                            closestDist = distanceinM
                        }
                        if (cnn == closeCNN) {
                            if (leftright == "L") {
                                lsideDoc = doc
                            } else if (leftright == "R") {
                                rsideDoc = doc
                            }
                        }
                    }
                }
                callback(Pair(lsideDoc, rsideDoc))
        }
    }
//
//
//        val doneTask = Tasks.whenAllComplete(tasks)
//            .addOnCompleteListener {
//                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
//                for (task in tasks) {
//                    val snap = task.result
//                    for (doc in snap!!.documents) {
//                        val lat = doc.getDouble("avg_lat")!!
//                        val lng = doc.getDouble("avg_lng")!!
//
//                        val avg_loc = GeoLocation(lat, lng)
//                        val distanceinM = GeoFireUtils.getDistanceBetween(avg_loc, center)
//                        if (distanceinM <= radius) {
//                            matchingDocs.add(doc)
//                        }
//                    }
//                }
//                val sortedDocs = sortSegments(matchingDocs)
//                callback(sortedDocs)
//            }
//
//    }

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