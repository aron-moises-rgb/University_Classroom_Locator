package com.example.oopproject.frontEnd.models

enum class RoomStatusType {
    AVAILABLE,
    OCCUPIED,
    UPCOMING
}

data class ClassroomUiModel(
    val roomId: String,
    val roomName: String,
    val buildingName: String = "Building B",
    val floorNumber: Int = 2,
    val roomSubtitle: String,
    val statusType: RoomStatusType,
    val statusText: String,
    val bannerTitle: String,
    val bannerSubtitle: String
)
