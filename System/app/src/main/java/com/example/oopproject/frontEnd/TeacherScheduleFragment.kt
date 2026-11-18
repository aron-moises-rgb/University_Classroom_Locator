package com.example.oopproject.frontEnd

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.example.oopproject.backEnd.service.AvailabilityService
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TeacherScheduleFragment : Fragment() {

    private lateinit var rvTeacherSchedules: RecyclerView
    private lateinit var tvTeacherGreeting: TextView
    private lateinit var tvLiveTeacherSyncTime: TextView
    private lateinit var tvNoTeacherSchedules: TextView
    private lateinit var chipGroupDayFilter: ChipGroup

    private lateinit var adapter: TeacherScheduleAdapter
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val availabilityService = AvailabilityService()

    private var schedulesListener: ListenerRegistration? = null
    private var rawTeacherSchedules = listOf<Schedule>()
    private var selectedDayFilter = "All Days"
    private var teacherName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_teacher_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTeacherSchedules = view.findViewById(R.id.rvTeacherSchedules)
        tvTeacherGreeting = view.findViewById(R.id.tvTeacherGreeting)
        tvLiveTeacherSyncTime = view.findViewById(R.id.tvLiveTeacherSyncTime)
        tvNoTeacherSchedules = view.findViewById(R.id.tvNoTeacherSchedules)
        chipGroupDayFilter = view.findViewById(R.id.chipGroupDayFilter)

        rvTeacherSchedules.layoutManager = LinearLayoutManager(requireContext())

        adapter = TeacherScheduleAdapter(
            items = emptyList(),
            onLocateBuildingClick = { schedule ->
                val homeActivity = activity as? HomePage
                val bldgName = extractBuildingNameFromRoom(schedule.classroomId)
                homeActivity?.navigateToMap(bldgName)
            }
        )
        rvTeacherSchedules.adapter = adapter

        setupDayFilterChips()
        loadTeacherProfileAndSchedules()
    }

    private fun setupDayFilterChips() {
        chipGroupDayFilter.removeAllViews()

        val dayOptions = listOf("All Days") + DayOfWeek.values().map { it.name }
        var defaultChipId: Int? = null

        dayOptions.forEach { dName ->
            val chip = Chip(requireContext()).apply {
                text = dName
                isCheckable = true
                isClickable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
                chipStrokeWidth = 0f
            }

            if (dName.equals(selectedDayFilter, ignoreCase = true)) {
                defaultChipId = chip.id
            }

            chipGroupDayFilter.addView(chip)
        }

        if (defaultChipId != null) {
            chipGroupDayFilter.check(defaultChipId)
        }

        chipGroupDayFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                selectedDayFilter = checkedChip?.text?.toString() ?: "All Days"
            } else {
                selectedDayFilter = "All Days"
            }
            filterTeacherSchedules()
        }
    }

    private fun loadTeacherProfileAndSchedules() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (_isFragmentAttached()) {
                    val rawName = (doc.getString("fullName") ?: doc.getString("name") ?: "").trim()
                    teacherName = rawName
                    if (rawName.isNotEmpty()) {
                        tvTeacherGreeting.text = "Prof. $rawName's Schedule"
                    }

                    startSchedulesListener()
                }
            }
            .addOnFailureListener {
                startSchedulesListener()
            }
    }

    private fun startSchedulesListener() {
        schedulesListener = db.collection("schedules").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("TeacherSchedule", "Firestore error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && _isFragmentAttached()) {
                val allSchedules = snapshot.toObjects(Schedule::class.java)

                // Strictly filter schedules assigned only to this teacher
                rawTeacherSchedules = allSchedules.filter { sch ->
                    isScheduleAssignedToTeacher(sch, teacherName)
                }

                filterTeacherSchedules()
            }
        }
    }

    private fun isScheduleAssignedToTeacher(schedule: Schedule, fullTeacherName: String): Boolean {
        val inst = schedule.instructor.trim()
        if (inst.isEmpty() || fullTeacherName.isBlank()) return false

        val cleanInst = inst.lowercase().replace("prof.", "").replace("dr.", "").replace("engr.", "").replace("dean", "").trim()
        val cleanTeacher = fullTeacherName.lowercase().replace("prof.", "").replace("dr.", "").replace("engr.", "").replace("dean", "").trim()

        if (cleanInst == cleanTeacher || cleanInst.contains(cleanTeacher) || cleanTeacher.contains(cleanInst)) {
            return true
        }

        val teacherParts = cleanTeacher.split("\\s+".toRegex()).filter { it.length > 2 }
        val instParts = cleanInst.split("\\s+".toRegex()).filter { it.length > 2 }

        for (tp in teacherParts) {
            for (ip in instParts) {
                if (tp == ip || tp.contains(ip) || ip.contains(tp)) {
                    return true
                }
            }
        }

        return false
    }

    private fun filterTeacherSchedules() {
        val filtered = if (selectedDayFilter.trim().equals("All Days", ignoreCase = true)) {
            rawTeacherSchedules
        } else {
            rawTeacherSchedules.filter { sch ->
                sch.day.name.equals(selectedDayFilter.trim(), ignoreCase = true)
            }
        }.sortedWith(compareBy({ it.day.ordinal }, { it.startTime }))

        if (filtered.isEmpty()) {
            tvNoTeacherSchedules.visibility = View.VISIBLE
        } else {
            tvNoTeacherSchedules.visibility = View.GONE
        }

        adapter.updateSchedules(filtered)
    }

    private fun extractBuildingNameFromRoom(roomStr: String): String {
        val clean = roomStr.uppercase()
        return when {
            clean.contains("HEB") -> "HEB Building"
            clean.contains("CTEB") -> "CTEB Building"
            clean.contains("TAAL") -> "TAAL Building"
            clean.contains("LEMERY") -> "LEMERY Building"
            clean.contains("CAS") -> "CAS Building"
            else -> "HEB Building"
        }
    }

    private fun _isFragmentAttached(): Boolean = isAdded && context != null

    override fun onDestroyView() {
        super.onDestroyView()
        schedulesListener?.remove()
    }
}

// Adapter for Teacher Schedule Cards
class TeacherScheduleAdapter(
    private var items: List<Schedule> = emptyList(),
    private val onLocateBuildingClick: (Schedule) -> Unit = {}
) : RecyclerView.Adapter<TeacherScheduleAdapter.TeacherScheduleViewHolder>() {

    class TeacherScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTeacherSubject: TextView = itemView.findViewById(R.id.tvTeacherSubject)
        val tvDayBadge: TextView = itemView.findViewById(R.id.tvDayBadge)
        val tvTimeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        val tvRoomBuilding: TextView = itemView.findViewById(R.id.tvRoomBuilding)
        val btnLocateBuilding: MaterialButton = itemView.findViewById(R.id.btnLocateBuilding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_schedule_card, parent, false)
        return TeacherScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeacherScheduleViewHolder, position: Int) {
        val item = items[position]

        holder.tvTeacherSubject.text = item.subject.ifEmpty { "Class Session" }
        holder.tvDayBadge.text = item.day.name
        holder.tvTimeSlot.text = "🕒 ${item.startTime} - ${item.endTime}"
        holder.tvRoomBuilding.text = "📍 Room: ${item.classroomId.ifEmpty { "Assigned Room" }}"

        holder.btnLocateBuilding.setOnClickListener { onLocateBuildingClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateSchedules(newItems: List<Schedule>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
