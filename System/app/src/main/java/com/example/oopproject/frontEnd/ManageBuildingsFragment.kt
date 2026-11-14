package com.example.oopproject.frontEnd

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Building
import com.example.oopproject.backEnd.repository.BuildingRepository
import com.example.oopproject.databinding.DialogAddEditBuildingBinding
import com.example.oopproject.frontEnd.adapters.BuildingAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration

class ManageBuildingsFragment : Fragment() {

    private lateinit var rvBuildings: RecyclerView
    private lateinit var btnAddBuilding: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: BuildingAdapter
    private val buildingRepo = BuildingRepository()
    private var buildingsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_manage_buildings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvBuildings = view.findViewById(R.id.rvBuildings)
        btnAddBuilding = view.findViewById(R.id.btnAddBuilding)
        btnBack = view.findViewById(R.id.btnBack)

        rvBuildings.layoutManager = LinearLayoutManager(requireContext())

        adapter = BuildingAdapter(
            items = emptyList(),
            onEditClick = { building ->
                showAddEditBuildingDialog(building)
            },
            onDeleteClick = { building ->
                confirmDeleteBuilding(building)
            }
        )
        rvBuildings.adapter = adapter

        btnBack.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.replaceFragment(AdminDashboardFragment())
        }

        btnAddBuilding.setOnClickListener {
            showAddEditBuildingDialog(null)
        }

        // Start Realtime Listener for Buildings in Firestore
        buildingsListener = buildingRepo.listenToBuildings(
            onUpdate = { buildingsList ->
                adapter.updateBuildings(buildingsList)
            },
            onError = { e ->
                Log.e("ManageBuildings", "Firestore permission error: ${e.message}")
            }
        )
    }

    private fun showAddEditBuildingDialog(existingBuilding: Building?) {
        val dialogBinding = DialogAddEditBuildingBinding.inflate(layoutInflater)

        if (existingBuilding != null) {
            dialogBinding.tvDialogTitle.text = "Edit Building"
            dialogBinding.etBuildingName.setText(existingBuilding.name)
            dialogBinding.etDescription.setText(existingBuilding.description)
            if (existingBuilding.latitude != 0.0) {
                dialogBinding.etLatitude.setText(existingBuilding.latitude.toString())
            }
            if (existingBuilding.longitude != 0.0) {
                dialogBinding.etLongitude.setText(existingBuilding.longitude.toString())
            }
        } else {
            dialogBinding.tvDialogTitle.text = "Add Building"
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etBuildingName.text.toString().trim()
            val desc = dialogBinding.etDescription.text.toString().trim()
            val latStr = dialogBinding.etLatitude.text.toString().trim()
            val lngStr = dialogBinding.etLongitude.text.toString().trim()

            if (name.isEmpty()) {
                dialogBinding.tilBuildingName.error = "Building name is required"
                return@setOnClickListener
            }

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            dialogBinding.btnSave.isEnabled = false

            if (existingBuilding != null) {
                // Update
                val updated = existingBuilding.copy(
                    name = name,
                    description = desc,
                    latitude = lat,
                    longitude = lng
                )
                buildingRepo.updateBuilding(
                    building = updated,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Building updated!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onError = { e ->
                        dialogBinding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Add
                val newBuilding = Building(
                    name = name,
                    description = desc,
                    latitude = lat,
                    longitude = lng
                )
                buildingRepo.addBuilding(
                    building = newBuilding,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Building added!", Toast.LENGTH_SHORT).show()
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

    private fun confirmDeleteBuilding(building: Building) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Building")
            .setMessage("Are you sure you want to delete ${building.name}?")
            .setPositiveButton("Delete") { _, _ ->
                buildingRepo.deleteBuilding(
                    buildingId = building.buildingId,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Building deleted!", Toast.LENGTH_SHORT).show()
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
        buildingsListener?.remove()
    }
}
