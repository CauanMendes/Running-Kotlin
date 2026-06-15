package com.example.running.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.databinding.ActivityHomeBinding
import com.example.running.helper.MessageNotifier
import com.example.running.helper.NotificationHelper
import com.example.running.ui.activity.StartActivity
import com.example.running.ui.auth.LoginActivity
import com.example.running.ui.chat.ChatListActivity
import com.example.running.ui.history.HistoryActivity
import com.example.running.ui.weather.WeatherActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ok */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val name = FirebaseAuthHelper.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: FirebaseAuthHelper.currentUser?.email
            ?: ""
        binding.tvGreeting.text = getString(R.string.home_greeting, name)

        binding.btnStart.setOnClickListener { goTo(StartActivity::class.java) }
        binding.btnChat.setOnClickListener { goTo(ChatListActivity::class.java) }
        binding.cardWeather.setOnClickListener { goTo(WeatherActivity::class.java) }
        binding.btnShare.setOnClickListener { goTo(HistoryActivity::class.java) }

        ensureNotificationPermission()
        NotificationHelper.ensureChannel(this)
        FirebaseAuthHelper.currentUser?.uid?.let { uid ->
            MessageNotifier.start(applicationContext, uid)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(perm)
            }
        }
    }

    private fun goTo(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                MessageNotifier.stop()
                FirebaseAuthHelper.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
