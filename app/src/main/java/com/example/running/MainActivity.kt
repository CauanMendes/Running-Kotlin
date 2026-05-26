package com.example.running

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.ui.auth.LoginActivity
import com.example.running.ui.home.HomeActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val next = if (FirebaseAuthHelper.currentUser != null) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
