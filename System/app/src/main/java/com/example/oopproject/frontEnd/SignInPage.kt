package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.oopproject.databinding.ActivitySignInPageBinding


class SignInPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignInPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignInPageBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        setContentView(binding.root)


        // Navigate to Sign Up
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpPage::class.java))
            finish()
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.tilEmail.error = "Enter your email first"
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Sign In button click
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Clear previous errors
            binding.tilEmail.error = null
            binding.tilPassword.error = null

            // Validate inputs
            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Show loading
            binding.progressBar.visibility = ProgressBar.VISIBLE
            binding.btnSignIn.isEnabled = false

            // Sign in with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = ProgressBar.GONE
                    binding.btnSignIn.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to Home
                        startActivity(Intent(this, HomePage::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // If user is already signed in, go straight to Home
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
        }
    }
}