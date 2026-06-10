package com.example.running.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.running.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.cardRun.setOnClickListener { open(TrackingActivity.TYPE_RUN) }
        binding.cardWalk.setOnClickListener { open(TrackingActivity.TYPE_WALK) }
        binding.cardBike.setOnClickListener { open(TrackingActivity.TYPE_BIKE) }
    }

    private fun open(type: String) {
        startActivity(
            Intent(this, TrackingActivity::class.java)
                .putExtra(TrackingActivity.EXTRA_TYPE, type)
        )
    }
}
