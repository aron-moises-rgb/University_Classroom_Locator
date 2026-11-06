package com.example.oopproject.backEnd.repository
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleRepository {
    private val db = FirebaseFirestore.getInstance()
    private val schedules = db.collection("schedules")

    fun getSchedulesByClassroom(
        classroomId: String,
        onSuccess: (List<Schedule>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        schedules.whereEqualTo("classroomId", classroomId).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Schedule::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun getSchedulesByDay(
        day: DayOfWeek,
        onSuccess: (List<Schedule>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        schedules.whereEqualTo("day", day.name).get()
            .addOnSuccessListener { result ->
                onSuccess(result.toObjects(Schedule::class.java))
            }
            .addOnFailureListener { onError(it) }
    }

    fun addSchedule(
        schedule: Schedule,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = schedules.document()

        document.set(schedule.copy(scheduleId = document.id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateSchedule(
        schedule: Schedule,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        schedules.document(schedule.scheduleId).set(schedule)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteSchedule(
        scheduleId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        schedules.document(scheduleId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}