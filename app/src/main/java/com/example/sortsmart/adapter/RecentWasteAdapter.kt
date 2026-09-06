package com.example.sortsmart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.R
import com.example.sortsmart.data.DisposalRecord

class RecentWasteAdapter(
    private var items: List<DisposalRecord>,
    private val onClick: (DisposalRecord) -> Unit
) : RecyclerView.Adapter<RecentWasteAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvItemIcon)
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvCategory: TextView= view.findViewById(R.id.tvItemCategory)
        val tvCO2e: TextView = view.findViewById(R.id.tvItemCO2e)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_waste, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = items[position]
        holder.tvIcon.text= categoryEmoji(record.category)
        holder.tvName.text = record.wasteName
        holder.tvCategory.text = record.category
        holder.tvCO2e.text = String.format("+%.2f kg", record.co2eAvoided)
        holder.itemView.setOnClickListener { onClick(record) }
    }

    override fun getItemCount():Int = items.size

    fun updateData(newItems: List<DisposalRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun categoryEmoji(category: String): String = when (category) {
        "Plastic"-> "♻"
        "Glass" -> "🍾"
        "Paper"-> "📄"
        "Hazardous Waste" ->"⚠️"
        "Electronic"-> "🔌"
        "Organic"-> "🌱"
        "Metal" -> "🔩"
        "Textile"-> "👕"
        else -> "🗑"
    }
}