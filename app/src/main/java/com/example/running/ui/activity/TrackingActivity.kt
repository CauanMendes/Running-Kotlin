package com.example.running.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.ActivityDao
import com.example.running.databinding.ActivityTrackingBinding
import com.example.running.helper.MotionState
import com.example.running.util.Base64Converter
import com.example.running.helper.SensorHelper
import com.example.running.model.FitActivity
import com.example.running.model.TrackPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private lateinit var sensors: SensorHelper
    private lateinit var activityType: String

    private var running = false
    private var paused = false
    private var startTimeMs = 0L
    private var accumulatedMs = 0L
    private var motionSamples = 0
    private var magnitudeSum = 0.0
    private var detectedState = MotionState.IDLE
    private var locationOverlay: MyLocationNewOverlay? = null

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val trackPoints = mutableListOf<TrackPoint>()
    private var lastTrackedLocation: Location? = null
    private var distanceMeters: Double = 0.0
    private val polyline = Polyline().apply {
        outlinePaint.strokeWidth = 10f
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableMyLocation()
            else Toast.makeText(this, R.string.permission_location_denied, Toast.LENGTH_SHORT).show()
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) finishAndSave(uri) else finishAndSave(null)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        activityType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_RUN
        sensors = SensorHelper(this)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        polyline.outlinePaint.color = ContextCompat.getColor(this, R.color.primary)

        setupMap()

        binding.btnPrimary.setOnClickListener {
            when {
                !running -> startTracking()
                paused -> resumeTracking()
                else -> pauseTracking()
            }
        }
        binding.btnFinish.setOnClickListener { showFinishDialog() }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(17.0)
        binding.mapView.controller.setCenter(GeoPoint(-21.7849, -48.1813))
        binding.mapView.overlays.add(polyline)

        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        overlay.enableMyLocation()
        overlay.enableFollowLocation()
        binding.mapView.overlays.add(overlay)
        locationOverlay = overlay
        binding.mapView.invalidate()
    }

    private fun startTracking() {
        running = true
        paused = false
        startTimeMs = SystemClock.elapsedRealtime()
        accumulatedMs = 0L
        binding.chronometer.base = startTimeMs
        binding.chronometer.start()
        binding.btnPrimary.setText(R.string.btn_pause)
        binding.btnFinish.isEnabled = true
        startSensors()
        startLocationUpdates()
    }

    private fun pauseTracking() {
        paused = true
        accumulatedMs += SystemClock.elapsedRealtime() - startTimeMs
        binding.chronometer.stop()
        binding.btnPrimary.setText(R.string.btn_resume)
        sensors.stop()
        stopLocationUpdates()
    }

    private fun resumeTracking() {
        paused = false
        startTimeMs = SystemClock.elapsedRealtime()
        binding.chronometer.base = startTimeMs - accumulatedMs
        binding.chronometer.start()
        binding.btnPrimary.setText(R.string.btn_pause)
        startSensors()
        startLocationUpdates()
    }

    private fun showFinishDialog() {
        if (!paused) {
            accumulatedMs += SystemClock.elapsedRealtime() - startTimeMs
            paused = true
        }
        binding.chronometer.stop()
        sensors.stop()
        stopLocationUpdates()

        AlertDialog.Builder(this)
            .setTitle(R.string.finish_dialog_title)
            .setMessage(R.string.finish_dialog_msg)
            .setPositiveButton(R.string.btn_attach_photo) { _, _ ->
                pickImageLauncher.launch("image/*")
            }
            .setNegativeButton(R.string.btn_finish_no_photo) { _, _ ->
                finishAndSave(null)
            }
            .setCancelable(false)
            .show()
    }

    private fun finishAndSave(photoUri: Uri?) {
        running = false
        val uid = FirebaseAuthHelper.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Sessão expirada", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        val durationSec = accumulatedMs / 1000L
        val avgAccel = if (motionSamples > 0) magnitudeSum / motionSamples else 0.0
        val kcal = estimateKcal(activityType, durationSec)

        binding.btnPrimary.isEnabled = false
        binding.btnFinish.isEnabled = false

        lifecycleScope.launch {
            val imageBase64 = if (photoUri != null) {
                Toast.makeText(
                    this@TrackingActivity, R.string.uploading_image, Toast.LENGTH_SHORT
                ).show()
                runCatching {
                    Base64Converter.uriToString(this@TrackingActivity, photoUri)
                }.getOrElse {
                    Log.e(TAG, "Falha ao comprimir imagem", it)
                    ""
                }
            } else ""

            val record = FitActivity(
                type = activityType,
                startedAt = System.currentTimeMillis() - accumulatedMs,
                durationSec = durationSec,
                kcal = kcal,
                detectedType = detectedState.name.lowercase(),
                avgAccel = avgAccel,
                distanceMeters = distanceMeters,
                trajectory = trackPoints.toList(),
                imageBase64 = imageBase64
            )

            runCatching { ActivityDao.save(uid, record) }
                .onSuccess {
                    Toast.makeText(
                        this@TrackingActivity, R.string.tracking_saved, Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                .onFailure { e ->
                    binding.btnPrimary.isEnabled = true
                    binding.btnFinish.isEnabled = true
                    Toast.makeText(
                        this@TrackingActivity,
                        getString(R.string.tracking_save_failed, e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun startSensors() {
        sensors.startMotion(
            onMagnitude = { magnitude ->
                motionSamples++
                magnitudeSum += magnitude
                detectedState = SensorHelper.classifyMotion(magnitude)
                updateDetectedLabel()
                updateKcal()
            },
            onRotation = { }
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(3f)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) onNewLocation(loc)
            }
        }
        locationCallback = callback
        runCatching {
            fusedClient.requestLocationUpdates(request, callback, mainLooper)
        }.onFailure { Log.e(TAG, "requestLocationUpdates falhou", it) }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun onNewLocation(loc: Location) {
        val point = TrackPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
        trackPoints.add(point)

        lastTrackedLocation?.let { prev ->
            distanceMeters += prev.distanceTo(loc)
        }
        lastTrackedLocation = loc

        polyline.addPoint(GeoPoint(loc.latitude, loc.longitude))
        binding.mapView.invalidate()
        updateDistanceLabel()
    }

    private fun updateDistanceLabel() {
        val tv = binding.tvDistance
        tv.text = if (distanceMeters >= 1000) {
            "%.2f %s".format(distanceMeters / 1000.0, getString(R.string.unit_km))
        } else {
            "${distanceMeters.roundToInt()} ${getString(R.string.unit_meters)}"
        }
    }

    private fun updateDetectedLabel() {
        val resId = when (detectedState) {
            MotionState.IDLE -> R.string.detected_idle
            MotionState.WALKING -> R.string.detected_walking
            MotionState.RUNNING -> R.string.detected_running
            MotionState.CYCLING -> R.string.detected_cycling
        }
        binding.tvDetected.setText(resId)
    }

    private fun updateKcal() {
        val elapsedSec = if (paused) {
            accumulatedMs / 1000L
        } else {
            (accumulatedMs + (SystemClock.elapsedRealtime() - startTimeMs)) / 1000L
        }
        val kcal = estimateKcal(activityType, elapsedSec)
        binding.tvKcal.text = "${kcal.roundToInt()} kcal"
    }

    private fun estimateKcal(type: String, durationSec: Long): Double {
        val met = when (type) {
            TYPE_RUN -> 9.8
            TYPE_BIKE -> 7.5
            else -> 3.8
        }
        val weightKg = 70.0
        val hours = durationSec / 3600.0
        return met * weightKg * hours
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        if (!running) {
            sensors.stop()
            stopLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensors.stop()
        stopLocationUpdates()
        locationOverlay?.disableMyLocation()
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val TYPE_RUN = "run"
        const val TYPE_WALK = "walk"
        const val TYPE_BIKE = "bike"
        private const val TAG = "TrackingActivity"
    }
}
