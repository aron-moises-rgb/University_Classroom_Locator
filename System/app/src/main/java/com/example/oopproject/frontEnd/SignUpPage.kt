package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.oopproject.databinding.ActivitySignUpPageBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivitySignUpPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Navigate to Sign In
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, SignInPage::class.java))
            finish()
        }

        // Sign Up button click
        binding.btnSignUp.setOnClickListener {
            val accountType = binding.statusInput.text.toString().trim()
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Clear previous errors
            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null

            // Validate inputs
            if (accountType.isEmpty()) {
                Toast.makeText(this, "Please select an account type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fullName.isEmpty()) {
                binding.tilFullName.error = "Full name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                binding.tilConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Show loading and disable navigation
            binding.progressBar.visibility = ProgressBar.VISIBLE
            binding.btnSignUp.isEnabled = false
            binding.tvLogin.isClickable = false

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        // Get the newly created user's UID
                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            binding.progressBar.visibility = ProgressBar.GONE
                            binding.btnSignUp.isEnabled = true
                            binding.tvLogin.isClickable = true

                            Toast.makeText(
                                this,
                                "Unable to get user information.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        // Prepare user data
                        val userMap = hashMapOf(
                            "fullName" to fullName,
                            "email" to email,
                            "accountType" to accountType,
                            "createdAt" to Timestamp.now()
                        )

                        // Save user information to Firestore
                        // IMPORTANT: Do NOT sign out before this operation.
                        db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener {

                                // Firestore save successful
                                binding.progressBar.visibility = ProgressBar.GONE
                                binding.btnSignUp.isEnabled = true
                                binding.tvLogin.isClickable = true

                                // Sign out AFTER Firestore successfully saves the data
                                auth.signOut()

                                Toast.makeText(
                                    this,
                                    "Account created successfully! Please sign in.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Go to Sign In page
                                startActivity(
                                    Intent(this, SignInPage::class.java)
                                )

                                finish()
                            }
                            .addOnFailureListener { e ->

                                // Firestore failed
                                binding.progressBar.visibility = ProgressBar.GONE
                                binding.btnSignUp.isEnabled = true
                                binding.tvLogin.isClickable = true

                                // Remove the authentication session because
                                // the registration process was not completed
                                auth.signOut()

                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } else {

                        // Firebase Authentication failed
                        binding.progressBar.visibility = ProgressBar.GONE
                        binding.btnSignUp.isEnabled = true
                        binding.tvLogin.isClickable = true

                        Toast.makeText(
                            this,
                            "Sign up failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}