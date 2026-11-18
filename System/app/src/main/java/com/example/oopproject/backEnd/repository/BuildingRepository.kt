package com.example.oopproject.backEnd.repository

import com.example.oopproject.backEnd.data.Building
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration

/**
 * BuildingRepository inherits from abstract BaseRepository<Building>,
 * demonstrating OOP Inheritance, Polymorphism (implementing parseDocument), and Code Reuse.
 */
class BuildingRepository : BaseRepository<Building>("buildings") {

    override fun parseDocument(doc: DocumentSnapshot): Building? {
        return doc.toObject(Building::class.java)
    }

    fun getBuildings(
        onSuccess: (List<Building>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collectionRef.get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { parseDocument(it) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun listenToBuildings(
        onUpdate: (List<Building>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { parseDocument(it) }
                onUpdate(list)
            }
        }
    }

    fun addBuilding(
        building: Building,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = collectionRef.document()
        addEntity(document.id, building.copy(buildingId = document.id), onSuccess, onError)
    }

    fun updateBuilding(
        building: Building,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        addEntity(building.buildingId, building, onSuccess, onError)
    }

    fun deleteBuilding(
        buildingId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        deleteEntity(buildingId, onSuccess, onError)
    }
}
