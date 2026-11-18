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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.constants.CampusBuildings
import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.repository.BuildingRepository
import com.example.oopproject.backEnd.repository.ClassroomRepository
import com.example.oopproject.databinding.DialogAddEditClassroomBinding
import com.example.oopproject.frontEnd.adapters.AdminClassroomAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ListenerRegistration

class ManageClassroomsFragment : Fragment() {

    private lateinit var rvClassrooms: RecyclerView
    private lateinit var btnAddClassroom: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var chipGroupBuildingFilter: ChipGroup

    private lateinit var adapter: AdminClassroomAdapter
    private val classroomRepo = ClassroomRepository()
    private val buildingRepo = BuildingRepository()

    private var classroomsListener: ListenerRegistration? = null
    private var rawClassroomsList = listOf<Classroom>()
    private var selectedBuildingFilter = "All Buildings"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_manage_classrooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvClassrooms = view.findViewById(R.id.rvClassrooms)
        btnAddClassroom = view.findViewById(R.id.btnAddClassroom)
        btnBack = view.findViewById(R.id.btnBack)
        chipGroupBuildingFilter = view.findViewById(R.id.chipGroupBuildingFilter)

        rvClassrooms.layoutManager = LinearLayoutManager(requireContext())

        adapter = AdminClassroomAdapter(
            items = emptyList(),
            onEditClick = { room ->
                showAddEditClassroomDialog(room)
            },
            onDeleteClick = { room ->
                confirmDeleteClassroom(room)
            }
        )
        rvClassrooms.adapter = adapter

        btnBack.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.replaceFragment(AdminDashboardFragment())
        }

        btnAddClassroom.setOnClickListener {
            showAddEditClassroomDialog(null)
        }

        setupBuildingFilterChips()

        // Start Realtime Listener for Classrooms in Firestore
        classroomsListener = classroomRepo.listenToClassrooms(
            onUpdate = { classroomsFromDb ->
                rawClassroomsList = classroomsFromDb
                filterClassroomsList()
            },
            onError = { e ->
                Log.e("ManageClassrooms", "Firestore permission error: ${e.message}")
            }
        )
    }

    private fun setupBuildingFilterChips() {
        chipGroupBuildingFilter.removeAllViews()

        val buildingOptions = listOf("All Buildings") + CampusBuildings.names()
        var defaultChipId: Int? = null

        buildingOptions.forEach { bName ->
            val chip = Chip(requireContext()).apply {
                text = bName
                isCheckable = true
                isClickable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
                chipStrokeWidth = 0f
            }

            if (bName.equals(selectedBuildingFilter, ignoreCase = true)) {
                defaultChipId = chip.id
            }

            chipGroupBuildingFilter.addView(chip)
        }

        if (defaultChipId != null) {
            chipGroupBuildingFilter.check(defaultChipId!!)
        }

        chipGroupBuildingFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                selectedBuildingFilter = checkedChip?.text?.toString() ?: "All Buildings"
            } else {
                selectedBuildingFilter = "All Buildings"
            }
            filterClassroomsList()
        }
    }

    private fun filterClassroomsList() {
        val filtered = if (selectedBuildingFilter.trim().equals("All Buildings", ignoreCase = true)) {
            rawClassroomsList
        } else {
            rawClassroomsList.filter { room ->
                room.buildingId.trim().equals(selectedBuildingFilter.trim(), ignoreCase = true) ||
                room.roomNumber.trim().contains(selectedBuildingFilter.trim(), ignoreCase = true)
            }
        }
        adapter.updateClassrooms(filtered)
    }

    private fun showAddEditClassroomDialog(existingClassroom: Classroom?) {
        val dialogBinding = DialogAddEditClassroomBinding.inflate(layoutInflater)

        // Populate Building Dropdown from static CampusBuildings constants
        val dropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            CampusBuildings.names()
        )
        dialogBinding.etBuildingId.setAdapter(dropdownAdapter)

        if (existingClassroom != null) {
            dialogBinding.tvDialogTitle.text = "Edit Classroom"
            dialogBinding.etRoomNumber.setText(existingClassroom.roomNumber)
            dialogBinding.etBuildingId.setText(existingClassroom.buildingId, false)
            dialogBinding.etFloorId.setText(existingClassroom.floorId.ifEmpty { "1" })
            if (existingClassroom.capacity > 0) {
                dialogBinding.etCapacity.setText(existingClassroom.capacity.toString())
            }
        } else {
            dialogBinding.tvDialogTitle.text = "Add Classroom"
            dialogBinding.etFloorId.setText("1")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val roomNum = dialogBinding.etRoomNumber.text.toString().trim()
            val bldgId = dialogBinding.etBuildingId.text.toString().trim()
            val floorStr = dialogBinding.etFloorId.text.toString().trim().ifEmpty { "1" }
            val capStr = dialogBinding.etCapacity.text.toString().trim()

            if (roomNum.isEmpty()) {
                dialogBinding.tilRoomNumber.error = "Room number is required"
                return@setOnClickListener
            }

            val cap = capStr.toIntOrNull() ?: 40

            dialogBinding.btnSave.isEnabled = false

            if (existingClassroom != null) {
                // Update
                val updated = existingClassroom.copy(
                    roomNumber = roomNum,
                    buildingId = bldgId,
                    floorId = floorStr,
                    capacity = cap
                )
                classroomRepo.updateClassroom(
                    classroom = updated,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Classroom updated!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onError = { e ->
                        dialogBinding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Add
                val newClassroom = Classroom(
                    roomNumber = roomNum,
                    buildingId = bldgId,
                    floorId = floorStr,
                    capacity = cap
                )
                classroomRepo.addClassroom(
                    classroom = newClassroom,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Classroom added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onError = { e ->
                        dialogBinding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Add failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun confirmDeleteClassroom(classroom: Classroom) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Classroom")
            .setMessage("Are you sure you want to delete ${classroom.roomNumber}?")
            .setPositiveButton("Delete") { _, _ ->
                classroomRepo.deleteClassroom(
                    classroomId = classroom.classroomId,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Classroom deleted!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classroomsListener?.remove()
    }
}
