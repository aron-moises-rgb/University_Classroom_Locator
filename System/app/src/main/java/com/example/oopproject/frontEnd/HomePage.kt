package com.example.oopproject.frontEnd

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.example.oopproject.databinding.ActivityHomePageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomePage : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Load default fragment (HomeFragment)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Check if user is Admin to show/hide Admin tab
        checkUserRole()

        // Bottom Navigation setup
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_classrooms -> {
                    replaceFragment(ClassroomsFragment())
                    true
                }
                R.id.nav_admin -> {
                    replaceFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUserRole() {
        val user = auth.currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val accountType = document.getString("accountType") ?: "Student"
                val adminItem = binding.bottomNavigation.menu.findItem(R.id.nav_admin)
                // Show Admin tab if accountType is Admin
                adminItem?.isVisible = accountType.equals("Admin", ignoreCase = true)
            }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
