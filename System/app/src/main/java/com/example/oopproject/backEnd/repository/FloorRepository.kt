package com.example.oopproject.backEnd.repository

import com.example.oopproject.backEnd.data.Floor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FloorRepository {
    private val db = FirebaseFirestore.getInstance()
    private val floors = db.collection("floors")

    fun listenToFloors(
        onUpdate: (List<Floor>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return floors.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                onUpdate(snapshot.toObjects(Floor::class.java))
            }
        }
    }

    fun getFloorsByBuilding(
        buildingId: String,
        onSuccess: (List<Floor>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        floors.whereEqualTo("buildingId", buildingId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Floor::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addFloor(
        floor: Floor,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = floors.document()

        document.set(floor.copy(floorId = document.id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateFloor(
        floor: Floor,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        floors.document(floor.floorId).set(floor)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteFloor(
        floorId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        floors.document(floorId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
