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
import com.example.running.ui.auth.LoginActivity

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

        binding.btnStart.setOnClickListener { todo() }
        binding.btnShare.setOnClickListener { todo() }
        binding.btnChat.setOnClickListener { todo() }
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

    private fun todo() {
        Toast.makeText(this, "Em breve", Toast.LENGTH_SHORT).show()
    }
}
