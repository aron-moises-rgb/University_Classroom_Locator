package com.example.oopproject

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class sign_up_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Views
        val root = findViewById<ScrollView>(R.id.main)
        val statusInput = findViewById<MaterialAutoCompleteTextView>(R.id.statusInput)
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val tilFullName = findViewById<TextInputLayout>(R.id.tilFullName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                (v as ScrollView).post {
                    v.smoothScrollTo(0, v.getChildAt(0).bottom)
                }
            }
            insets
        }

        // Navigate to Sign In
        tvLogin.setOnClickListener {
            startActivity(Intent(this, sign_in_page::class.java))
            finish()
        }

        // Sign Up button click
        btnSignUp.setOnClickListener {
            val accountType = statusInput.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Clear previous errors
            tilFullName.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            // Validate inputs
            if (accountType.isEmpty()) {
                Toast.makeText(this, "Please select an account type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fullName.isEmpty()) {
                tilFullName.error = "Full name is required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Show loading and disable navigation
            progressBar.visibility = ProgressBar.VISIBLE
            btnSignUp.isEnabled = false
            tvLogin.isClickable = false

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        // Get the newly created user's UID
                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            progressBar.visibility = ProgressBar.GONE
                            btnSignUp.isEnabled = true
                            tvLogin.isClickable = true

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
                                progressBar.visibility = ProgressBar.GONE
                                btnSignUp.isEnabled = true
                                tvLogin.isClickable = true

                                // Sign out AFTER Firestore successfully saves the data
                                auth.signOut()

                                Toast.makeText(
                                    this,
                                    "Account created successfully! Please sign in.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Go to Sign In page
                                startActivity(
                                    Intent(this, sign_in_page::class.java)
                                )

                                finish()
                            }
                            .addOnFailureListener { e ->

                                // Firestore failed
                                progressBar.visibility = ProgressBar.GONE
                                btnSignUp.isEnabled = true
                                tvLogin.isClickable = true

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
                        progressBar.visibility = ProgressBar.GONE
                        btnSignUp.isEnabled = true
                        tvLogin.isClickable = true

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