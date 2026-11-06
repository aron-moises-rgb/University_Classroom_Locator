package com.example.oopproject.service
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.example.oopproject.backEnd.data.enums.RoomStatus
import java.time.LocalTime

class AvailabilityService {

    fun checkAvailability(
        schedules: List<Schedule>,
        currentDay: DayOfWeek,
        currentTime: LocalTime
    ): RoomStatus {

        for (schedule in schedules) {

            if (schedule.day != currentDay) {
                continue
            }

            val startTime =
                LocalTime.parse(schedule.startTime)

            val endTime =
                LocalTime.parse(schedule.endTime)

            if (!currentTime.isBefore(startTime) &&
                currentTime.isBefore(endTime)
            ) {
                return RoomStatus.OCCUPIED
            }
        }

        return RoomStatus.AVAILABLE
    }
}