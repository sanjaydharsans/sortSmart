package com.example.sortsmart.utils

import android.content.Context
import com.example.sortsmart.data.DisposalRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
object PrefsManager {

    private const val PREFS_NAME ="sortsmart_prefs"
    private const val KEY_RECORDS= "disposal_records"
    private const val KEY_WALK_TRACKING ="walk_tracking_enabled"
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun getRecords(context: Context): List<DisposalRecord> {
        val json = prefs(context).getString(KEY_RECORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<DisposalRecord>>() {}.type
        return Gson().fromJson<List<DisposalRecord>>(json, type)
            .sortedByDescending { it.timestamp }
    }
    fun saveRecord(context: Context, record: DisposalRecord) {
        val current = getRecords(context).toMutableList()
        current.add(0, record)
        prefs(context).edit().putString(KEY_RECORDS, Gson().toJson(current)).apply()
    }
    fun getTotalCO2e(context: Context): Double = getRecords(context).sumOf { it.co2eAvoided }

    fun getTotalItems(context: Context): Int = getRecords(context).size

    fun getRecentRecords(context: Context, limit: Int = 5): List<DisposalRecord> =
        getRecords(context).take(limit)

    fun clearRecords(context: Context) = prefs(context).edit().remove(KEY_RECORDS).apply()

    fun isWalkTrackingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WALK_TRACKING, true)

    fun setWalkTrackingEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_WALK_TRACKING, enabled).apply()
}