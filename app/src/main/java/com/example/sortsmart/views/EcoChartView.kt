package com.example.sortsmart.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.sortsmart.utils.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

class EcoChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32"); style = Paint.Style.FILL
    }
    private val barHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00897B"); style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); style = Paint.Style.STROKE; strokeWidth = 1f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#558B2F"); textAlign = Paint.Align.CENTER; textSize = 28f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B5E20"); textAlign = Paint.Align.CENTER; textSize = 22f; isFakeBoldText = true
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B5E20"); textAlign = Paint.Align.LEFT; textSize = 34f; isFakeBoldText = true
    }
    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E"); textAlign = Paint.Align.RIGHT; textSize = 22f
    }
    private var dailyTotals = FloatArray(7) { 0f }
    private var dayLabels = Array(7) { "" }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadData()
    }

    fun refresh() {
        loadData()
        invalidate()
    }
    private fun loadData() {
        val records= PrefsManager.getRecords(context)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat =SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dailyMap= mutableMapOf<String, Double>()

        for (record in records) {
            val key = dateFormat.format(Date(record.timestamp))
            dailyMap[key] =(dailyMap[key] ?: 0.0) + record.co2eAvoided
        }

        val cal= Calendar.getInstance()
        for (i in 6 downTo 0) {
            val slotIndex = 6 - i
            val key = dateFormat.format(cal.time)
            dailyTotals[slotIndex] = (dailyMap[key] ?: 0.0).toFloat()
            dayLabels[slotIndex] = dayFormat.format(cal.time).take(1)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        dailyTotals.reverse()
        dayLabels.reverse()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val paddingLeft = 80f
        val paddingRight= 20f
        val paddingTop= 60f
        val paddingBottom = 50f
        val chartLeft = paddingLeft
        val chartRight =w - paddingRight
        val chartTop = paddingTop
        val chartBottom= h - paddingBottom
        val chartH =chartBottom - chartTop
        val chartW=chartRight - chartLeft

        canvas.drawRoundRect(0f, 0f, w, h, 16f, 16f, backgroundPaint)
        canvas.drawText("Weekly CO₂e Benefit", chartLeft, 42f, titlePaint)

        val maxVal = dailyTotals.maxOrNull()?.takeIf { it > 0f } ?: 0.5f
        val gridCount = 4

        for (i in 0..gridCount) {
            val y = chartBottom - (i.toFloat() / gridCount) * chartH
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            canvas.drawText(
                String.format("%.2f", (i.toFloat() / gridCount) * maxVal),
                chartLeft - 8f, y + 8f, yLabelPaint
            )
        }

        val barSpacing = chartW / 7
        val barWidth = barSpacing * 0.55f

        for (i in 0 until 7) {
            val value = dailyTotals[i]
            val barH =if (maxVal > 0) (value / maxVal) * chartH else 0f
            val barH2 = barH.coerceAtLeast(if (value > 0f) 8f else 0f)
            val centerX = chartLeft + (i + 0.5f) * barSpacing
            val left = centerX - barWidth / 2f
            val right= centerX + barWidth / 2f
            val top = chartBottom - barH2

            if (barH2 > 0f)
                canvas.drawRoundRect(left, top, right, chartBottom, 6f, 6f,
                    if (i == 6) barHighlightPaint else barPaint)

            if (value > 0f)
                canvas.drawText(String.format("%.2f", value), centerX, top - 6f, valuePaint)
            canvas.drawText(dayLabels[i], centerX, h - 12f, labelPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH= (220 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(
            resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }
}