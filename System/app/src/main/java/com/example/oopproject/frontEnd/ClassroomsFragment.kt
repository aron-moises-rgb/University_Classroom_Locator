package com.example.oopproject.frontEnd

import android.os.Bundle
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
import com.example.oopproject.backEnd.repository.BuildingRepository
import com.example.oopproject.backEnd.repository.ClassroomRepository
import com.example.oopproject.frontEnd.adapters.ClassroomAdapter
import com.example.oopproject.frontEnd.models.ClassroomUiModel
import com.example.oopproject.frontEnd.models.RoomStatusType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ListenerRegistration

class ClassroomsFragment : Fragment() {

    private lateinit var rvClassrooms: RecyclerView
    private lateinit var adapter: ClassroomAdapter

    private lateinit var chipGroupBuildings: ChipGroup
    private lateinit var chipGroupFloors: ChipGroup
    private lateinit var tvSelectedBuildingHeader: TextView
    private lateinit var tvSelectedLevelHeader: TextView
    private lateinit var tvFacilitiesCount: TextView

    private val classroomRepo = ClassroomRepository()
    private val buildingRepo = BuildingRepository()

    private var classroomsListener: ListenerRegistration? = null
    private var buildingsListener: ListenerRegistration? = null

    // Default filter to "All Buildings" and "All Floors" so all rooms are visible
    private var selectedBuilding: String = "All Buildings"
    private var selectedFloor: Int? = 1

    private var allClassesList = listOf<ClassroomUiModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_classrooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvClassrooms = view.findViewById(R.id.rvClassrooms)
        chipGroupBuildings = view.findViewById(R.id.chipGroupBuildings)
        chipGroupFloors = view.findViewById(R.id.chipGroupFloors)
        tvSelectedBuildingHeader = view.findViewById(R.id.tvSelectedBuildingHeader)
        tvSelectedLevelHeader = view.findViewById(R.id.tvSelectedLevelHeader)
        tvFacilitiesCount = view.findViewById(R.id.tvFacilitiesCount)

        rvClassrooms.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClassroomAdapter(
            items = emptyList(),
            onDetailsClick = { room ->
                Toast.makeText(requireContext(), "Details for ${room.roomName}", Toast.LENGTH_SHORT).show()
            },
            onViewInfoClick = { room ->
                Toast.makeText(requireContext(), "Viewing info for ${room.roomName}", Toast.LENGTH_SHORT).show()
            }
        )
        rvClassrooms.adapter = adapter

        setupFilterListeners()
        loadRealtimeBuildings()
        loadRealtimeClassrooms()
    }

    private fun loadRealtimeBuildings() {
        buildingsListener = buildingRepo.listenToBuildings(
            onUpdate = { buildingsFromDb ->
                if (isAdded) {
                    chipGroupBuildings.removeAllViews()

                    // Always add "All Buildings" chip first
                    val allChip = createChip("All Buildings", selectedBuilding == "All Buildings")
                    chipGroupBuildings.addView(allChip)

                    // Dynamically add chips for every building in Firestore
                    buildingsFromDb.forEach { bldg ->
                        val bName = bldg.name.ifEmpty { "Building" }
                        val bChip = createChip(bName, selectedBuilding.equals(bName, ignoreCase = true))
                        chipGroupBuildings.addView(bChip)
                    }
                }
            },
            onError = {}
        )
    }

    private fun createChip(label: String, isChecked: Boolean): Chip {
        return Chip(requireContext()).apply {
            id = View.generateViewId()
            text = label
            isCheckable = true
            this.isChecked = isChecked
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
        }
    }

    private fun loadRealtimeClassrooms() {
        classroomsListener = classroomRepo.listenToClassrooms(
            onUpdate = { classroomsFromDb ->
                val firestoreModels = classroomsFromDb.map { room ->
                    // 1. Try parsing floorId string
                    var fNum = room.floorId.replace(Regex("[^0-9]"), "").toIntOrNull()

                    // 2. If floorId is empty/invalid, infer floor from room number first digit (e.g. "HEB 201" -> "201" -> Floor 2)
                    if (fNum == null || fNum == 0) {
                        val digits = room.roomNumber.replace(Regex("[^0-9]"), "")
                        if (digits.length >= 3) {
                            fNum = digits.substring(0, 1).toIntOrNull() ?: 1
                        } else {
                            fNum = 1
                        }
                    }

                    val bName = room.buildingId.ifEmpty { "Main Building" }

                    ClassroomUiModel(
                        roomId = room.classroomId,
                        roomName = room.roomNumber.ifEmpty { "Classroom" },
                        buildingName = bName,
                        floorNumber = fNum,
                        roomSubtitle = "Floor $fNum • Capacity: ${room.capacity} seats • $bName",
                        statusType = RoomStatusType.AVAILABLE,
                        statusText = "• AVAILABLE",
                        bannerTitle = "Capacity: ${room.capacity} Students",
                        bannerSubtitle = "Live Sync from Firestore"
                    )
                }
                allClassesList = firestoreModels
                updateDynamicFloorChips()
                filterClassrooms()
            },
            onError = {}
        )
    }

    private fun updateDynamicFloorChips() {
        if (!isAdded) return
        chipGroupFloors.removeAllViews()

        // Discover all unique floor numbers present in Firestore classrooms
        val availableFloors = allClassesList.map { it.floorNumber }.distinct().sorted()

        val activeFloor = if (selectedFloor in availableFloors) selectedFloor else (availableFloors.firstOrNull() ?: 1)
        selectedFloor = activeFloor

        availableFloors.forEach { fNum ->
            val fLabel = "Floor $fNum"
            val fChip = createChip(fLabel, selectedFloor == fNum)
            chipGroupFloors.addView(fChip)
        }
    }

    private fun setupFilterListeners() {
        chipGroupBuildings.setOnCheckedStateChangeListener { group, checkedIds ->
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
        classroomsListener?.remove()
        buildingsListener?.remove()
    }
}
