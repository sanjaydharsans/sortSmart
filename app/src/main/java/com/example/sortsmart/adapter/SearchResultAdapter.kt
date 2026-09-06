package com.example.sortsmart.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.R
import com.example.sortsmart.data.WasteItem

class SearchResultAdapter(
    private var items: List<WasteItem>,
    private val onClick:(WasteItem) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon:TextView= view.findViewById(R.id.tvResultIcon)
        val tvName: TextView = view.findViewById(R.id.tvResultName)
        val tvCategory: TextView = view.findViewById(R.id.tvResultCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcon.text= categoryEmoji(item.category)
        holder.tvName.text = item.name
        holder.tvCategory.text = item.category
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<WasteItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun categoryEmoji(category: String): String = when (category) {
        "Plastic"-> "♻"
        "Glass"-> "🍾"
        "Paper"-> "📄"
        "Hazardous Waste" -> "⚠️"
        "Electronic"-> "🔌"
        "Organic"-> "🌱"
        "Metal" -> "🔩"
        "Textile"-> "👕"
        else -> "🗑"
    }
}