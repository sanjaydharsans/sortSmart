package com.example.sortsmart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.R
import com.example.sortsmart.data.CollectionPoint
import com.google.android.material.button.MaterialButton

class CollectionPointAdapter(
    private var items: List<CollectionPoint>,
    private var distances: List<Float>,
    private val onSelect: (CollectionPoint, Float) -> Unit,
    private val onMapClick:(CollectionPoint) -> Unit
) : RecyclerView.Adapter<CollectionPointAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:TextView = view.findViewById(R.id.tvPointName)
        val tvDistance:TextView= view.findViewById(R.id.tvPointDistance)
        val tvCategories:TextView = view.findViewById(R.id.tvPointCategories)
        val btnMaps: MaterialButton= view.findViewById(R.id.btnOpenMaps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_point, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val point = items[position]
        val distance = distances[position]
        holder.tvName.text = point.name
        holder.tvDistance.text= formatDistance(distance)
        holder.tvCategories.text = point.acceptedCategories.joinToString(" · ")
        holder.itemView.setOnClickListener { onSelect(point, distance) }
        holder.btnMaps.setOnClickListener { onMapClick(point) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CollectionPoint>, newDistances: List<Float>) {
        items= newItems
        distances = newDistances
        notifyDataSetChanged()
    }

    private fun formatDistance(metres: Float):String =
        if (metres < 1000) "${metres.toInt()} m away"
        else String.format("%.1f km away", metres / 1000f)
}