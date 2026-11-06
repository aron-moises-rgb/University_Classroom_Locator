package com.example.oopproject.frontEnd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.frontEnd.adapters.ClassroomAdapter
import com.example.oopproject.frontEnd.models.ClassroomUiModel
import com.example.oopproject.frontEnd.models.RoomStatusType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ClassroomsFragment : Fragment() {

    private lateinit var rvClassrooms: RecyclerView
    private lateinit var adapter: ClassroomAdapter

    private lateinit var chipGroupBuildings: ChipGroup
    private lateinit var chipGroupFloors: ChipGroup
    private lateinit var tvSelectedBuildingHeader: TextView
    private lateinit var tvSelectedLevelHeader: TextView
    private lateinit var tvFacilitiesCount: TextView

    private var selectedBuilding: String = "Building B"
    private var selectedFloor: Int? = 2

    // Master dataset of classrooms
    private val allClassesList = listOf(
        ClassroomUiModel(
            roomId = "101",
            roomName = "Room 101",
            buildingName = "Building A",
            floorNumber = 1,
            roomSubtitle = "Lecture Room • Building A • 1st Floor • West Wing",
            statusType = RoomStatusType.AVAILABLE,
            statusText = "• AVAILABLE",
            bannerTitle = "Free until 11:00 AM",
            bannerSubtitle = "Next: MATH 101 (11:00 - 12:30 PM)"
        ),
        ClassroomUiModel(
            roomId = "102",
            roomName = "Room 102",
            buildingName = "Building A",
            floorNumber = 1,
            roomSubtitle = "Science Lab A • Building A • 1st Floor • East Wing",
            statusType = RoomStatusType.OCCUPIED,
            statusText = "• OCCUPIED",
            bannerTitle = "PHYS 102 - Physics Lab",
            bannerSubtitle = "08:00 - 10:00 AM • Prof. Cruz"
        ),
        ClassroomUiModel(
            roomId = "201",
            roomName = "Room 201",
            buildingName = "Building A",
            floorNumber = 2,
            roomSubtitle = "Auditorium • Building A • 2nd Floor • Main Wing",
            statusType = RoomStatusType.AVAILABLE,
            statusText = "• AVAILABLE",
            bannerTitle = "Free All Day",
            bannerSubtitle = "No upcoming classes scheduled"
        ),
        ClassroomUiModel(
            roomId = "104",
            roomName = "Room 104",
            buildingName = "Building B",
            floorNumber = 1,
            roomSubtitle = "Computer Lab A • Building B • 1st Floor • South Wing",
            statusType = RoomStatusType.AVAILABLE,
            statusText = "• AVAILABLE",
            bannerTitle = "Free until 01:00 PM",
            bannerSubtitle = "Next: IT 201 (01:00 - 03:00 PM)"
        ),
        ClassroomUiModel(
            roomId = "204",
            roomName = "Room 204",
            buildingName = "Building B",
            floorNumber = 2,
            roomSubtitle = "Lecture Hall • Building B • 2nd Floor • North Wing",
            statusType = RoomStatusType.AVAILABLE,
            statusText = "• AVAILABLE",
            bannerTitle = "Free until 10:00 AM",
            bannerSubtitle = "Next: BSIT 1207 (10:00 - 11:30 AM)"
        ),
        ClassroomUiModel(
            roomId = "205",
            roomName = "Room 205",
            buildingName = "Building B",
            floorNumber = 2,
            roomSubtitle = "Computer Lab B • Building B • 2nd Floor • East Wing",
            statusType = RoomStatusType.OCCUPIED,
            statusText = "• OCCUPIED",
            bannerTitle = "CS 101 - Intro to Computing",
            bannerSubtitle = "09:00 - 11:00 AM • Prof. Santos"
        ),
        ClassroomUiModel(
            roomId = "206",
            roomName = "Room 206",
            buildingName = "Building B",
            floorNumber = 2,
            roomSubtitle = "Seminar Room • Building B • 2nd Floor • West Wing",
            statusType = RoomStatusType.UPCOMING,
            statusText = "• UPCOMING (In 15m)",
            bannerTitle = "BSIT 1208 - Discrete Math",
            bannerSubtitle = "Starts promptly at 10:30 AM"
        ),
        ClassroomUiModel(
            roomId = "301",
            roomName = "Room 301",
            buildingName = "Building B",
            floorNumber = 3,
            roomSubtitle = "Multipurpose Hall • Building B • 3rd Floor • Top Wing",
            statusType = RoomStatusType.AVAILABLE,
            statusText = "• AVAILABLE",
            bannerTitle = "Free until 02:00 PM",
            bannerSubtitle = "Next: ENG 101 (02:00 - 04:00 PM)"
        )
    )

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
        filterClassrooms()
    }

    private fun setupFilterListeners() {
        // Building Chip Listener
        chipGroupBuildings.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                selectedBuilding = checkedChip?.text?.toString() ?: "All Buildings"
            } else {
                selectedBuilding = "All Buildings"
            }
            filterClassrooms()
        }

        // Floor Chip Listener
        chipGroupFloors.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                val text = checkedChip?.text?.toString() ?: ""
                selectedFloor = when {
                    text.contains("Floor 1") -> 1
                    text.contains("Floor 2") -> 2
                    text.contains("Floor 3") -> 3
                    else -> null
                }
            } else {
                selectedFloor = null
            }
            filterClassrooms()
        }
    }

    private fun filterClassrooms() {
        val filteredList = allClassesList.filter { room ->
            val matchesBuilding = when (selectedBuilding) {
                "All Buildings" -> true
                else -> room.buildingName.equals(selectedBuilding, ignoreCase = true)
            }

            val matchesFloor = when (selectedFloor) {
                null -> true
                else -> room.floorNumber == selectedFloor
            }

            matchesBuilding && matchesFloor
        }

        // Update Headers
        tvSelectedBuildingHeader.text = selectedBuilding
        tvSelectedLevelHeader.text = if (selectedFloor != null) "Level $selectedFloor" else "All Levels"
        tvFacilitiesCount.text = "${filteredList.size} Facilities"

        adapter.updateClassrooms(filteredList)
    }
}
