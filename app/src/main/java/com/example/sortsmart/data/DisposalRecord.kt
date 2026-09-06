package com.example.sortsmart.data
data class DisposalRecord(
    val wasteName: String,
    val category: String,
    val pointName: String,
    val distanceMeters: Float,
    val co2eAvoided: Double,
    val timestamp: Long
)