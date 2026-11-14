package com.example.oopproject.frontEnd.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oopproject.R
import com.example.oopproject.backEnd.data.Building

class BuildingAdapter(
    private var items: List<Building> = emptyList(),
    private val onEditClick: (Building) -> Unit = {},
    private val onDeleteClick: (Building) -> Unit = {}
) : RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {

    class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBuildingName: TextView = itemView.findViewById(R.id.tvBuildingName)
        val tvBuildingDesc: TextView = itemView.findViewById(R.id.tvBuildingDesc)
        val tvBuildingGps: TextView = itemView.findViewById(R.id.tvBuildingGps)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditBuilding)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteBuilding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_building_card, parent, false)
        return BuildingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val item = items[position]

        holder.tvBuildingName.text = item.name.ifEmpty { "Unnamed Building" }
        holder.tvBuildingDesc.text = item.description.ifEmpty { "No description provided" }
        holder.tvBuildingGps.text = "GPS: ${item.latitude}, ${item.longitude}"

        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateBuildings(newItems: List<Building>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
