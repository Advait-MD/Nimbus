package com.nimbus.spatial

import android.graphics.RenderEffect

import android.graphics.Shader

import android.view.View

import android.Manifest

import android.app.PendingIntent

import android.content.Intent

import android.content.pm.PackageManager

import android.location.Location

import android.os.Build

import android.os.Bundle

import android.util.Log

import android.widget.*

import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity

import androidx.core.app.ActivityCompat

import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView

import com.google.android.gms.location.*

import com.google.android.gms.location.LocationServices

import com.google.android.material.slider.Slider

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore

import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        // ✅ This makes enabledGeofenceIds accessible from other classes like GeofenceBroadcastReceiver
        val enabledGeofenceIds = mutableListOf<String>()
    }

    private val TAG = "MainActivity"



    private lateinit var adapter: GeofenceAdapter

    private lateinit var firestore: FirebaseFirestore

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var geofencingClient: GeofencingClient

    private lateinit var geofencePendingIntent: PendingIntent

    private lateinit var locationCallback: LocationCallback



    private var currentFenceId: String? = null

    private var currentLocation: Location? = null

    private val geofenceList = mutableListOf<GeofenceArea>()

    private val PERMISSIONS_CODE = 1001



// UI elements

    private lateinit var btnCreate: Button

    private lateinit var slider: Slider

    private lateinit var tvRadius: TextView



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val rootView = window.decorView.findViewById<View>(android.R.id.content)

                val blur = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)

                rootView.setRenderEffect(blur)

            }



            val intent = Intent(this, LoginActivity::class.java)

            startActivity(intent)

            return // Don't finish(), just let it come back

        }





        setContentView(R.layout.activity_main)



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        val logoutButton = findViewById<Button>(R.id.logoutButton)

        logoutButton.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)

            startActivity(intent)

            finish()

        }

        // Enable Firestore logs (optional)

        FirebaseFirestore.setLoggingEnabled(true)

        firestore = FirebaseFirestore.getInstance()



        requestPermissions()

        initLocationAndGeofence()



        // Now load UI and data

        setupUI()

        loadGeofences()

        loadAndRegisterEnabledGeofences()



        startForegroundService()

    }



    override fun onStart() {

        super.onStart()



        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (user == null) {

            startActivity(Intent(this, LoginActivity::class.java))

            finish()

        }

    }



    override fun onResume() {

        super.onResume()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val rootView = window.decorView.findViewById<View>(android.R.id.content)

            rootView.setRenderEffect(null)

        }

    }





    private fun requestPermissions() {

        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) perms.add(Manifest.permission.FOREGROUND_SERVICE)



        ActivityCompat.requestPermissions(this, perms.toTypedArray(), PERMISSIONS_CODE)

    }



    private fun initLocationAndGeofence() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        geofencingClient = LocationServices.getGeofencingClient(this)



        geofencePendingIntent = PendingIntent.getBroadcast(

            this, 0, Intent(this, GeofenceBroadcastReceiver::class.java),

            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

        )



        startLocationUpdates()

    }



    private fun setupUI() {

        slider = findViewById(R.id.seekbar_radius)

        tvRadius = findViewById(R.id.tv_selected_radius)

        btnCreate = findViewById(R.id.btn_create_geofence)



        slider.addOnChangeListener { _, value, _ ->

            tvRadius.text = "Selected Radius: ${value.toInt()} meters"

        }



        btnCreate.setOnClickListener {

            currentLocation?.let {

                showNameDialog(it.latitude, it.longitude, slider.value)

            } ?: Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()

        }



        adapter = GeofenceAdapter(

            geofenceList,

            onDelete = { deleteGeofence(it) },

            onClick = { geofence ->

                val intent = Intent(this, GeofenceDetailActivity::class.java)

                intent.putExtra("geofence_id", geofence.id)

                startActivity(intent)

            }

        )





        findViewById<RecyclerView>(R.id.recycler_geofences).apply {

            layoutManager = LinearLayoutManager(this@MainActivity)

            adapter = this@MainActivity.adapter

        }

    }



    private fun startForegroundService() {

        val intent = Intent(this, LocationService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

            startForegroundService(intent)

        else

            startService(intent)

    }



    private fun startLocationUpdates() {

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L).build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(res: LocationResult) {

                currentLocation = res.lastLocation

                Log.d(TAG, "Location update: $currentLocation")

            }

        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(req, locationCallback, mainLooper)

        }

    }



    private fun showNameDialog(lat: Double, lng: Double, radius: Float) {

        val input = EditText(this).apply { hint = "Enter area name" }

        AlertDialog.Builder(this)

            .setTitle("Name this Geofence")

            .setView(input)

            .setPositiveButton("Save") { _, _ ->

                val name = input.text.toString().trim()

                if (name.isEmpty()) {

                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()

                } else {

                    val id = UUID.randomUUID().toString()

                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                    val userId = currentUser?.uid



                    if (userId == null) {

                        Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()

                        return@setPositiveButton

                    }



                    val a = GeofenceArea(

                        id = id,

                        name = name,

                        latitude = lat,

                        longitude = lng,

                        radius = radius,

                        isEnabled = true,

                        ownerId = userId // ✅ important!

                    )



                    firestore.collection("geofence_areas")

                        .document(id)

                        .set(a)

                        .addOnSuccessListener {

                            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()

                            geofenceList.add(a)

                            adapter.notifyItemInserted(geofenceList.size - 1)

                            addGeofence(a.id, a.latitude, a.longitude, a.radius)

                        }

                        .addOnFailureListener {

                            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()

                        }



                }

            }

            .setNegativeButton("Cancel", null)

            .show()

    }



    private fun loadGeofences() {

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        val userId = currentUser?.uid



        if (userId == null) {

            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()

            return

        }



        firestore.collection("geofence_areas")

            .whereEqualTo("ownerId", userId) // ✅ Filter by logged-in user

            .get()

            .addOnSuccessListener { result ->

                geofenceList.clear()

                for (doc in result) {

                    val geofence = doc.toObject(GeofenceArea::class.java)

                    geofenceList.add(geofence)

                }

                adapter.notifyDataSetChanged()

            }

            .addOnFailureListener {

                Toast.makeText(this, "Failed to load geofences", Toast.LENGTH_SHORT).show()

            }

    }





    private fun loadAndRegisterEnabledGeofences() {

        firestore.collection("geofence_areas")

            .whereEqualTo("isEnabled", true)

            .get()

            .addOnSuccessListener { snap ->

                snap.forEach { d ->

                    d.toObject(GeofenceArea::class.java).let {

                        addGeofence(it.id, it.latitude, it.longitude, it.radius)

                        Log.d(TAG, "Registered geofence ${it.id}")

                    }

                }

            }

            .addOnFailureListener {

                Log.e(TAG, "Reload failed: ${it.message}")

            }

    }



    private fun addGeofence(id: String, lat: Double, lng: Double, radius: Float) {

        val gf = Geofence.Builder()

            .setRequestId(id).setCircularRegion(lat, lng, radius)

            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

            .build()

        val r = GeofencingRequest.Builder()

            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            .addGeofence(gf).build()



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

            return



        geofencingClient.addGeofences(r, geofencePendingIntent)

            .addOnSuccessListener { Log.d(TAG, "Added fence $id") }

            .addOnFailureListener { Log.e(TAG, "Add fence $id failed: ${it.message}") }

    }



