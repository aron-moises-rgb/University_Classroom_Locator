package com.example.oopproject.backEnd.data
import com.example.oopproject.backEnd.data.enums.UserRole

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT
)