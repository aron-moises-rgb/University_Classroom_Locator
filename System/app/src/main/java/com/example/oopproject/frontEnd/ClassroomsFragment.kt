package com.example.oopproject.frontEnd

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Floor
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.RoomStatus
import com.example.oopproject.backEnd.repository.BuildingRepository
import com.example.oopproject.backEnd.repository.ClassroomRepository
import com.example.oopproject.backEnd.repository.FloorRepository
import com.example.oopproject.backEnd.repository.ScheduleRepository
import com.example.oopproject.backEnd.service.AvailabilityService
import com.example.oopproject.frontEnd.adapters.ClassroomAdapter
import com.example.oopproject.frontEnd.models.ClassroomUiModel
import com.example.oopproject.frontEnd.models.RoomStatusType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClassroomsFragment : Fragment() {

    private lateinit var rvClassrooms: RecyclerView
    private lateinit var adapter: ClassroomAdapter

    private lateinit var chipGroupBuildings: ChipGroup
    private lateinit var chipGroupFloors: ChipGroup
    private lateinit var tvSelectedBuildingHeader: TextView
    private lateinit var tvSelectedLevelHeader: TextView
    private lateinit var tvFacilitiesCount: TextView
    private lateinit var tvLiveSyncTime: TextView

    private val classroomRepo = ClassroomRepository()
    private val buildingRepo = BuildingRepository()
    private val floorRepo = FloorRepository()
    private val scheduleRepo = ScheduleRepository()
    private val availabilityService = AvailabilityService()

    private var classroomsListener: ListenerRegistration? = null
    private var buildingsListener: ListenerRegistration? = null
    private var floorsListener: ListenerRegistration? = null
    private var schedulesListener: ListenerRegistration? = null

    private var rawClassroomsFromDb = listOf<Classroom>()
    private var rawFloorsFromDb = listOf<Floor>()
    private var rawSchedulesFromDb = listOf<Schedule>()

    // Default filter to "All Buildings" and "Floor 1"
    private var selectedBuilding: String = "All Buildings"
    private var selectedFloor: Int? = 1
    private var isBuildingFilterProgrammatic = false

    private var allClassesList = listOf<ClassroomUiModel>()

    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            if (isAdded) {
                recalculateAvailabilityAndRefreshUi()
                val millisUntilNextMinute = 60000L - (System.currentTimeMillis() % 60000L)
                timeUpdateHandler.postDelayed(this, millisUntilNextMinute)
            }
        }
    }

    companion object {
        private const val ARG_BUILDING_NAME = "arg_building_name"

        fun newInstance(buildingName: String = ""): ClassroomsFragment {
            val fragment = ClassroomsFragment()
            val args = Bundle()
            args.putString(ARG_BUILDING_NAME, buildingName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_classrooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialBldg = arguments?.getString(ARG_BUILDING_NAME)
        if (!initialBldg.isNullOrEmpty()) {
            selectedBuilding = initialBldg
        }

        rvClassrooms = view.findViewById(R.id.rvClassrooms)
        chipGroupBuildings = view.findViewById(R.id.chipGroupBuildings)
        chipGroupFloors = view.findViewById(R.id.chipGroupFloors)
        tvSelectedBuildingHeader = view.findViewById(R.id.tvSelectedBuildingHeader)
        tvSelectedLevelHeader = view.findViewById(R.id.tvSelectedLevelHeader)
        tvFacilitiesCount = view.findViewById(R.id.tvFacilitiesCount)
        tvLiveSyncTime = view.findViewById(R.id.tvLiveSyncTime)

        rvClassrooms.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClassroomAdapter(
            items = emptyList(),
            onDetailsClick = { room ->
                Toast.makeText(requireContext(), "Details for ${room.roomName}", Toast.LENGTH_SHORT).show()
            },
            onLocateOnMapClick = { room ->
                Toast.makeText(requireContext(), "Locating ${room.roomName} on Campus Map...", Toast.LENGTH_SHORT).show()
            }
        )
        rvClassrooms.adapter = adapter

        setupFilterListeners()
        loadRealtimeBuildings()
        loadRealtimeClassroomsAndSchedules()

        // Start precision-synchronized system clock timer
        timeUpdateHandler.post(timeUpdateRunnable)
    }

    private fun loadRealtimeBuildings() {
        buildingsListener = buildingRepo.listenToBuildings(
            onUpdate = { buildingsFromDb ->
                if (isAdded) {
                    val targetBuilding = selectedBuilding
                    isBuildingFilterProgrammatic = true
                    chipGroupBuildings.removeAllViews()

                    var targetChipId: Int? = null

                    // Always add "All Buildings" chip first
                    val allChip = createChip("All Buildings")
                    if (targetBuilding.equals("All Buildings", ignoreCase = true)) {
                        targetChipId = allChip.id
                    }
                    chipGroupBuildings.addView(allChip)

                    // Dynamically add chips for every building in Firestore
                    buildingsFromDb.forEach { bldg ->
                        val bName = bldg.name.ifEmpty { "Building" }
                        val bChip = createChip(bName)
                        if (targetBuilding.trim().equals(bName.trim(), ignoreCase = true)) {
                            targetChipId = bChip.id
                        }
                        chipGroupBuildings.addView(bChip)
                    }

                    if (targetChipId != null) {
                        chipGroupBuildings.check(targetChipId)
                    } else {
                        chipGroupBuildings.check(allChip.id)
                    }

                    isBuildingFilterProgrammatic = false
                    filterClassrooms()
                }
            },
            onError = { e ->
                Log.e("ClassroomsFragment", "Building listener error: ${e.message}")
            }
        )
    }

    private fun createChip(label: String): Chip {
        return Chip(requireContext()).apply {
            id = View.generateViewId()
            text = label
            isCheckable = true
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
        }
    }

    private fun loadRealtimeClassroomsAndSchedules() {
        // Listen to Classrooms
        classroomsListener = classroomRepo.listenToClassrooms(
            onUpdate = { classroomsFromDb ->
                rawClassroomsFromDb = classroomsFromDb
                recalculateAvailabilityAndRefreshUi()
            },
            onError = { e ->
                Log.e("ClassroomsFragment", "Classrooms listener error: ${e.message}")
            }
        )

        // Listen to Floors via FloorRepository
        floorsListener = floorRepo.listenToFloors(
            onUpdate = { floorsFromDb ->
                rawFloorsFromDb = floorsFromDb
                recalculateAvailabilityAndRefreshUi()
            },
            onError = { e ->
                Log.e("ClassroomsFragment", "Floors listener error: ${e.message}")
            }
        )

        // Listen to Schedules
        schedulesListener = scheduleRepo.listenToSchedules(
            onUpdate = { schedulesFromDb ->
                rawSchedulesFromDb = schedulesFromDb
                recalculateAvailabilityAndRefreshUi()
            },
            onError = { e ->
                Log.e("ClassroomsFragment", "Schedules listener error: ${e.message}")
            }
        )
    }

    private fun recalculateAvailabilityAndRefreshUi() {
        val currentDay = availabilityService.getCurrentDayOfWeek()
        val currentTime = LocalTime.now(ZoneId.of("Asia/Manila"))

        if (::tvLiveSyncTime.isInitialized) {
            val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
            tvLiveSyncTime.text = "Live Sync: $formattedTime"
        }

        val firestoreModels = rawClassroomsFromDb.map { room ->
            // Parse floor number
            var fNum = room.floorId.replace(Regex("[^0-9]"), "").toIntOrNull()
            if (fNum == null || fNum == 0) {
                val digits = room.roomNumber.replace(Regex("[^0-9]"), "")
                if (digits.length >= 3) {
                    fNum = digits.substring(0, 1).toIntOrNull() ?: 1
                } else {
                    fNum = 1
                }
            }

            val matchingFloor = rawFloorsFromDb.find { fl ->
                fl.buildingId.equals(room.buildingId, ignoreCase = true) && fl.floorNumber == fNum
            }
            val floorLabel = if (matchingFloor != null && matchingFloor.name.isNotEmpty()) {
                matchingFloor.name
            } else {
                "Floor $fNum"
            }

            val bName = room.buildingId.ifEmpty { "Main Building" }

            // Get schedules for this room
            val roomSchedules = rawSchedulesFromDb.filter { sch ->
                sch.classroomId.equals(room.classroomId, ignoreCase = true) ||
                sch.classroomId.equals(room.roomNumber, ignoreCase = true)
            }

            // Calculate live availability with AvailabilityService
            val availability = availabilityService.calculateAvailability(
                schedules = roomSchedules,
                currentDay = currentDay,
                currentTime = currentTime
            )

            val uiStatusType = when (availability.status) {
                RoomStatus.AVAILABLE -> RoomStatusType.AVAILABLE
                RoomStatus.OCCUPIED -> RoomStatusType.OCCUPIED
                RoomStatus.UPCOMING -> RoomStatusType.UPCOMING
                RoomStatus.CLOSED -> RoomStatusType.CLOSED
            }

            val uiStatusText = when (availability.status) {
                RoomStatus.AVAILABLE -> "• AVAILABLE"
                RoomStatus.OCCUPIED -> "• OCCUPIED"
                RoomStatus.UPCOMING -> "• UPCOMING"
                RoomStatus.CLOSED -> "• CLOSED"
            }

            ClassroomUiModel(
                roomId = room.classroomId,
                roomName = room.roomNumber.ifEmpty { "Classroom" },
                buildingName = bName,
                floorNumber = fNum,
                roomSubtitle = "$floorLabel • Capacity: ${room.capacity} seats • $bName",
                statusType = uiStatusType,
                statusText = uiStatusText,
                bannerTitle = availability.bannerTitle,
                bannerSubtitle = availability.bannerSubtitle
            )
        }

        allClassesList = firestoreModels
        updateDynamicFloorChips()
        filterClassrooms()
    }

    private fun updateDynamicFloorChips() {
        if (!isAdded) return
        chipGroupFloors.removeAllViews()

        // Discover all unique floor numbers present in Firestore classrooms
        val availableFloors = allClassesList.map { it.floorNumber }.distinct().sorted()

        val activeFloor = if (selectedFloor in availableFloors) selectedFloor else (availableFloors.firstOrNull() ?: 1)
        selectedFloor = activeFloor

        var matchingFloorChipId: Int? = null

        availableFloors.forEach { fNum ->
            val fLabel = "Floor $fNum"
            val fChip = createChip(fLabel)
            if (selectedFloor == fNum) {
                matchingFloorChipId = fChip.id
            }
            chipGroupFloors.addView(fChip)
        }

        if (matchingFloorChipId != null) {
            chipGroupFloors.check(matchingFloorChipId)
        }
    }

    private fun setupFilterListeners() {
        chipGroupBuildings.setOnCheckedStateChangeListener { group, checkedIds ->
            if (isBuildingFilterProgrammatic) return@setOnCheckedStateChangeListener
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                selectedBuilding = checkedChip?.text?.toString() ?: "All Buildings"
            } else {
                selectedBuilding = "All Buildings"
            }
            filterClassrooms()
        }

        chipGroupFloors.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                val text = checkedChip?.text?.toString() ?: ""
                val parsedFloor = text.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (parsedFloor != null) {
                    selectedFloor = parsedFloor
                }
            } else {
                selectedFloor = null
            }
            filterClassrooms()
        }
    }

    private fun filterClassrooms() {
        val filteredList = allClassesList.filter { room ->
            val matchesBuilding = when (selectedBuilding.trim()) {
                "All Buildings" -> true
                else -> room.buildingName.trim().equals(selectedBuilding.trim(), ignoreCase = true)
            }

            val matchesFloor = when (selectedFloor) {
                null -> true
                else -> room.floorNumber == selectedFloor
            }

            matchesBuilding && matchesFloor
        }

        tvSelectedBuildingHeader.text = selectedBuilding
        tvSelectedLevelHeader.text = if (selectedFloor != null) "Level $selectedFloor" else "All Levels"
        tvFacilitiesCount.text = "${filteredList.size} Facilities"

        adapter.updateClassrooms(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        classroomsListener?.remove()
        buildingsListener?.remove()
        floorsListener?.remove()
        schedulesListener?.remove()
    }
}
