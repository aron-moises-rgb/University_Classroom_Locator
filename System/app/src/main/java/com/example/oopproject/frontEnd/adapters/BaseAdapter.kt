package com.example.oopproject.frontEnd.adapters

import androidx.recyclerview.widget.RecyclerView


abstract class BaseAdapter<T, VH : RecyclerView.ViewHolder>(
    protected var items: List<T> = emptyList()
) : RecyclerView.Adapter<VH>() {

    override fun getItemCount(): Int = items.size

    open fun updateItems(newItems: List<T>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
