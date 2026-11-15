package com.example.oopproject.backEnd.util

import com.example.oopproject.backEnd.data.Classroom
import com.example.oopproject.backEnd.data.Schedule
import com.example.oopproject.backEnd.data.enums.DayOfWeek
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleSeeder {

    private val db = FirebaseFirestore.getInstance()
    private val schedulesCollection = db.collection("schedules")

    fun seedSchedulesFromRealClassrooms(
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("classrooms").get().addOnSuccessListener { roomResult ->
            val classroomsList = roomResult.toObjects(Classroom::class.java)
            if (classroomsList.isEmpty()) {
                onSuccess(0)
                return@addOnSuccessListener
            }

            val subjectsAndInstructors = listOf(
                Pair("CS 101 - Intro to Computing", "Prof. Santos"),
                Pair("MATH 101 - Calculus I", "Prof. Cruz"),
                Pair("ENG 101 - Technical Writing", "Dr. Reyes"),
                Pair("PHYS 102 - Physics Lab", "Dr. Reyes"),
                Pair("IT 201 - Data Structures", "Prof. Alan Turing"),
                Pair("BSIT 1207 - Discrete Mathematics", "Prof. Santos"),
                Pair("BSIT 1208 - Object-Oriented Programming", "Prof. Grace Hopper"),
                Pair("WEB 101 - Web Development", "Prof. Santos"),
                Pair("NET 201 - Computer Networks", "Prof. Alan Turing"),
                Pair("DB 101 - Database Systems", "Prof. Grace Hopper"),
                Pair("SYS 202 - Systems Analysis", "Prof. Cruz"),
                Pair("CYBER 101 - Cybersecurity", "Prof. Alan Turing"),
                Pair("AI 301 - Artificial Intelligence", "Prof. Grace Hopper"),
                Pair("CAP 401 - Capstone Project", "Dr. Reyes"),
                Pair("SAT 101 - Review Class", "Prof. Santos"),
                Pair("ROBO 201 - Robotics Workshop", "Prof. Alan Turing"),
                Pair("SUN 101 - Open Lab Session", "Prof. Grace Hopper")
            )

            val days = DayOfWeek.values()
            val timeSlots = listOf(
                Pair("08:00", "10:00"),
                Pair("09:00", "11:00"),
                Pair("10:00", "12:00"),
                Pair("11:00", "13:00"),
                Pair("13:00", "15:00"),
                Pair("14:00", "16:00"),
                Pair("15:00", "17:00")
            )

            var count = 0
            val batch = db.batch()

            classroomsList.forEachIndexed { rIdx, classroom ->
                val targetRoomId = classroom.roomNumber.ifEmpty { classroom.classroomId }

                // Assign 2 to 3 schedules per room spread across Monday - Sunday
                for (i in 0..2) {
                    val subInst = subjectsAndInstructors[(rIdx * 3 + i) % subjectsAndInstructors.size]
                    val day = days[(rIdx + i * 2) % days.size]
                    val time = timeSlots[(rIdx + i) % timeSlots.size]

                    val docRef = schedulesCollection.document()
                    val newSched = Schedule(
                        scheduleId = docRef.id,
                        classroomId = targetRoomId,
                        subject = subInst.first,
                        instructor = subInst.second,
                        day = day,
                        startTime = time.first,
                        endTime = time.second
                    )
                    batch.set(docRef, newSched)
                    count++
                }
            }

            batch.commit()
                .addOnSuccessListener { onSuccess(count) }
                .addOnFailureListener { onError(it) }
        }.addOnFailureListener { onError(it) }
    }
}
