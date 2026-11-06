package com.example.oopproject.backEnd.repository
import com.example.oopproject.backEnd.data.Building
import com.google.firebase.firestore.FirebaseFirestore

class BuildingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val buildings = db.collection("buildings")

    fun getBuildings(
        onSuccess: (List<Building>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        buildings.get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Building::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addBuilding(
        building: Building,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = buildings.document()

        document.set(building.copy(buildingId = document.id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateBuilding(
        building: Building,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        buildings.document(building.buildingId).set(building)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteBuilding(
        buildingId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        buildings.document(buildingId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}