// FIXED: Remove only the specific geofence by ID

    private fun removeSpecificGeofence(id: String) {

        geofencingClient.removeGeofences(listOf(id))

            .addOnSuccessListener {

                Log.d(TAG, "Removed geofence: $id")

            }

            .addOnFailureListener {

                Log.e(TAG, "Remove geofence $id failed: ${it.message}")

            }

    }



// Keep this method for complete removal (used in delete)

    private fun removeGeofence(id: String) {

        geofencingClient.removeGeofences(listOf(id))

            .addOnSuccessListener {

                Log.d(TAG, "Removed geofence: $id")

            }

            .addOnFailureListener {

                Log.e(TAG, "Remove geofence $id failed: ${it.message}")

            }

    }



    private fun deleteGeofence(area: GeofenceArea) {

        firestore.collection("geofence_areas").document(area.id)

            .delete()

            .addOnSuccessListener {

                removeGeofence(area.id)



                // ✅ Remove from the local list and update the UI

                val idx = geofenceList.indexOfFirst { it.id == area.id }

                if (idx != -1) {

                    geofenceList.removeAt(idx)

                    adapter.notifyItemRemoved(idx)

                }



                Toast.makeText(this, "${area.name} deleted", Toast.LENGTH_SHORT).show()

            }

            .addOnFailureListener {

                Log.e("North", "Failed to delete geofence ${area.id}: ${it.message}")

                Toast.makeText(this, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()

            }

    }



    override fun onDestroy() {

        if (::fusedLocationClient.isInitialized) {

            fusedLocationClient.removeLocationUpdates(locationCallback)

        }

        super.onDestroy()

    }

}
