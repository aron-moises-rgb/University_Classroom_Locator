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
    private var isProgrammaticNav = false

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

        // Check user role to adjust tabs dynamically
        checkUserRole()

        // Bottom Navigation setup
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isProgrammaticNav) return@setOnItemSelectedListener true

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
                R.id.nav_teacher_schedule -> {
                    if (currentFragment !is TeacherScheduleFragment) replaceFragment(TeacherScheduleFragment())
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
                val teacherItem = binding.bottomNavigation.menu.findItem(R.id.nav_teacher_schedule)

                adminItem?.isVisible = accountType.equals("Admin", ignoreCase = true)
                teacherItem?.isVisible = accountType.equals("Teacher", ignoreCase = true) || accountType.equals("Faculty", ignoreCase = true)
            }
    }

    fun navigateToClassrooms(buildingName: String = "") {
        isProgrammaticNav = true
        val fragment = if (buildingName.isNotEmpty()) {
            ClassroomsFragment.newInstance(buildingName)
        } else {
            ClassroomsFragment()
        }
        replaceFragment(fragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_classrooms
        isProgrammaticNav = false
    }

    fun navigateToMap(buildingName: String = "") {
        isProgrammaticNav = true
        replaceFragment(CampusMapFragment.newInstance(buildingName))
        binding.bottomNavigation.selectedItemId = R.id.nav_map
        isProgrammaticNav = false
    }

    fun navigateToTeacherSchedule() {
        isProgrammaticNav = true
        replaceFragment(TeacherScheduleFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_teacher_schedule
        isProgrammaticNav = false
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
