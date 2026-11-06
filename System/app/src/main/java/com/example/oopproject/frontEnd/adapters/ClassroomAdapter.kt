package com.example.oopproject.frontEnd.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.frontEnd.models.ClassroomUiModel
import com.example.oopproject.frontEnd.models.RoomStatusType
import com.google.android.material.button.MaterialButton

class ClassroomAdapter(
    private var items: List<ClassroomUiModel> = emptyList(),
    private val onDetailsClick: (ClassroomUiModel) -> Unit = {},
    private val onViewInfoClick: (ClassroomUiModel) -> Unit = {}
) : RecyclerView.Adapter<ClassroomAdapter.ClassroomViewHolder>() {

    class ClassroomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvRoomSubtitle: TextView = itemView.findViewById(R.id.tvRoomSubtitle)
        val tvBannerTitle: TextView = itemView.findViewById(R.id.tvBannerTitle)
        val tvBannerSubtitle: TextView = itemView.findViewById(R.id.tvBannerSubtitle)
        val btnDetails: MaterialButton = itemView.findViewById(R.id.btnDetails)
        val btnViewInfo: MaterialButton = itemView.findViewById(R.id.btnViewInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassroomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_classroom_card, parent, false)
        return ClassroomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassroomViewHolder, position: Int) {
        val item = items[position]

        holder.tvRoomName.text = item.roomName
        holder.tvRoomSubtitle.text = item.roomSubtitle
        holder.tvStatusBadge.text = item.statusText
        holder.tvBannerTitle.text = item.bannerTitle
        holder.tvBannerSubtitle.text = item.bannerSubtitle

        when (item.statusType) {
            RoomStatusType.AVAILABLE -> {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
            }
            RoomStatusType.OCCUPIED -> {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
            }
            RoomStatusType.UPCOMING -> {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#DBEAFE"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#1D4ED8"))
            }
        }

        holder.btnDetails.setOnClickListener { onDetailsClick(item) }
        holder.btnViewInfo.setOnClickListener { onViewInfoClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateClassrooms(newItems: List<ClassroomUiModel>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
