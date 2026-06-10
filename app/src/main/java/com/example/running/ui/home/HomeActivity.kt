package com.example.running.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.databinding.ActivityHomeBinding
import com.example.running.ui.activity.StartActivity
import com.example.running.ui.auth.LoginActivity
import com.example.running.ui.chat.ChatListActivity
import com.example.running.ui.weather.WeatherActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

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
        binding.btnShare.setOnClickListener {
            Toast.makeText(this, "Em breve", Toast.LENGTH_SHORT).show()
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
                FirebaseAuthHelper.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
