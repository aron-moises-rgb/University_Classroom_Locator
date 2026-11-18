package com.example.oopproject.backEnd.service

import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.RoomStatus

interface RoomAvailabilityEvaluator {
    fun evaluateRoomStatus(classroom: Classroom, schedules: List<Schedule>): RoomStatus
}
