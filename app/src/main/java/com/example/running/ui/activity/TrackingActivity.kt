package com.example.running.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.widget.Chronometer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.ActivityDao
import com.example.running.databinding.ActivityTrackingBinding
import com.example.running.helper.MotionState
import com.example.running.helper.SensorHelper
import com.example.running.model.FitActivity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
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

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableMyLocation()
            else Toast.makeText(this, R.string.permission_location_denied, Toast.LENGTH_SHORT).show()
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

        setupMap()

        binding.btnPrimary.setOnClickListener {
            when {
                !running -> startActivity()
                paused -> resume()
                else -> pause()
            }
        }
        binding.btnFinish.setOnClickListener { finishActivity() }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(16.0)
        binding.mapView.controller.setCenter(GeoPoint(-21.7849, -48.1813)) // Araraquara default

        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?) = false
        })
        binding.mapView.overlays.add(tapOverlay)

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

    private fun startActivity() {
        running = true
        paused = false
        startTimeMs = SystemClock.elapsedRealtime()
        accumulatedMs = 0L
        binding.chronometer.base = startTimeMs
        binding.chronometer.start()
        binding.btnPrimary.setText(R.string.btn_pause)
        binding.btnFinish.isEnabled = true
        startSensors()
    }

    private fun pause() {
        paused = true
        accumulatedMs += SystemClock.elapsedRealtime() - startTimeMs
        binding.chronometer.stop()
        binding.btnPrimary.setText(R.string.btn_resume)
        sensors.stop()
    }

    private fun resume() {
        paused = false
        startTimeMs = SystemClock.elapsedRealtime()
        binding.chronometer.base = startTimeMs - accumulatedMs
        binding.chronometer.start()
        binding.btnPrimary.setText(R.string.btn_pause)
        startSensors()
    }

    private fun finishActivity() {
        if (!paused) {
            accumulatedMs += SystemClock.elapsedRealtime() - startTimeMs
        }
        binding.chronometer.stop()
        sensors.stop()
        running = false
        paused = false

        val durationSec = accumulatedMs / 1000L
        val avgAccel = if (motionSamples > 0) magnitudeSum / motionSamples else 0.0
        val kcal = estimateKcal(activityType, durationSec)

        val uid = FirebaseAuthHelper.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Sessão expirada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val record = FitActivity(
            type = activityType,
            startedAt = System.currentTimeMillis() - accumulatedMs,
            durationSec = durationSec,
            kcal = kcal,
            detectedType = detectedState.name.lowercase(),
            avgAccel = avgAccel
        )

        lifecycleScope.launch {
            runCatching { ActivityDao.save(uid, record) }
                .onSuccess {
                    Toast.makeText(this@TrackingActivity, R.string.tracking_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { e ->
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
            onRotation = { /* unused for now */ }
        )
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
        if (!running) sensors.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensors.stop()
        locationOverlay?.disableMyLocation()
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val TYPE_RUN = "run"
        const val TYPE_WALK = "walk"
        const val TYPE_BIKE = "bike"
    }
}
