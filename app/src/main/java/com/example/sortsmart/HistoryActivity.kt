package com.example.sortsmart

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.adapter.RecentWasteAdapter
import com.example.sortsmart.utils.PrefsManager
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHistory)
        val rv= findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvHistoryEmpty)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        val records = PrefsManager.getRecords(this)
        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            rv.layoutManager= LinearLayoutManager(this)
            rv.adapter = RecentWasteAdapter(records) { }
        }
    }
}