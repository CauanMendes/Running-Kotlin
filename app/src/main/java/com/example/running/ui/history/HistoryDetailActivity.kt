package com.example.running.ui.history

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.ActivityDao
import com.example.running.databinding.ActivityHistoryDetailBinding
import com.example.running.model.FitActivity
import com.example.running.util.Base64Converter
import com.example.running.util.Formatters
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class HistoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        val activityId = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }
        val uid = FirebaseAuthHelper.currentUser?.uid ?: run { finish(); return }

        lifecycleScope.launch {
            val activity = runCatching { ActivityDao.get(uid, activityId) }.getOrNull()
            if (activity == null) {
                finish(); return@launch
            }
            render(activity)
        }
    }

    private fun render(activity: FitActivity) {
        binding.tvType.text = Formatters.labelForType(activity.type)
        binding.tvDate.text = Formatters.formatDateTime(activity.startedAt)
        binding.tvDuration.text = Formatters.formatDuration(activity.durationSec)
        binding.tvDistance.text = if (activity.distanceMeters > 0) {
            Formatters.formatDistance(activity.distanceMeters)
        } else "—"
        binding.tvKcal.text = "${activity.kcal.toInt()} kcal"

        renderRoute(activity)
        renderPhoto(activity)
    }

    private fun renderRoute(activity: FitActivity) {
        if (activity.trajectory.isEmpty()) {
            binding.mapView.visibility = View.GONE
            binding.tvNoRoute.visibility = View.VISIBLE
            return
        }

        val points = activity.trajectory.map { GeoPoint(it.lat, it.lng) }

        val polyline = Polyline().apply {
            outlinePaint.color = ContextCompat.getColor(this@HistoryDetailActivity, R.color.primary)
            outlinePaint.strokeWidth = 10f
            setPoints(points)
        }
        binding.mapView.overlays.add(polyline)

        Marker(binding.mapView).apply {
            position = points.first()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Início"
            binding.mapView.overlays.add(this)
        }
        Marker(binding.mapView).apply {
            position = points.last()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Fim"
            binding.mapView.overlays.add(this)
        }

        binding.mapView.post {
            if (points.size == 1) {
                binding.mapView.controller.setZoom(17.0)
                binding.mapView.controller.setCenter(points.first())
            } else {
                val box = BoundingBox.fromGeoPointsSafe(points)
                binding.mapView.zoomToBoundingBox(box, false, 80)
            }
        }
    }

    private fun renderPhoto(activity: FitActivity) {
        if (activity.imageBase64.isBlank()) {
            binding.cardPhoto.visibility = View.GONE
            return
        }
        runCatching { Base64Converter.stringToBitmap(activity.imageBase64) }
            .onSuccess { binding.ivPhoto.setImageBitmap(it) }
            .onFailure { binding.cardPhoto.visibility = View.GONE }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    companion object {
        const val EXTRA_ID = "activity_id"
    }
}
