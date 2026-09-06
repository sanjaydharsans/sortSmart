package com.example.sortsmart

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.adapter.RecentWasteAdapter
import com.example.sortsmart.adapter.SearchResultAdapter
import com.example.sortsmart.data.WasteItem
import com.example.sortsmart.utils.JsonLoader
import com.example.sortsmart.utils.PrefsManager
import com.example.sortsmart.views.EcoChartView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_WASTE_ID = "extra_waste_id"
        const val EXTRA_WALK_TRACKING = "extra_walk_tracking"
    }

    private lateinit var etSearch:TextInputEditText
    private lateinit var btnSearch:MaterialButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoResults:TextView
    private lateinit var rvRecentWaste:RecyclerView
    private lateinit var tvNoRecentWaste: TextView
    private lateinit var tvTotalCO2e:TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalDistance:TextView
    private lateinit var btnViewImpact: MaterialButton
    private lateinit var switchWalkTracking:SwitchCompat
    private lateinit var cardEcoChart:MaterialCardView
    private lateinit var ecoChartView:EcoChartView
    private lateinit var tvAllTimeCO2e:TextView
    private lateinit var tvAllTimeItems:TextView

    private lateinit var searchAdapter:SearchResultAdapter
    private lateinit var recentAdapter:RecentWasteAdapter
    private lateinit var allWasteItems:List<WasteItem>
    private var isChartVisible = false

    private val ecoWalkLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshHomeData() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupToolbar()
        allWasteItems = JsonLoader.loadWasteItems(this)
        setupSearchRecyclerView()
        setupRecentRecyclerView()
        setupSearchListeners()
        setupSwitch()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_clear -> {
                PrefsManager.clearRecords(this)
                refreshHomeData()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                AlertDialog.Builder(this)
                    .setTitle("SortSmart")
                    .setMessage("Smart Waste & Eco-Walk Assistant\n\nVersion 1.0")
                    .setPositiveButton("OK", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun bindViews() {
        etSearch= findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        rvSearchResults= findViewById(R.id.rvSearchResults)
        tvNoResults =findViewById(R.id.tvNoResults)
        rvRecentWaste = findViewById(R.id.rvRecentWaste)
        tvNoRecentWaste = findViewById(R.id.tvNoRecentWaste)
        tvTotalCO2e = findViewById(R.id.tvTotalCO2e)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvTotalDistance= findViewById(R.id.tvTotalDistance)
        btnViewImpact   = findViewById(R.id.btnViewImpact)
        switchWalkTracking= findViewById(R.id.switchWalkTracking)
        cardEcoChart =findViewById(R.id.cardEcoChart)
        ecoChartView = findViewById(R.id.ecoChartView)
        tvAllTimeCO2e = findViewById(R.id.tvAllTimeCO2e)
        tvAllTimeItems= findViewById(R.id.tvAllTimeItems)
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
    }
    private fun setupSearchRecyclerView() {
        searchAdapter= SearchResultAdapter(emptyList()) { item ->
            navigateToWasteResult(item)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter
    }

    private fun setupRecentRecyclerView() {
        recentAdapter = RecentWasteAdapter(emptyList()){ _ -> }
        rvRecentWaste.layoutManager = LinearLayoutManager(this)
        rvRecentWaste.adapter = recentAdapter
    }

    private fun setupSearchListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim() ?: ""
                if (q.isEmpty()) hideSearchResults() else performSearch(q)
            }
        })
        btnSearch.setOnClickListener {
            val q = etSearch.text?.toString()?.trim() ?: ""
            if (q.isEmpty()) hideSearchResults() else { performSearch(q); hideKeyboard() }
        }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q= etSearch.text?.toString()?.trim() ?: ""
                performSearch(q); hideKeyboard(); true
            } else false
        }
    }

    private fun performSearch(query: String) {
        val results = allWasteItems.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
        if (results.isEmpty()) {
            rvSearchResults.visibility= View.GONE
            tvNoResults.visibility = View.VISIBLE
        } else {
            tvNoResults.visibility = View.GONE
            rvSearchResults.visibility = View.VISIBLE
            searchAdapter.updateData(results)
        }
    }

    private fun hideSearchResults() {
        rvSearchResults.visibility= View.GONE
        tvNoResults.visibility = View.GONE
    }

    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun setupSwitch() {
        switchWalkTracking.isChecked =PrefsManager.isWalkTrackingEnabled(this)
        switchWalkTracking.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setWalkTrackingEnabled(this, isChecked)
        }
    }

    private fun setupButtons() {
        btnViewImpact.setOnClickListener { toggleEcoChart() }
    }

    private fun toggleEcoChart() {
        if (isChartVisible) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            anim.setAnimationListener(object :Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {}
                override fun onAnimationRepeat(a: Animation?) {}
                override fun onAnimationEnd(a: Animation?) { cardEcoChart.visibility = View.GONE }
            })
            cardEcoChart.startAnimation(anim)
            btnViewImpact.text = getString(R.string.btn_view_impact)
            isChartVisible = false
        } else {
            cardEcoChart.visibility = View.VISIBLE
            cardEcoChart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down))
            ecoChartView.refresh()
            btnViewImpact.text = "Hide Eco Impact"
            isChartVisible = true
            tvAllTimeCO2e.text = "Total CO₂e: ${String.format("%.2f kg", PrefsManager.getTotalCO2e(this))}"
            tvAllTimeItems.text = "Items: ${PrefsManager.getTotalItems(this)}"
        }
    }

    private fun refreshHomeData() {
        tvTotalCO2e.text = String.format("%.2f kg", PrefsManager.getTotalCO2e(this))
        tvTotalItems.text = PrefsManager.getTotalItems(this).toString()

        val records = PrefsManager.getRecentRecords(this)
        val totalDistanceM = records.sumOf { it.distanceMeters.toDouble() }.toFloat()
        tvTotalDistance.text = if (totalDistanceM < 1000) "${totalDistanceM.toInt()} m"
        else String.format("%.2f km",totalDistanceM / 1000f)
        if (records.isEmpty()) {
            tvNoRecentWaste.visibility =View.VISIBLE
            rvRecentWaste.visibility = View.GONE
        } else {
            tvNoRecentWaste.visibility = View.GONE
            rvRecentWaste.visibility= View.VISIBLE
            recentAdapter.updateData(records)
        }
        if (isChartVisible) ecoChartView.refresh()
    }

    private fun navigateToWasteResult(item: WasteItem) {
        val intent = Intent(this, WasteResultActivity::class.java).apply {
            putExtra(EXTRA_WASTE_ID,item.id)
            putExtra(EXTRA_WALK_TRACKING, switchWalkTracking.isChecked)
        }
        ecoWalkLauncher.launch(intent)
    }
}