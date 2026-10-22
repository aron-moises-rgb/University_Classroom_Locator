package com.example.oopproject.frontEnd

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.oopproject.databinding.ActivitySignInPageBinding
import com.example.oopproject.databinding.DialogForgotBinding
import com.google.firebase.auth.FirebaseAuth

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

        // Forgot Password dialog popup
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
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

    private fun showForgotPasswordDialog() {
        val dialogBinding = DialogForgotBinding.inflate(layoutInflater)

        // Pre-fill email if user already typed it on the sign-in screen
        val prefilledEmail = binding.etEmail.text.toString().trim()
        if (prefilledEmail.isNotEmpty()) {
            dialogBinding.etResetEmail.setText(prefilledEmail)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Set transparent window background for card corner rounding
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Send Reset Email button
        dialogBinding.btnSendReset.setOnClickListener {
            val email = dialogBinding.etResetEmail.text.toString().trim()
            dialogBinding.tilResetEmail.error = null

            if (email.isEmpty()) {
                dialogBinding.tilResetEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            dialogBinding.btnSendReset.isEnabled = false
            dialogBinding.btnCancel.isEnabled = false

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    dialogBinding.btnSendReset.isEnabled = true
                    dialogBinding.btnCancel.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset link sent to $email. Check your inbox!",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    } else {
                        dialogBinding.tilResetEmail.error =
                            task.exception?.message ?: "Failed to send reset email"
                    }
                }
        }

        dialog.show()
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
