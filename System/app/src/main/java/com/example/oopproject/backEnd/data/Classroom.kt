package com.example.oopproject.backEnd.data

data class Classroom(
    val classroomId: String = "",
    val roomNumber: String = "",
    val buildingId: String = "",
    val floorId: String = "",
    val capacity: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)