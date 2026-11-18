package com.example.oopproject.backEnd.service

import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.example.oopproject.backEnd.data.enums.RoomStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class AvailabilityResult(
    val status: RoomStatus,
    val bannerTitle: String,
    val bannerSubtitle: String
)

/**
 * AvailabilityService implements RoomAvailabilityEvaluator interface,
 * demonstrating OOP Interface Implementation and Polymorphism.
 */
class AvailabilityService : RoomAvailabilityEvaluator {

    override fun evaluateRoomStatus(classroom: Classroom, schedules: List<Schedule>): RoomStatus {
        val roomSchedules = schedules.filter { isScheduleForRoom(it, classroom.classroomId, classroom.roomNumber) }
        val result = calculateAvailability(roomSchedules)
        return result.status
    }

    fun getCurrentDayOfWeek(): DayOfWeek {
        val zone = ZoneId.of("Asia/Manila")
        val dayName = LocalDate.now(zone).dayOfWeek.name
        return try {
            DayOfWeek.valueOf(dayName)
        } catch (_: Exception) {
            DayOfWeek.WEDNESDAY
        }
    }

    fun isScheduleForDay(schedule: Schedule, targetDay: DayOfWeek): Boolean {
        return schedule.day == targetDay || 
               schedule.day.name.equals(targetDay.name, ignoreCase = true)
    }

    fun isScheduleForRoom(schedule: Schedule, roomId: String, roomNumber: String): Boolean {
        val sId = schedule.classroomId.trim().lowercase()
        val rId = roomId.trim().lowercase()
        val rNum = roomNumber.trim().lowercase()

        if (sId.isEmpty()) return false
        return sId == rId || sId == rNum || 
               (rId.isNotEmpty() && rId.contains(sId)) || 
               (rNum.isNotEmpty() && (rNum.contains(sId) || sId.contains(rNum)))
    }

    fun calculateAvailability(
        schedules: List<Schedule>,
        currentDay: DayOfWeek = getCurrentDayOfWeek(),
        currentTime: LocalTime = LocalTime.now()
    ): AvailabilityResult {

        // 0. Check Campus Operating Hours (07:00 AM - 08:00 PM)
        val openingTime = LocalTime.of(7, 0)
        val closingTime = LocalTime.of(20, 0)

        if (!currentTime.isBefore(closingTime) || currentTime.isBefore(openingTime)) {
            return AvailabilityResult(
                status = RoomStatus.CLOSED,
                bannerTitle = "Campus Closed",
                bannerSubtitle = "Outside Operating Hours (07:00 AM - 08:00 PM)"
            )
        }

        val todaysSchedules = schedules.filter { isScheduleForDay(it, currentDay) }

        // 1. Check if room is currently OCCUPIED
        for (schedule in todaysSchedules) {
            val start = parseTime(schedule.startTime) ?: continue
            val end = parseTime(schedule.endTime) ?: continue

            if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                return AvailabilityResult(
                    status = RoomStatus.OCCUPIED,
                    bannerTitle = schedule.subject.ifEmpty { "Class in Session" },
                    bannerSubtitle = "${schedule.startTime} - ${schedule.endTime} • ${schedule.instructor.ifEmpty { "Instructor" }}"
                )
            }
        }

        // 2. Check if a class is UPCOMING within 15 minutes
        for (schedule in todaysSchedules) {
            val start = parseTime(schedule.startTime) ?: continue
            val upcomingThreshold = start.minusMinutes(15)

            if (!currentTime.isBefore(upcomingThreshold) && currentTime.isBefore(start)) {
                return AvailabilityResult(
                    status = RoomStatus.UPCOMING,
                    bannerTitle = schedule.subject.ifEmpty { "Upcoming Class" },
                    bannerSubtitle = "Starts at ${schedule.startTime} • ${schedule.instructor.ifEmpty { "Instructor" }}"
                )
            }
        }

        // 3. Otherwise room is AVAILABLE -> find next upcoming class today
        val upcomingToday = todaysSchedules.mapNotNull { schedule ->
            val start = parseTime(schedule.startTime) ?: return@mapNotNull null
            if (start.isAfter(currentTime)) {
                Pair(start, schedule)
            } else {
                null
            }
        }.minByOrNull { it.first }

        return if (upcomingToday != null) {
            val nextSchedule = upcomingToday.second
            AvailabilityResult(
                status = RoomStatus.AVAILABLE,
                bannerTitle = "Free until ${nextSchedule.startTime}",
                bannerSubtitle = "Next: ${nextSchedule.subject} (${nextSchedule.startTime} - ${nextSchedule.endTime})"
            )
        } else {
            AvailabilityResult(
                status = RoomStatus.AVAILABLE,
                bannerTitle = "Free All Day",
                bannerSubtitle = "No upcoming classes scheduled"
            )
        }
    }

    private fun parseTime(timeStr: String): LocalTime? {
        if (timeStr.isBlank()) return null
        return try {
            val clean = timeStr.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsOnly = clean.replace(Regex("[^0-9:]"), "")
            val parts = digitsOnly.split(":")
            var hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0

            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0

            LocalTime.of(hour, min)
        } catch (_: Exception) {
            null
        }
    }
}
