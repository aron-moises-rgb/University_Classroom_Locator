package com.example.oopproject.backEnd.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Abstract Base Repository demonstrating OOP Principles:
 * 1. Abstraction: Defines common data operations while leaving specific document parsing abstract.
 * 2. Encapsulation: Protects Firestore instance and collection reference.
 * 3. Inheritance & Polymorphism: Subclasses inherit shared CRUD behavior and implement custom data mapping.
 */
abstract class BaseRepository<T>(
    protected val collectionPath: String
) {
    protected val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    protected val collectionRef = db.collection(collectionPath)

    /**
     * Abstract method enforcing polymorphic parsing for each data entity type.
     */
    abstract fun parseDocument(doc: DocumentSnapshot): T?

    fun addEntity(
        id: String,
        entity: Any,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collectionRef.document(id).set(entity)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteEntity(
        id: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collectionRef.document(id).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
