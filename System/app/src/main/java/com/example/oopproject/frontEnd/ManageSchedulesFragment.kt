package com.example.oopproject.frontEnd

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.example.oopproject.backEnd.repository.BuildingRepository
import com.example.oopproject.backEnd.repository.ClassroomRepository
import com.example.oopproject.backEnd.repository.ScheduleRepository
import com.example.oopproject.backEnd.util.ScheduleSeeder
import com.example.oopproject.databinding.DialogAddEditScheduleBinding
import com.example.oopproject.frontEnd.adapters.ScheduleAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalTime

class ManageSchedulesFragment : Fragment() {

    private lateinit var rvSchedules: RecyclerView
    private lateinit var btnAddSchedule: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: ScheduleAdapter
    private val scheduleRepo = ScheduleRepository()
    private val classroomRepo = ClassroomRepository()
    private val buildingRepo = BuildingRepository()
    private val scheduleSeeder = ScheduleSeeder()
    private val db = FirebaseFirestore.getInstance()

    private var schedulesListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_manage_schedules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvSchedules = view.findViewById(R.id.rvSchedules)
        btnAddSchedule = view.findViewById(R.id.btnAddSchedule)
        btnBack = view.findViewById(R.id.btnBack)

        rvSchedules.layoutManager = LinearLayoutManager(requireContext())

        adapter = ScheduleAdapter(
            items = emptyList(),
            onEditClick = { schedule ->
                showAddEditScheduleDialog(schedule)
            },
            onDeleteClick = { schedule ->
                confirmDeleteSchedule(schedule)
            }
        )
        rvSchedules.adapter = adapter

