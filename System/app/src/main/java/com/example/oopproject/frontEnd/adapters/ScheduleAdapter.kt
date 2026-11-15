package com.example.oopproject.frontEnd.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Schedule

class ScheduleAdapter(
    private var items: List<Schedule> = emptyList(),
    private val onEditClick: (Schedule) -> Unit = {},
    private val onDeleteClick: (Schedule) -> Unit = {}
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubjectCode: TextView = itemView.findViewById(R.id.tvSubjectCode)
        val tvInstructor: TextView = itemView.findViewById(R.id.tvInstructor)
        val tvRoomAndDay: TextView = itemView.findViewById(R.id.tvRoomAndDay)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditSchedule)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSchedule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_schedule_card, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = items[position]

        holder.tvSubjectCode.text = item.subject.ifEmpty { "Subject" }
        holder.tvInstructor.text = "Instructor: ${item.instructor.ifEmpty { "Unassigned" }}"
        holder.tvRoomAndDay.text = "Room: ${item.classroomId.ifEmpty { "General" }} • ${item.day.name} • ${item.startTime} - ${item.endTime}"

        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateSchedules(newItems: List<Schedule>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
