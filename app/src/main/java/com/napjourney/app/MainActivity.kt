package com.napjourney.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var map: MapView
    private lateinit var status: TextView
    private lateinit var radiusSpinner: Spinner
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null
    private val radii = listOf(500, 1000, 2000, 5000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        buildUi()
        handleIncomingIntent(intent)
        requestPermissionsIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
        }
        val title = TextView(this).apply {
            text = "Nap-Journey"
            textSize = 26f
            setTextColor(0xFF172B4D.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "Choose a point before your destination. We'll alert you when you're nearby."
            textSize = 14f
        }
        status = TextView(this).apply {
            text = "Tap the map to set your alarm point."
            textSize = 14f
            setPadding(0, 10, 0, 8)
        }
        map = MapView(this).apply {
            setMultiTouchControls(true)
            controller.setZoom(6.5)
            controller.setCenter(GeoPoint(23.8, 90.4))
        }
        val mapParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = 8; bottomMargin = 8 }

        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                val point = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                setPoint(point)
                return true
            }
        })

        val radiusLabel = TextView(this).apply { text = "Alert distance before this point:" }
        radiusSpinner = Spinner(this)
        radiusSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("500 metres", "1 kilometre", "2 kilometres", "5 kilometres")
        )

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val start = Button(this).apply {
            text = "Start Journey"
            setOnClickListener { startJourney() }
        }
        val stop = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, LocationAlarmService::class.java))
                status.text = "Journey monitoring stopped."
            }
        }
        buttons.addView(start, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(stop, LinearLayout.LayoutParams(0, -2, 1f))

        val linkInput = EditText(this).apply {
            hint = "Paste a Google Maps link (optional)"
            textSize = 13f
            maxLines = 2
        }
        val importButton = Button(this).apply {
            text = "Use shared/pasted map link"
            setOnClickListener {
                val parsed = parseCoordinates(linkInput.text.toString())
                if (parsed != null) {
                    setPoint(GeoPoint(parsed.first, parsed.second))
                    map.controller.setZoom(15.0)
                    selectedPoint?.let { map.controller.setCenter(it) }
                } else {
                    Toast.makeText(this@MainActivity,
                        "Couldn't find coordinates in that link. Choose the point on the map instead.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(map, mapParams)
        root.addView(radiusLabel)
        root.addView(radiusSpinner)
        root.addView(buttons)
        root.addView(linkInput)
        root.addView(importButton)
        setContentView(root)
    }

    private fun setPoint(point: GeoPoint) {
        selectedPoint = point
        marker?.let { map.overlays.remove(it) }
        marker = Marker(map).apply {
            position = point
            title = "Nap-Journey alarm point"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)
        map.invalidate()
        status.text = String.format(Locale.US, "Alarm point: %.5f, %.5f", point.latitude, point.longitude)
    }

    private fun startJourney() {
        val point = selectedPoint
        if (point == null) {
            Toast.makeText(this, "Choose your alarm point on the map first.", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded()
            Toast.makeText(this, "Allow location permission, then tap Start Journey again.", Toast.LENGTH_LONG).show()
            return
        }
        val radius = radii[radiusSpinner.selectedItemPosition]
        val serviceIntent = Intent(this, LocationAlarmService::class.java).apply {
            action = LocationAlarmService.ACTION_START
            putExtra(LocationAlarmService.EXTRA_LAT, point.latitude)
            putExtra(LocationAlarmService.EXTRA_LON, point.longitude)
            putExtra(LocationAlarmService.EXTRA_RADIUS, radius)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        status.text = "Journey monitoring is active. Keep location enabled."
        Toast.makeText(this, "Nap-Journey is monitoring your journey.", Toast.LENGTH_LONG).show()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val parsed = parseCoordinates(data.toString())
        if (parsed != null) {
            setPoint(GeoPoint(parsed.first, parsed.second))
            map.controller.setZoom(15.0)
            selectedPoint?.let { map.controller.setCenter(it) }
        } else {
            status.text = "Map link received. If it is a short link, paste coordinates or choose the point on the map."
        }
    }

    private fun parseCoordinates(raw: String): Pair<Double, Double>? {
        val patterns = listOf(
            Regex("@(-?\\d{1,2}\\.\\d+),\\s*(-?\\d{1,3}\\.\\d+)"),
            Regex("[?&](?:q|query|destination)=(-?\\d{1,2}\\.\\d+),\\s*(-?\\d{1,3}\\.\\d+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(Uri.decode(raw)) ?: continue
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0)
                return lat to lon
        }
        return null
    }

    override fun onResume() { super.onResume(); if (::map.isInitialized) map.onResume() }
    override fun onPause() { if (::map.isInitialized) map.onPause(); super.onPause() }
    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent); setIntent(intent); handleIncomingIntent(intent) }
    override fun onDestroy() { if (::map.isInitialized) map.onDetach(); super.onDestroy() }
}
