package com.example.oopproject.frontEnd.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Classroom


class AdminClassroomAdapter(
    items: List<Classroom> = emptyList(),
    private val onEditClick: (Classroom) -> Unit = {},
    private val onDeleteClick: (Classroom) -> Unit = {}
) : BaseAdapter<Classroom, AdminClassroomAdapter.AdminClassroomViewHolder>(items) {

    class AdminClassroomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        val tvRoomSubtitle: TextView = itemView.findViewById(R.id.tvRoomSubtitle)
        val tvRoomId: TextView = itemView.findViewById(R.id.tvRoomId)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditClassroom)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteClassroom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminClassroomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_classroom_card, parent, false)
        return AdminClassroomViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminClassroomViewHolder, position: Int) {
        val item = items[position]

        holder.tvRoomName.text = item.roomNumber.ifEmpty { "Classroom" }
        holder.tvRoomSubtitle.text = "Building: ${item.buildingId} • Floor ${item.floorId.ifEmpty { "1" }} • Capacity: ${item.capacity} seats"
        holder.tvRoomId.visibility = View.GONE

        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    fun updateClassrooms(newItems: List<Classroom>) {
        updateItems(newItems)
    }
}
