package com.example.oopproject.backEnd.data
import com.example.oopproject.backEnd.data.enums.DayOfWeek

data class Schedule(
    val scheduleId: String = "",
    val classroomId: String = "",
    val subject: String = "",
    val section: String = "",
    val instructor: String = "",
    val day: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: String = "",
    val endTime: String = ""
)