package com.example.datasimulator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.datasimulator.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        // Verificar si hay sesión activa
        val sharedPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = sharedPrefs.getLong("current_user_id", -1)
        if (currentUserId != -1L) {
            // Ya hay una sesión activa, ir directamente a MainActivity
            goToMainActivity(currentUserId)
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = databaseHelper.authenticateUser(username, password)
            if (user != null) {
                // Login exitoso
                saveUserSession(user.id)
                goToMainActivity(user.id)
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveUserSession(userId: Long) {
        val sharedPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("current_user_id", userId)
            .apply()
    }

    private fun goToMainActivity(userId: Long) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_id", userId)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }
}