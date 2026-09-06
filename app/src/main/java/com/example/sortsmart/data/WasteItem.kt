package com.example.sortsmart.data
data class WasteItem(
    val id: Int,
    val name: String,
    val category: String,
    val disposalMethod: String,
    val warning: String,
    val co2eAvoided: Double
)