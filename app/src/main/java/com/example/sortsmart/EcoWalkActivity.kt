package com.example.sortsmart

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sortsmart.adapter.CollectionPointAdapter
import com.example.sortsmart.data.CollectionPoint
import com.example.sortsmart.data.DisposalRecord
import com.example.sortsmart.data.WasteItem
import com.example.sortsmart.utils.DistanceUtils
import com.example.sortsmart.utils.JsonLoader
import com.example.sortsmart.utils.PrefsManager
import com.google.android.gms.location.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import kotlin.math.sqrt
class EcoWalkActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_WASTE_JSON    = "extra_waste_json"
        const val EXTRA_WALK_TRACKING = "extra_walk_tracking"
    }

    private lateinit var tvGpsStatus:TextView
    private lateinit var cardSelectedPoint:MaterialCardView
    private lateinit var tvSelectedPointName:TextView
    private lateinit var progressWalk:android.widget.ProgressBar
    private lateinit var tvDistanceWalked: TextView
    private lateinit var tvDistanceRemaining:TextView
    private lateinit var tvWalkStatus:TextView
    private lateinit var tvWalkIcon:TextView
    private lateinit var tvWalkCO2e:TextView
    private lateinit var btnFinishDisposal:MaterialButton
    private lateinit var btnDebugSimulate: MaterialButton
    private lateinit var rvCollectionPoints:RecyclerView
    private lateinit var nestedScrollEcoWalk:NestedScrollView
    private lateinit var cardMapView:MaterialCardView
    private lateinit var tvMapTitle:TextView
    private lateinit var webViewMap:WebView
    private lateinit var btnCloseMap: MaterialButton
    private lateinit var cbPlastic:CheckBox
    private lateinit var cbGlass: CheckBox
    private lateinit var cbPaper:CheckBox
    private lateinit var cbHazardous:CheckBox
    private lateinit var cbElectronic: CheckBox
    private lateinit var cbOrganic:CheckBox
    private lateinit var cbMetal:CheckBox
    private lateinit var cbTextile: CheckBox

    private var wasteItem: WasteItem? = null
    private var walkTrackingEnabled =true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback?= null
    private var lastKnownLat: Double? =null
    private var lastKnownLng: Double? = null
    private var selectedPoint: CollectionPoint?=null
    private var initialDistanceMetres: Float = 0f
    private var currentDistanceMetres: Float = 0f
    private lateinit var sensorManager:SensorManager
    private var accelerometer: Sensor? = null
    private var lastMovementTimeMs:Long = 0L
    private val WALKING_TIMEOUT_MS= 2000L
    private val MOVEMENT_THRESHOLD = 11.5f
    private lateinit var pointAdapter:CollectionPointAdapter

    private val locationPermissionLauncher =registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLocationUpdates()
        else tvGpsStatus.text = "⚠️ Location permission denied."
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eco_walk)
        bindViews()
        setupToolbar()
        readIntent()
        setupFilters()
        setupRecyclerView()
        setupButtons()
        setupWebView()
        setupAccelerometer()
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let{ sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (cardMapView.visibility == View.VISIBLE) closeMapCard()
        else { @Suppress("DEPRECATION") super.onBackPressed() }
    }

    private fun bindViews() {
        tvGpsStatus= findViewById(R.id.tvGpsStatus)
        cardSelectedPoint = findViewById(R.id.cardSelectedPoint)
        tvSelectedPointName = findViewById(R.id.tvSelectedPointName)
        progressWalk = findViewById(R.id.progressWalk)
        tvDistanceWalked = findViewById(R.id.tvDistanceWalked)
        tvDistanceRemaining = findViewById(R.id.tvDistanceRemaining)
        tvWalkStatus= findViewById(R.id.tvWalkStatus)
        tvWalkIcon = findViewById(R.id.tvWalkIcon)
        tvWalkCO2e = findViewById(R.id.tvWalkCO2e)
        btnFinishDisposal = findViewById(R.id.btnFinishDisposal)
        btnDebugSimulate=findViewById(R.id.btnDebugSimulate)
        rvCollectionPoints= findViewById(R.id.rvCollectionPoints)
        nestedScrollEcoWalk = findViewById(R.id.nestedScrollEcoWalk)
        cardMapView = findViewById(R.id.cardMapView)
        tvMapTitle =findViewById(R.id.tvMapTitle)
        webViewMap = findViewById(R.id.webViewMap)
        btnCloseMap = findViewById(R.id.btnCloseMap)
        cbPlastic = findViewById(R.id.cbPlastic)
        cbGlass = findViewById(R.id.cbGlass)
        cbPaper = findViewById(R.id.cbPaper)
        cbHazardous = findViewById(R.id.cbHazardous)
        cbElectronic = findViewById(R.id.cbElectronic)
        cbOrganic= findViewById(R.id.cbOrganic)
        cbMetal = findViewById(R.id.cbMetal)
        cbTextile = findViewById(R.id.cbTextile)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarEcoWalk)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun readIntent() {
        val wasteJson = intent.getStringExtra(EXTRA_WASTE_JSON)
        walkTrackingEnabled = intent.getBooleanExtra(EXTRA_WALK_TRACKING, true)
        wasteJson?.let {
            wasteItem = Gson().fromJson(it, WasteItem::class.java)
            tvWalkCO2e.text = "🌱 CO₂e Benefit: ${String.format("%.2f kg", wasteItem!!.co2eAvoided)}"
        }
    }

    private fun setupFilters() {
        val cat = wasteItem?.category ?: ""
        cbPlastic.isChecked = cat == "Plastic"
        cbGlass.isChecked = cat == "Glass"
        cbPaper.isChecked =cat== "Paper"
        cbHazardous.isChecked= cat =="Hazardous Waste"
        cbElectronic.isChecked = cat == "Electronic"
        cbOrganic.isChecked = cat == "Organic"
        cbMetal.isChecked = cat == "Metal"
        cbTextile.isChecked = cat =="Textile"
        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            lastKnownLat?.let { lat -> lastKnownLng?.let { lng -> onNewLocation(lat, lng) } }
        }
        listOf(cbPlastic, cbGlass,cbPaper,cbHazardous,
            cbElectronic,cbOrganic, cbMetal, cbTextile)
            .forEach { it.setOnCheckedChangeListener(listener) }
    }

    private fun getSelectedCategories(): Set<String> {
        val s= mutableSetOf<String>()
        if (cbPlastic.isChecked) s.add("Plastic")
        if (cbGlass.isChecked) s.add("Glass")
        if (cbPaper.isChecked) s.add("Paper")
        if (cbHazardous.isChecked) s.add("Hazardous Waste")
        if (cbElectronic.isChecked) s.add("Electronic")
        if (cbOrganic.isChecked) s.add("Organic")
        if (cbMetal.isChecked) s.add("Metal")
        if (cbTextile.isChecked) s.add("Textile")
        return s
    }

    private fun setupRecyclerView() {
        pointAdapter = CollectionPointAdapter(
            items = emptyList(),
            distances= emptyList(),
            onSelect = { point, distance -> selectPoint(point, distance) },
            onMapClick ={ point -> showMapCard(point) }
        )
        rvCollectionPoints.layoutManager =LinearLayoutManager(this)
        rvCollectionPoints.adapter = pointAdapter
    }
    private fun setupButtons() {
        btnFinishDisposal.setOnClickListener { showDisposalCompleteDialog() }
        btnDebugSimulate.setOnClickListener {
            enableFinishButton()
            currentDistanceMetres = 0f
            updateProgressBar()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webViewMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort= true
        }
        webViewMap.webViewClient = WebViewClient()
        btnCloseMap.setOnClickListener { closeMapCard() }
    }

    private fun showMapCard(point: CollectionPoint) {
        val userLat = lastKnownLat ?: run {
            Toast.makeText(this, "GPS not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val userLng = lastKnownLng ?: return

        val osrmUrl= "https://router.project-osrm.org/route/v1/foot/" +
                "$userLng,$userLat;${point.longitude},${point.latitude}" +
                "?overview=full&geometries=geojson"

        val html = """
            <!DOCTYPE html><html>
            <head>
                <meta name="viewport" content="width=device-width,initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>*{margin:0;padding:0}#map{width:100vw;height:320px}</style>
            </head>
            <body><div id="map"></div><script>
                var map = L.map('map').setView([$userLat,$userLng],15);
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
                    {attribution:'© OpenStreetMap © CARTO',subdomains:'abcd',maxZoom:19}).addTo(map);
                L.circleMarker([$userLat,$userLng],
                    {radius:10,color:'#1565C0',fillColor:'#42A5F5',fillOpacity:0.9,weight:2})
                    .addTo(map).bindPopup('<b>You are here</b>').openPopup();
                L.marker([${point.latitude},${point.longitude}])
                    .addTo(map).bindPopup('<b>${point.name}</b>');
                fetch('$osrmUrl')
                    .then(r=>r.json())
                    .then(d=>{
                        if(!d.routes||!d.routes.length)return;
                        var coords=d.routes[0].geometry.coordinates.map(c=>[c[1],c[0]]);
                        var line=L.polyline(coords,{color:'#2E7D32',weight:5,opacity:0.85}).addTo(map);
                        map.fitBounds(line.getBounds(),{padding:[30,30]});
                    })
                    .catch(()=>{
                        var fb=L.polyline([[$userLat,$userLng],[${point.latitude},${point.longitude}]],
                            {color:'#2E7D32',weight:3,dashArray:'8,8'}).addTo(map);
                        map.fitBounds(fb.getBounds(),{padding:[30,30]});
                    });
            </script></body></html>
        """.trimIndent()

        tvMapTitle.text = "↗ Walking to ${point.name}"
        cardMapView.visibility= View.VISIBLE
        cardMapView.alpha = 0f
        cardMapView.animate().alpha(1f).setDuration(300).start()
        webViewMap.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "utf-8", null)
        nestedScrollEcoWalk.post { nestedScrollEcoWalk.smoothScrollTo(0, cardMapView.top) }
    }

    private fun closeMapCard() {
        cardMapView.animate().alpha(0f).setDuration(200).withEndAction {
            cardMapView.visibility = View.GONE
            webViewMap.loadUrl("about:blank")
        }.start()
    }

    private fun setupAccelerometer() {
        sensorManager =getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) tvWalkStatus.text = "Accelerometer unavailable"
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val (x, y, z) = event.values
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        if (magnitude > MOVEMENT_THRESHOLD) lastMovementTimeMs = System.currentTimeMillis()
        val isWalking = (System.currentTimeMillis() - lastMovementTimeMs) < WALKING_TIMEOUT_MS
        if (walkTrackingEnabled) {
            tvWalkStatus.text =if (isWalking) "Walking" else "Stationary"
            tvWalkIcon.text = if (isWalking) "🚶" else "🧍"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val perm= android.Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED)
            startLocationUpdates()
        else
            locationPermissionLauncher.launch(perm)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        tvGpsStatus.text = "📡 GPS active — acquiring location…"
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(2f).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onNewLocation(it.latitude, it.longitude) }
            }
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun onNewLocation(lat: Double, lng: Double) {
        lastKnownLat = lat
        lastKnownLng = lng
        tvGpsStatus.text ="📡 GPS: ${String.format("%.5f", lat)}, ${String.format("%.5f", lng)}"

        val selected = getSelectedCategories()
        val filtered= JsonLoader.loadCollectionPoints(this)
            .filter { p -> p.acceptedCategories.any { it in selected } }

        if (filtered.isEmpty()) {
            tvGpsStatus.text= "⚠️ No points found for selected categories"
            pointAdapter.updateData(emptyList(), emptyList())
            return
        }

        val sorted = filtered
            .map { it to DistanceUtils.haversine(lat, lng, it.latitude, it.longitude) }
            .sortedBy { it.second }

        pointAdapter.updateData(sorted.map { it.first }, sorted.map { it.second })

        if (selectedPoint == null) selectPoint(sorted.first().first, sorted.first().second)

        selectedPoint?.let { pt ->
            currentDistanceMetres = DistanceUtils.haversine(lat, lng, pt.latitude, pt.longitude)
            updateProgressBar()
            if (currentDistanceMetres <= 30f) enableFinishButton()
        }
    }

    private fun selectPoint(point: CollectionPoint, distance: Float) {
        selectedPoint= point
        initialDistanceMetres = distance
        currentDistanceMetres =distance
        cardSelectedPoint.visibility = View.VISIBLE
        tvSelectedPointName.text = point.name
        updateProgressBar()
        disableFinishButton()
    }

    private fun updateProgressBar() {
        if (initialDistanceMetres <= 0f) return
        val walked = (initialDistanceMetres - currentDistanceMetres).coerceAtLeast(0f)
        progressWalk.progress = ((walked / initialDistanceMetres) * 100f).toInt().coerceIn(0, 100)
        tvDistanceWalked.text = formatDistance(walked) + " walked"
        tvDistanceRemaining.text = formatDistance(currentDistanceMetres) + " remaining"
    }

    private fun enableFinishButton() { btnFinishDisposal.isEnabled = true;  btnFinishDisposal.alpha = 1.0f }
    private fun disableFinishButton() { btnFinishDisposal.isEnabled = false; btnFinishDisposal.alpha = 0.5f }

    private fun showDisposalCompleteDialog() {
        val item= wasteItem ?: return
        val point = selectedPoint ?: return
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_disposal_complete)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.tvDialogWaste).text = "♻ ${item.name}"
        dialog.findViewById<TextView>(R.id.tvDialogPoint).text= "📍 ${point.name}"
        dialog.findViewById<TextView>(R.id.tvDialogDistance).text = "🚶 ${formatDistance(initialDistanceMetres)} walked"
        dialog.findViewById<TextView>(R.id.tvDialogCO2e).text= "🌱 +${String.format("%.2f kg", item.co2eAvoided)} CO₂e"

        dialog.findViewById<TextView>(R.id.tvCompletionIcon)
            .animate().scaleX(1f).scaleY(1f).setDuration(600)
            .setInterpolator(OvershootInterpolator()).start()

        dialog.findViewById<MaterialButton>(R.id.btnSaveRecord).setOnClickListener {
            PrefsManager.saveRecord(this, DisposalRecord(
                wasteName= item.name,
                category = item.category,
                pointName =point.name,
                distanceMeters = initialDistanceMetres,
                co2eAvoided = item.co2eAvoided,
                timestamp = System.currentTimeMillis()
            ))
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    private fun formatDistance(metres: Float) =
        if (metres < 1000) "${metres.toInt()} m"
        else String.format("%.1f km", metres / 1000f)
}