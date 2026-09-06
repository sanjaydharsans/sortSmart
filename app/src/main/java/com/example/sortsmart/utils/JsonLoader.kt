package com.example.sortsmart.utils

import android.content.Context
import com.example.sortsmart.data.CollectionPoint
import com.example.sortsmart.data.WasteItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
object JsonLoader {
    fun loadWasteItems(context: Context): List<WasteItem> {
        return try {
            val json = context.assets.open("waste_items.json")
                .bufferedReader()
                .use { it.readText() }
            val type = object : TypeToken<List<WasteItem>>() {}.type

            Gson().fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun loadCollectionPoints(context: Context): List<CollectionPoint> {
        return try {
            val json = context.assets.open("collection_points.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<CollectionPoint>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}