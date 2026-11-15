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
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
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

        // 1. Populate Teacher / Instructor Dropdown from Firestore Users where accountType == "Teacher"
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

        // 2. Populate Classroom Dropdown from ClassroomRepository
        classroomRepo.getClassrooms(
            onSuccess = { classroomsList ->
                val roomNames = if (classroomsList.isNotEmpty()) {
                    classroomsList.map { it.roomNumber.ifEmpty { "Room" } }
                } else {
                    listOf("Room 204", "Room 205", "Room 206")
                }
                val roomAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    roomNames
                )
                dialogBinding.etClassroomId.setAdapter(roomAdapter)
            },
            onError = {
                val roomAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    listOf("Room 204", "Room 205", "Room 206")
                )
                dialogBinding.etClassroomId.setAdapter(roomAdapter)
            }
        )

        // 3. Populate Day of Week Dropdown
        val dayOptions = DayOfWeek.values().map { it.name }
        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            dayOptions
        )
        dialogBinding.etDay.setAdapter(dayAdapter)

        if (existingSchedule != null) {
            dialogBinding.tvDialogTitle.text = "Edit Schedule"
            dialogBinding.etSubject.setText(existingSchedule.subject)
            dialogBinding.etInstructor.setText(existingSchedule.instructor, false)
            dialogBinding.etClassroomId.setText(existingSchedule.classroomId, false)
            dialogBinding.etDay.setText(existingSchedule.day.name, false)
            dialogBinding.etStartTime.setText(existingSchedule.startTime)
            dialogBinding.etEndTime.setText(existingSchedule.endTime)
        } else {
            dialogBinding.tvDialogTitle.text = "Add Schedule"
            dialogBinding.etDay.setText(DayOfWeek.MONDAY.name, false)
            dialogBinding.etStartTime.setText("09:00")
            dialogBinding.etEndTime.setText("11:00")
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
            val classroomId = dialogBinding.etClassroomId.text.toString().trim()
            val dayStr = dialogBinding.etDay.text.toString().trim()
            val startTime = dialogBinding.etStartTime.text.toString().trim().ifEmpty { "09:00" }
            val endTime = dialogBinding.etEndTime.text.toString().trim().ifEmpty { "11:00" }

            dialogBinding.tilSubject.error = null
            dialogBinding.tilStartTime.error = null

            if (subject.isEmpty()) {
                dialogBinding.tilSubject.error = "Subject code/name is required"
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
                dialogBinding.tilStartTime.error = "Start time must be before end time"
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
                    Toast.makeText(
                        requireContext(),
                        "Conflict! $classroomId already has ${conflictingSchedule.subject} on $dayEnum (${conflictingSchedule.startTime} - ${conflictingSchedule.endTime})",
                        Toast.LENGTH_LONG
                    ).show()
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
