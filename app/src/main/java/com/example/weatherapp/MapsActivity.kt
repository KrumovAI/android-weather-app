package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import android.widget.SearchView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var searchBox: SearchView
    private lateinit var currentLocationButton: Button
    private lateinit var submitButton: Button


    private var chosenLocation: LatLng? = null
    private var currentLocation: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentMarker: Marker? = null

    private val REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        this.searchBox = this.findViewById((R.id.search_box))
        this.currentLocationButton = this.findViewById(R.id.current_location_button)
        this.submitButton = this.findViewById(R.id.submit_button)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        this.fetchLastLocation()

        searchBox.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val locationName: String = searchBox.query.toString()
                var addressList: List<Address>? = null

                if (!locationName.isBlank()) {
                    val geocoder: Geocoder = Geocoder(this@MapsActivity)

                    try {
                        addressList = geocoder.getFromLocationName(locationName, 1)
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }

                    if (addressList != null && addressList.count() > 0) {
                        val address: Address = addressList.get(0)
                        val latLng: LatLng = LatLng(address.latitude, address.longitude)

                        setNewLocation(latLng, locationName)
                    }
                }

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        this.currentLocationButton.setOnClickListener {
            this.fetchLastLocation()
        }

        this.submitButton.setOnClickListener {
            val intent: Intent = Intent(this, WeatherActivity::class.java).apply {
                putExtra("latitude", chosenLocation?.latitude)
                putExtra("longitude", chosenLocation?.longitude)
            }

            this.startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        val location = this.currentLocation

        // Add a marker in Sydney and move the camera
        if (location != null) {
            this.setNewLocation(location, "Current Location")
        }
    }

    private fun setNewLocation(location: LatLng, label: String) {
        this.chosenLocation = location

        val markerOptions: MarkerOptions =
            MarkerOptions().position((location)).title(label)

        this.currentMarker?.remove()

        this.currentMarker = googleMap.addMarker(markerOptions)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10.0f))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == this.REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.fetchLastLocation()
            }
        }
    }

    private fun fetchLastLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                Array<String>(1) { Manifest.permission.ACCESS_FINE_LOCATION },
                this.REQUEST_CODE)

            return
        }

        val task = this.fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener {
            if (it != null) {
                this.currentLocation = LatLng(it.latitude, it.longitude)

                val supportMapFragment: SupportMapFragment = this.supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment

                supportMapFragment.getMapAsync(this)
            }
        }
    }
}