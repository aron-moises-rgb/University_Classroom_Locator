package com.example.oopproject.frontEnd

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Building
import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.RoomStatus
import com.example.oopproject.backEnd.constants.CampusBuildings
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

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && isAdded) {
                val now = LocalTime.now(ZoneId.of("Asia/Manila"))
                val dateNow = LocalDate.now(ZoneId.of("Asia/Manila"))
                val formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm:ss a"))
                val formattedDay = dateNow.format(DateTimeFormatter.ofPattern("EEEE"))

                binding.tvLiveTimeAndDay.text = "🕒 $formattedTime • $formattedDay"
            }
            timeHandler.postDelayed(this, 1000)
        }
    }

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

        // Hide shortcuts by default until role is checked from Firestore
        binding.btnAdminPortal.visibility = View.GONE
        binding.btnTeacherSchedulePortal.visibility = View.GONE

        loadUserProfile()
        loadLiveCampusStats()
        timeHandler.post(timeRunnable)

        // Quick Action Shortcuts
        binding.btnExploreClassrooms.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.navigateToClassrooms("")
        }

        binding.btnCampusMap.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.navigateToMap()
        }

        binding.btnTeacherSchedulePortal.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.navigateToTeacherSchedule()
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

                    // Strict Role Access: Display cards based on user role
                    if (accountType.equals("Admin", ignoreCase = true)) {
                        binding.tvAdminTitle.text = "Admin Management Portal"
                        binding.tvAdminSubtitle.text = "Manage Buildings, Classrooms & Schedules"
                        binding.btnAdminPortal.visibility = View.VISIBLE
                    } else {
                        binding.btnAdminPortal.visibility = View.GONE
                    }

                    if (accountType.equals("Teacher", ignoreCase = true) || accountType.equals("Faculty", ignoreCase = true)) {
                        binding.btnTeacherSchedulePortal.visibility = View.VISIBLE
                    } else {
                        binding.btnTeacherSchedulePortal.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "User profile error: ${e.message}")
            }
    }

    private fun loadLiveCampusStats() {
        // 1. Buildings Count (Static Campus Constants)
        binding.tvBuildingsCount.text = CampusBuildings.COUNT.toString()

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
        timeHandler.removeCallbacks(timeRunnable)
        buildingsListener?.remove()
        classroomsListener?.remove()
        schedulesListener?.remove()
        _binding = null
    }
}
