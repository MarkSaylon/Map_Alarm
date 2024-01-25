package com.example.mapalarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Location
import android.os.Build
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.round

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var mMap: GoogleMap? = null
    lateinit var mapView: MapView
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    private val pinnedLocations = mutableListOf<PinnedLocation>()
    var alarmDistance: Float = 0.0f // Initialize with a default value
    lateinit var editTextDist: EditText
    lateinit var buttonSetAlarm: Button

    override fun onMapReady(googleMap: GoogleMap) {
        mapView.onResume()
        mMap = googleMap

        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )!= PackageManager.PERMISSION_GRANTED
        ){
            return
        }
        mMap!!.setMyLocationEnabled(true)

        mMap!!.setOnMapClickListener { latLng ->
            // Add a marker on map click
            mMap!!.clear() // Clear previous markers
            val marker = mMap!!.addMarker(MarkerOptions().position(latLng).title("Pinned Location"))

            // Store the location data
            val pinnedLocation = PinnedLocation(latLng.latitude, latLng.longitude)
            pinnedLocations.add(pinnedLocation)
            val distance = calculateDistanceToPinnedLocation(pinnedLocation)
            updateDistanceTextView(distance)
            // You can now use 'pinnedLocation' to store or manipulate the location data
            if (distance >= 0 && distance < alarmDistance) {
                // Trigger the notification or alarm
                triggerNotificationOrAlarm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextDist = findViewById(R.id.dist)
        buttonSetAlarm = findViewById(R.id.setAlarm)
        mapView = findViewById<MapView>(R.id.mapView)

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null){
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }

        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
        requestLocationPerms()

        buttonSetAlarm.setOnClickListener {
            val inputDistanceText = editTextDist?.text?.toString()
            val inputDistance = inputDistanceText?.toFloatOrNull()

            if (inputDistance != null) {
                // Save the distance entered by the user
                alarmDistance = inputDistance
                Toast.makeText(this, "Alarm distance set to $alarmDistance meters", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid distance input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Denied")
            .setMessage("This app requires location permission to show your location on the map.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun requestLocationPerms() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, enable My Location layer
            enableMyLocation()
        }
    }


    private fun enableMyLocation() {
        if (mMap != null) {
            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Enable My Location layer
                mMap!!.isMyLocationEnabled = true
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable My Location layer
                enableMyLocation()
            } else {
                // Permission denied, show a message or handle accordingly
                showPermissionDeniedDialog()
            }
        }
    }

    data class PinnedLocation(val latitude: Double, val longitude: Double)

    private fun calculateDistanceToPinnedLocation(pinnedLocation: PinnedLocation): Float {
        // Ensure mMap is not null before accessing myLocation
        if (mMap != null) {
            val myLocation = mMap!!.myLocation
            if (myLocation != null) {
                val currentLocation = Location("currentLocation")
                currentLocation.latitude = myLocation.latitude
                currentLocation.longitude = myLocation.longitude

                val pinnedLocationObj = Location("pinnedLocation")
                pinnedLocationObj.latitude = pinnedLocation.latitude
                pinnedLocationObj.longitude = pinnedLocation.longitude

                val distance = currentLocation.distanceTo(pinnedLocationObj)
                // The 'distance' variable now contains the distance in meters
                return round(distance)
            }
        }

        // Return a default distance if current location is not available
        return -1f
    }

    private fun updateDistanceTextView(distance: Float) {
        val textViewDistance = findViewById<TextView>(R.id.distansya)
        if (textViewDistance != null) {
            if (distance >= 0) {
                textViewDistance.text = "Distance: $distance meters"
            } else {
                textViewDistance.text = "Distance: N/A"
            }
        }
    }

    private fun triggerNotificationOrAlarm() {
        // Example: Show a notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "AlarmChannel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gising Julian")
            .setContentText("Bobo, malapit kana lumagpas!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1, notificationBuilder.build())

        // You can also play a loud sound here using MediaPlayer or other methods
    }

}