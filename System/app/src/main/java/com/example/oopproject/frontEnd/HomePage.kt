package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.oopproject.R
import com.example.oopproject.databinding.ActivityHomePageBinding
import com.google.firebase.auth.FirebaseAuth

class HomePage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Display current user's email
        binding.tvUserEmail.text = auth.currentUser?.email ?: "No user"

        // Sign out
        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, SignInPage::class.java))
            finish()
        }

        // Bottom Navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home
                    true
                }
                R.id.nav_classrooms -> {
                    Toast.makeText(this, "Classrooms coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilePage::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}