package com.example.sortsmart.data
data class CollectionPoint(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val acceptedCategories: List<String>
)