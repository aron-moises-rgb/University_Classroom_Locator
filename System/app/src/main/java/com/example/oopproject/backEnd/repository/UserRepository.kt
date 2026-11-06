package com.example.oopproject.backEnd.repository
import com.example.oopproject.backEnd.data.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    fun getUserById(
        userId: String,
        onSuccess: (User?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        users.document(userId).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(User::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addUser(
        user: User,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        users.document(user.userId).set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateUser(
        user: User,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        users.document(user.userId).set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}