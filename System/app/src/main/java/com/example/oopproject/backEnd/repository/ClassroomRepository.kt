package com.example.oopproject.backEnd.repository

import com.example.oopproject.backEnd.data.Classroom
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ClassroomRepository {
    private val db = FirebaseFirestore.getInstance()
    private val classrooms = db.collection("classrooms")

    fun getClassrooms(
        onSuccess: (List<Classroom>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        classrooms.get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Classroom::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun listenToClassrooms(
        onUpdate: (List<Classroom>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return classrooms.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                onUpdate(snapshot.toObjects(Classroom::class.java))
            }
        }
    }

    fun getClassroomsByBuilding(
        buildingId: String,
        onSuccess: (List<Classroom>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        classrooms.whereEqualTo("buildingId", buildingId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Classroom::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun getClassroomById(
        classroomId: String,
        onSuccess: (Classroom?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        classrooms.document(classroomId).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(Classroom::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addClassroom(
        classroom: Classroom,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = classrooms.document()

        document.set(classroom.copy(classroomId = document.id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateClassroom(
        classroom: Classroom,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        classrooms.document(classroom.classroomId).set(classroom)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteClassroom(
        classroomId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        classrooms.document(classroomId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