        btnBack.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.replaceFragment(AdminDashboardFragment())
        }

        btnAddSchedule.setOnClickListener {
            showAddEditScheduleDialog(null)
        }

        loadSchedules()
    }

    private fun loadSchedules() {
        schedulesListener = db.collection("schedules").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ManageSchedules", "Firestore error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val schedulesFromDb = snapshot.toObjects(Schedule::class.java)
                if (schedulesFromDb.isEmpty()) {
                    scheduleSeeder.seedSchedulesFromRealClassrooms(
                        onSuccess = { count ->
                            if (isAdded && count > 0) {
                                Toast.makeText(requireContext(), "Seeded $count sample schedules across your classrooms!", Toast.LENGTH_LONG).show()
                            }
                        },
                        onError = { e ->
                            Log.e("ManageSchedules", "Seed failed: ${e.message}")
                        }
                    )
                }
                adapter.updateSchedules(schedulesFromDb)
            }
        }
    }

    private fun showAddEditScheduleDialog(existingSchedule: Schedule?) {
        val dialogBinding = DialogAddEditScheduleBinding.inflate(layoutInflater)
        var allClassroomsList = listOf<Classroom>()

        // 1. Populate Teacher / Instructor Dropdown from Firestore Users
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val teacherNames = result.documents.mapNotNull { doc ->
                    val accType = doc.getString("accountType") ?: ""
                    val rawName = (doc.getString("fullName") ?: doc.getString("name") ?: "").trim()
                    if (accType.equals("Teacher", ignoreCase = true) || accType.equals("Faculty", ignoreCase = true)) {
                        val nameToUse = rawName.ifEmpty { doc.getString("email") ?: "Instructor" }
                        if (nameToUse.startsWith("Prof.", ignoreCase = true) ||
                            nameToUse.startsWith("Dr.", ignoreCase = true) ||
                            nameToUse.startsWith("Engr.", ignoreCase = true) ||
                            nameToUse.startsWith("Dean", ignoreCase = true)
                        ) {
                            nameToUse
                        } else {
                            "Prof. $nameToUse"
                        }
                    } else {
                        null
                    }
                }.distinct()

                val fallbackTeachers = if (teacherNames.isNotEmpty()) teacherNames else listOf("Prof. Santos", "Prof. Cruz", "Dr. Reyes")
                val teacherAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    fallbackTeachers
                )
                dialogBinding.etInstructor.setAdapter(teacherAdapter)
            }
            .addOnFailureListener {
                val fallbackAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    listOf("Prof. Santos", "Prof. Cruz", "Dr. Reyes")
                )
                dialogBinding.etInstructor.setAdapter(fallbackAdapter)
            }

        // Helper function to update classroom dropdown based on selected building
        fun updateClassroomDropdown(selectedBuilding: String) {
            if (selectedBuilding.isBlank()) {
                dialogBinding.etClassroomId.setAdapter(null)
                return
            }

            val matchingRooms = allClassroomsList.filter { it.buildingId.equals(selectedBuilding, ignoreCase = true) }

            val roomNames = if (matchingRooms.isNotEmpty()) {
                matchingRooms.map { it.roomNumber.ifEmpty { "Room" } }
            } else {
                allClassroomsList.map { it.roomNumber.ifEmpty { "Room" } }
            }

            val roomAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                if (roomNames.isNotEmpty()) roomNames else listOf("Room 204", "Room 205", "Room 206")
            )
            dialogBinding.etClassroomId.setAdapter(roomAdapter)
        }

        // 2. Fetch Buildings and Classrooms for Cascading Dropdowns
        buildingRepo.getBuildings(
            onSuccess = { buildingsList ->
                val buildingNames = if (buildingsList.isNotEmpty()) {
                    buildingsList.map { it.name.ifEmpty { "Building" } }
                } else {
                    listOf("CAS Building", "Taal Building", "HEB Building")
                }
                val buildingAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    buildingNames
                )
                dialogBinding.etBuildingId.setAdapter(buildingAdapter)
            },
            onError = {
                val fallbackBuildingAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    listOf("CAS Building", "Taal Building", "HEB Building")
                )
                dialogBinding.etBuildingId.setAdapter(fallbackBuildingAdapter)
            }
        )

        classroomRepo.getClassrooms(
            onSuccess = { classroomsList ->
                allClassroomsList = classroomsList

                if (existingSchedule != null) {
                    val matchingRoom = classroomsList.find {
                        it.roomNumber.equals(existingSchedule.classroomId, ignoreCase = true) ||
                        it.classroomId.equals(existingSchedule.classroomId, ignoreCase = true)
                    }
                    if (matchingRoom != null) {
                        dialogBinding.etBuildingId.setText(matchingRoom.buildingId, false)
                        updateClassroomDropdown(matchingRoom.buildingId)
                    } else {
                        updateClassroomDropdown("")
                    }
                } else {
                    // When adding a new schedule, clear classroom until building is selected
                    dialogBinding.etClassroomId.setText("", false)
                    updateClassroomDropdown("")
                }
            },
            onError = {
                updateClassroomDropdown("")
            }
        )

        // Prompt user with inline red error if classroom dropdown is clicked before choosing a building
        dialogBinding.etClassroomId.setOnClickListener {
            val currentBldg = dialogBinding.etBuildingId.text.toString().trim()
            if (currentBldg.isEmpty()) {
                dialogBinding.etClassroomId.dismissDropDown()
                dialogBinding.tilBuildingId.error = "Please select a building first"
            } else {
                dialogBinding.tilBuildingId.error = null
            }
        }

        // When building selection changes, filter classroom dropdown and clear error
        dialogBinding.etBuildingId.setOnItemClickListener { _, _, _, _ ->
            dialogBinding.tilBuildingId.error = null
            val selectedBldg = dialogBinding.etBuildingId.text.toString().trim()
            updateClassroomDropdown(selectedBldg)
            dialogBinding.etClassroomId.setText("", false)
        }

        // 3. Populate Day of Week Dropdown
        val dayOptions = DayOfWeek.values().map { it.name }
        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            dayOptions
        )
        dialogBinding.etDay.setAdapter(dayAdapter)

        // 4. Populate Hour & Minute Dropdowns (Start & End)
        val hourOptions = (7..20).map { String.format("%02d", it) }
        val minuteOptions = listOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")

        val hourAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hourOptions)
        val minAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, minuteOptions)

        dialogBinding.etStartHour.setAdapter(hourAdapter)
        dialogBinding.etStartMin.setAdapter(minAdapter)
        dialogBinding.etEndHour.setAdapter(hourAdapter)
        dialogBinding.etEndMin.setAdapter(minAdapter)

        if (existingSchedule != null) {
            dialogBinding.tvDialogTitle.text = "Edit Schedule"
            dialogBinding.etSubject.setText(existingSchedule.subject)
            dialogBinding.etInstructor.setText(existingSchedule.instructor, false)
            dialogBinding.etClassroomId.setText(existingSchedule.classroomId, false)
            dialogBinding.etDay.setText(existingSchedule.day.name, false)

            val sParts = existingSchedule.startTime.split(":")
            val eParts = existingSchedule.endTime.split(":")

            dialogBinding.etStartHour.setText(sParts.getOrNull(0) ?: "09", false)
            dialogBinding.etStartMin.setText(sParts.getOrNull(1) ?: "00", false)
            dialogBinding.etEndHour.setText(eParts.getOrNull(0) ?: "11", false)
            dialogBinding.etEndMin.setText(eParts.getOrNull(1) ?: "00", false)
        } else {
            dialogBinding.tvDialogTitle.text = "Add Schedule"
            dialogBinding.etDay.setText(DayOfWeek.MONDAY.name, false)
            dialogBinding.etStartHour.setText("09", false)
            dialogBinding.etStartMin.setText("00", false)
            dialogBinding.etEndHour.setText("11", false)
            dialogBinding.etEndMin.setText("00", false)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val subject = dialogBinding.etSubject.text.toString().trim()
            val instructor = dialogBinding.etInstructor.text.toString().trim()
            val buildingId = dialogBinding.etBuildingId.text.toString().trim()
            val classroomId = dialogBinding.etClassroomId.text.toString().trim()
            val dayStr = dialogBinding.etDay.text.toString().trim()

            val startHour = dialogBinding.etStartHour.text.toString().trim().ifEmpty { "09" }
            val startMin = dialogBinding.etStartMin.text.toString().trim().ifEmpty { "00" }
            val endHour = dialogBinding.etEndHour.text.toString().trim().ifEmpty { "11" }
            val endMin = dialogBinding.etEndMin.text.toString().trim().ifEmpty { "00" }

            val startTime = "$startHour:$startMin"
            val endTime = "$endHour:$endMin"

            dialogBinding.tilSubject.error = null
            dialogBinding.tilBuildingId.error = null
            dialogBinding.tilClassroomId.error = null
            dialogBinding.tilStartHour.error = null

            if (subject.isEmpty()) {
                dialogBinding.tilSubject.error = "Subject code/name is required"
                return@setOnClickListener
            }

            if (buildingId.isEmpty()) {
                dialogBinding.tilBuildingId.error = "Please select a building first"
                return@setOnClickListener
            }

            if (classroomId.isEmpty()) {
                dialogBinding.tilClassroomId.error = "Please select a classroom"
                return@setOnClickListener
            }

            val dayEnum = try {
                DayOfWeek.valueOf(dayStr.uppercase())
            } catch (_: Exception) {
                DayOfWeek.MONDAY
            }

            val newStart = parseTime(startTime)
            val newEnd = parseTime(endTime)

            if (newStart != null && newEnd != null && !newStart.isBefore(newEnd)) {
                dialogBinding.tilStartHour.error = "Start time must be before end time"
                return@setOnClickListener
            }

            // Conflict Check in Firestore before saving
            db.collection("schedules").get().addOnSuccessListener { snapshot ->
                val allSchedules = snapshot.toObjects(Schedule::class.java)

                val conflictingSchedule = allSchedules.find { sched ->
                    if (existingSchedule != null && sched.scheduleId == existingSchedule.scheduleId) {
                        return@find false
                    }

                    val sameRoom = sched.classroomId.equals(classroomId, ignoreCase = true)
                    val sameDay = sched.day == dayEnum

                    if (sameRoom && sameDay) {
                        val exStart = parseTime(sched.startTime) ?: return@find false
                        val exEnd = parseTime(sched.endTime) ?: return@find false

                        if (newStart != null && newEnd != null) {
                            newStart.isBefore(exEnd) && newEnd.isAfter(exStart)
                        } else false
                    } else false
                }

                if (conflictingSchedule != null) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Schedule Conflict Detected")
                        .setMessage("Room $classroomId is already booked for [${conflictingSchedule.subject}] on $dayEnum from ${conflictingSchedule.startTime} to ${conflictingSchedule.endTime}.\n\nPlease choose a different time slot or classroom.")
                        .setPositiveButton("Got It", null)
                        .show()
                    return@addOnSuccessListener
                }

                dialogBinding.btnSave.isEnabled = false

                if (existingSchedule != null) {
                    val updated = existingSchedule.copy(
                        subject = subject,
                        instructor = instructor,
                        classroomId = classroomId,
                        day = dayEnum,
                        startTime = startTime,
                        endTime = endTime
                    )
                    scheduleRepo.updateSchedule(
                        schedule = updated,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Schedule updated!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        },
                        onError = { e ->
                            dialogBinding.btnSave.isEnabled = true
                            Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    val newSchedule = Schedule(
                        subject = subject,
                        instructor = instructor,
                        classroomId = classroomId,
                        day = dayEnum,
                        startTime = startTime,
                        endTime = endTime
                    )
                    scheduleRepo.addSchedule(
                        schedule = newSchedule,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Schedule added!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        },
                        onError = { e ->
                            dialogBinding.btnSave.isEnabled = true
                            Toast.makeText(requireContext(), "Add failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    private fun confirmDeleteSchedule(schedule: Schedule) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete ${schedule.subject}?")
            .setPositiveButton("Delete") { _, _ ->
                scheduleRepo.deleteSchedule(
                    scheduleId = schedule.scheduleId,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Schedule deleted!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseTime(timeStr: String): LocalTime? {
        if (timeStr.isBlank()) return null
        return try {
            val clean = timeStr.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsOnly = clean.replace(Regex("[^0-9:]"), "")
            val parts = digitsOnly.split(":")
            var hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0

            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0

            LocalTime.of(hour, min)
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        schedulesListener?.remove()
    }
}
