package com.example.running.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.running.R
import com.example.running.auth.FirebaseAuthHelper
import com.example.running.dao.UserDao
import com.example.running.databinding.ActivityRegisterBinding
import com.example.running.ui.home.HomeActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvHaveAccount.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etPasswordConfirm.text?.toString().orEmpty()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            toast(R.string.err_fill_fields); return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(R.string.err_invalid_email); return
        }
        if (password.length < 6) {
            toast(R.string.err_short_password); return
        }
        if (password != confirm) {
            toast(R.string.err_password_mismatch); return
        }

        setLoading(true)
        lifecycleScope.launch {
            val firebaseUser = try {
                FirebaseAuthHelper.signUp(email, password, name)
            } catch (e: Exception) {
                setLoading(false)
                toast(getString(R.string.err_register_failed, e.message ?: ""))
                return@launch
            }

            runCatching {
                UserDao.ensureUser(firebaseUser, displayName = name)
            }.onFailure { e ->
                Log.e(TAG, "Falha ao criar doc do usuário (auth OK)", e)
            }

            toast(R.string.msg_register_success)
            startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
            finishAffinity()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun toast(resId: Int) =
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "RegisterActivity"
    }
}
