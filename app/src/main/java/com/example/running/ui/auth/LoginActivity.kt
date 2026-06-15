package com.example.running.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.auth.BiometricHelper
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.UserDao
import com.example.running.databinding.ActivityLoginBinding
import com.example.running.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometricButton()

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnBiometric.setOnClickListener { attemptBiometricLogin() }
    }

    private fun setupBiometricButton() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hasSavedUser = prefs.getString(KEY_LAST_EMAIL, null) != null
        val biometricReady = BiometricHelper.canAuthenticate(this)
        binding.btnBiometric.visibility =
            if (hasSavedUser && biometricReady) View.VISIBLE else View.GONE
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            toast(R.string.err_fill_fields); return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(R.string.err_invalid_email); return
        }

        setLoading(true)
        lifecycleScope.launch {
            runCatching { FirebaseAuthHelper.signIn(email, password) }
                .onSuccess { user ->
                    saveLastEmail(email)
                    runCatching { UserDao.ensureUser(user) }
                        .onFailure { e -> Log.e(TAG, "ensureUser falhou", e) }
                    goHome()
                }
                .onFailure { e ->
                    setLoading(false)
                    toast(getString(R.string.err_login_failed, e.message ?: ""))
                }
        }
    }

    private fun attemptBiometricLogin() {
        BiometricHelper.prompt(
            activity = this,
            title = getString(R.string.biometric_title),
            subtitle = getString(R.string.biometric_subtitle),
            onSuccess = {
                if (FirebaseAuthHelper.currentUser != null) {
                    goHome()
                } else {
                    toast(R.string.err_no_saved_user)
                }
            },
            onError = { msg -> toast(msg) }
        )
    }

    private fun saveLastEmail(email: String) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_EMAIL, email)
            .apply()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun toast(resId: Int) =
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val PREFS = "running_prefs"
        const val KEY_LAST_EMAIL = "last_email"
        private const val TAG = "LoginActivity"
    }
}
