package com.example.oopproject.frontEnd

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Building
import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.RoomStatus
import com.example.oopproject.backEnd.service.AvailabilityService
import com.example.oopproject.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalTime
import java.time.ZoneId

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private val availabilityService = AvailabilityService()

    private var buildingsListener: ListenerRegistration? = null
    private var classroomsListener: ListenerRegistration? = null
    private var schedulesListener: ListenerRegistration? = null

    private var rawClassrooms = listOf<Classroom>()
    private var rawSchedules = listOf<Schedule>()

    private var currentUserRole: String = "Student"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Hide admin shortcut by default until role is checked from Firestore
        binding.btnAdminPortal.visibility = View.GONE

        loadUserProfile()
        loadLiveCampusStats()

        // Quick Action Shortcuts
        binding.btnExploreClassrooms.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.navigateToClassrooms("")
        }

        binding.btnCampusMap.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.navigateToMap()
        }

        binding.btnAdminPortal.setOnClickListener {
            if (currentUserRole.equals("Admin", ignoreCase = true)) {
                val homeActivity = activity as? HomePage
                homeActivity?.replaceFragment(AdminDashboardFragment())
                homeActivity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    ?.selectedItemId = R.id.nav_admin
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding != null) {
                    val rawName = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    val accountType = doc.getString("accountType") ?: "Student"
                    currentUserRole = accountType

                    binding.tvUserName.text = if (rawName.isNotEmpty()) "Welcome, $rawName!" else "Welcome!"
                    binding.tvRoleBadge.text = accountType.uppercase()

                    // Strict Role Access: Only display Admin Management Portal card for Admins
                    if (accountType.equals("Admin", ignoreCase = true)) {
                        binding.tvAdminTitle.text = "Admin Management Portal"
                        binding.tvAdminSubtitle.text = "Manage Buildings, Classrooms & Schedules"
                        binding.btnAdminPortal.visibility = View.VISIBLE
                    } else {
                        // Completely hide Admin Portal card for Students & Teachers
                        binding.btnAdminPortal.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "User profile error: ${e.message}")
            }
    }

    private fun loadLiveCampusStats() {
        // 1. Buildings Count
        buildingsListener = db.collection("buildings").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("HomeFragment", "Buildings error: ${e.message}")
                return@addSnapshotListener
            }
            if (_binding != null && snapshot != null) {
                val buildingsList = snapshot.toObjects(Building::class.java)
                binding.tvBuildingsCount.text = buildingsList.size.toString()
            }
        }

        // 2. Classrooms Count & Data
        classroomsListener = db.collection("classrooms").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("HomeFragment", "Classrooms error: ${e.message}")
                return@addSnapshotListener
            }
            if (_binding != null && snapshot != null) {
                rawClassrooms = snapshot.toObjects(Classroom::class.java)
                recalculateStats()
            }
        }

        // 3. Schedules Data
        schedulesListener = db.collection("schedules").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("HomeFragment", "Schedules error: ${e.message}")
                return@addSnapshotListener
            }
            if (_binding != null && snapshot != null) {
                rawSchedules = snapshot.toObjects(Schedule::class.java)
                recalculateStats()
            }
        }
    }

    private fun recalculateStats() {
        if (_binding == null) return

        val currentDay = availabilityService.getCurrentDayOfWeek()
        val currentTime = LocalTime.now(ZoneId.of("Asia/Manila"))

        var freeCount = 0
        var occupiedCount = 0

        rawClassrooms.forEach { room ->
            val roomSchedules = rawSchedules.filter { sch ->
                sch.classroomId.equals(room.classroomId, ignoreCase = true) ||
                sch.classroomId.equals(room.roomNumber, ignoreCase = true)
            }

            val availability = availabilityService.calculateAvailability(
                schedules = roomSchedules,
                currentDay = currentDay,
                currentTime = currentTime
            )

            when (availability.status) {
                RoomStatus.OCCUPIED -> occupiedCount++
                RoomStatus.AVAILABLE, RoomStatus.UPCOMING -> freeCount++
                RoomStatus.CLOSED -> { /* Campus closed outside operating hours */ }
            }
        }

        binding.tvAvailableCount.text = freeCount.toString()
        binding.tvOccupiedCount.text = occupiedCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        buildingsListener?.remove()
        classroomsListener?.remove()
        schedulesListener?.remove()
        _binding = null
    }
}
