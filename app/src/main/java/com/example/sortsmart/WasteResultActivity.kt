package com.example.sortsmart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sortsmart.data.WasteItem
import com.example.sortsmart.utils.JsonLoader
import com.example.sortsmart.views.EcoImpactView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson

class WasteResultActivity : AppCompatActivity() {

    private lateinit var tvWasteIcon:TextView
    private lateinit var tvWasteName:TextView
    private lateinit var tvCategoryChip:TextView
    private lateinit var tvDisposalMethod:TextView
    private lateinit var tvCO2eBenefit: TextView
    private lateinit var cardWarning:MaterialCardView
    private lateinit var tvWarning:TextView
    private lateinit var ecoImpactView:EcoImpactView
    private lateinit var btnFindPoint:MaterialButton

    private var wasteItem:WasteItem? = null
    private var walkTrackingEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waste_result)
        bindViews()
        setupToolbar()
        readIntent()
        populateUI()
        setupButtons()
    }

    private fun bindViews() {
        tvWasteIcon = findViewById(R.id.tvWasteIcon)
        tvWasteName = findViewById(R.id.tvWasteName)
        tvCategoryChip = findViewById(R.id.tvCategoryChip)
        tvDisposalMethod = findViewById(R.id.tvDisposalMethod)
        tvCO2eBenefit = findViewById(R.id.tvCO2eBenefit)
        cardWarning= findViewById(R.id.cardWarning)
        tvWarning =findViewById(R.id.tvWarning)
        ecoImpactView = findViewById(R.id.ecoImpactView)
        btnFindPoint= findViewById(R.id.btnFindPoint)
    }

    private fun setupToolbar() {
        val toolbar= findViewById<MaterialToolbar>(R.id.toolbarWasteResult)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun readIntent() {
        val wasteId =intent.getIntExtra(MainActivity.EXTRA_WASTE_ID, -1)
        walkTrackingEnabled = intent.getBooleanExtra(MainActivity.EXTRA_WALK_TRACKING, true)
        if (wasteId == -1) { finish(); return }
        wasteItem =JsonLoader.loadWasteItems(this).find { it.id == wasteId }
        if (wasteItem == null) finish()
    }

    private fun populateUI() {
        val item = wasteItem ?: return
        tvWasteIcon.text= categoryEmoji(item.category)
        tvWasteName.text = item.name
        tvCategoryChip.text = item.category
        tvDisposalMethod.text = item.disposalMethod
        tvCO2eBenefit.text = String.format("%.2f kg", item.co2eAvoided)
        cardWarning.visibility = if (item.warning.isBlank()) View.GONE else View.VISIBLE
        tvWarning.text = item.warning
        ecoImpactView.category = item.category
        ecoImpactView.co2eValue = item.co2eAvoided
    }

    private fun setupButtons() {
        btnFindPoint.setOnClickListener {
            val item = wasteItem ?: return@setOnClickListener
            startActivity(Intent(this, EcoWalkActivity::class.java).apply {
                putExtra(EcoWalkActivity.EXTRA_WASTE_JSON, Gson().toJson(item))
                putExtra(EcoWalkActivity.EXTRA_WALK_TRACKING, walkTrackingEnabled)
            })
        }
    }

    private fun categoryEmoji(category: String): String = when (category) {
        "Plastic"-> "♻"
        "Glass"-> "🍾"
        "Paper"-> "📄"
        "Hazardous Waste" -> "⚠️"
        "Electronic"-> "🔌"
        "Organic"-> "🌱"
        "Metal"-> "🔩"
        "Textile"-> "👕"
        else-> "🗑"
    }
}