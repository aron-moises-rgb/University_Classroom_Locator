package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.oopproject.R
import com.example.oopproject.databinding.ActivityProfilePageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfilePage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityProfilePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserProfile()

        // Sign Out button click
        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SignInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom Navigation setup
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomePage::class.java))
                    finish()
                    true
                }
                R.id.nav_classrooms -> {
                    Toast.makeText(this, "Classrooms coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // Already on Profile
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            binding.tvProfileHeaderName.text = "Not Signed In"
            binding.tvProfileName.text = "N/A"
            binding.tvProfileEmail.text = "N/A"
            binding.tvProfileAccountType.text = "N/A"
            return
        }

        // Show Firebase Auth email initially
        binding.tvProfileEmail.text = user.email ?: ""
        binding.tvProfileHeaderName.text = user.email?.substringBefore("@") ?: "User"

        // Fetch user data from Firestore
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: "N/A"
                    val email = document.getString("email") ?: user.email ?: "N/A"
                    val accountType = document.getString("accountType") ?: "N/A"

                    binding.tvProfileHeaderName.text = fullName
                    binding.tvProfileName.text = fullName
                    binding.tvProfileEmail.text = email
                    binding.tvProfileAccountType.text = accountType
                } else {
                    binding.tvProfileName.text = "N/A"
                    binding.tvProfileAccountType.text = "N/A"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
    }
}
