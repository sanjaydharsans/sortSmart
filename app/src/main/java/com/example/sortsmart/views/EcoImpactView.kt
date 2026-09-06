package com.example.sortsmart.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class EcoImpactView @JvmOverloads constructor(
    context:Context,
    attrs: AttributeSet? = null,
    defStyleAttr:Int = 0
) : View(context, attrs, defStyleAttr) {
    var co2eValue:Double = 0.0
        set(value) { field = value; startAnimation() }

    var category:String = ""
        set(value) { field = value; arcPaint.color = categoryColour(value); invalidate() }

    private var animatedAngle:Float = 0f
    private val ovalRect = RectF()

    private val trackPaint =Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); style = Paint.Style.STROKE; strokeWidth = 28f; strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66BB6A"); style = Paint.Style.STROKE; strokeWidth = 28f; strokeCap = Paint.Cap.ROUND
    }
    private val emojiPaint= Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 80f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B5E20"); textAlign =Paint.Align.CENTER; textSize = 52f; isFakeBoldText = true
    }
    private val unitPaint =Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#558B2F"); textAlign = Paint.Align.CENTER; textSize = 32f
    }
    private val categoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575"); textAlign = Paint.Align.CENTER; textSize = 30f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy =height / 2f
        val radius= (minOf(width, height) / 2f) - 40f
        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(ovalRect, 135f, 270f, false, trackPaint)
        canvas.drawArc(ovalRect, 135f, animatedAngle, false, arcPaint)
        canvas.drawText("♻", cx, cy - 20f, emojiPaint)
        canvas.drawText(String.format("%.2f", co2eValue), cx, cy + 60f, valuePaint)
        canvas.drawText("kg CO₂e", cx, cy + 100f, unitPaint)
        canvas.drawText(category, cx, cy + radius + 50f, categoryPaint)
    }

    private fun startAnimation() {
        val targetAngle= ((co2eValue / 0.5) * 270f).toFloat().coerceAtMost(270f)
        ValueAnimator.ofFloat(0f, targetAngle).apply {
            duration = 1200
            interpolator =DecelerateInterpolator()
            addUpdateListener{ animatedAngle = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun categoryColour(cat: String): Int = when (cat) {
        "Plastic"-> Color.parseColor("#29B6F6")
        "Glass" ->Color.parseColor("#66BB6A")
        "Paper"-> Color.parseColor("#FFA726")
        "Hazardous Waste" -> Color.parseColor("#EF5350")
        "Electronic"-> Color.parseColor("#AB47BC")
        "Organic" ->Color.parseColor("#8D6E63")
        "Metal" -> Color.parseColor("#78909C")
        "Textile"  -> Color.parseColor("#EC407A")
        else -> Color.parseColor("#66BB6A")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (280 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(resolveSize(desired, widthMeasureSpec), resolveSize(desired, widthMeasureSpec))
    }
}