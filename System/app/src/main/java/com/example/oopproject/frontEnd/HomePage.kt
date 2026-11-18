package com.example.oopproject.frontEnd

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

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
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            when (item.itemId) {
                R.id.nav_home -> {
                    if (currentFragment !is HomeFragment) replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_classrooms -> {
                    if (currentFragment !is ClassroomsFragment) replaceFragment(ClassroomsFragment())
                    true
                }
                R.id.nav_map -> {
                    if (currentFragment !is CampusMapFragment) replaceFragment(CampusMapFragment())
                    true
                }
                R.id.nav_admin -> {
                    if (currentFragment !is AdminDashboardFragment) replaceFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_profile -> {
                    if (currentFragment !is ProfileFragment) replaceFragment(ProfileFragment())
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

    fun navigateToClassrooms(buildingName: String = "") {
        val fragment = if (buildingName.isNotEmpty()) {
            ClassroomsFragment.newInstance(buildingName)
        } else {
            ClassroomsFragment()
        }
        replaceFragment(fragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_classrooms
    }

    fun navigateToMap() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment !is CampusMapFragment) {
            replaceFragment(CampusMapFragment())
        }
        if (binding.bottomNavigation.selectedItemId != R.id.nav_map) {
            binding.bottomNavigation.selectedItemId = R.id.nav_map
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
