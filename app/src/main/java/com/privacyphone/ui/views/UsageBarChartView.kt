package com.privacyphone.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class UsageBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: List<Long> = List(7) { 0L }
    private val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.FILL
    }
    private val todayBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.FILL
        alpha = 255
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1f
    }

    fun setData(usageData: List<Long>) {
        data = usageData.takeLast(7).let {
            if (it.size < 7) List(7 - it.size) { 0L } + it else it
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingBottom = 60f
        val paddingTop = 20f
        val chartH = h - paddingBottom - paddingTop
        val barCount = days.size
        val barWidth = (w / barCount) * 0.5f
        val spacing = w / barCount
        val maxVal = data.maxOrNull()?.takeIf { it > 0 } ?: 60L

        // Draw grid lines
        for (i in 1..4) {
            val y = paddingTop + (chartH / 4) * i
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Draw bars
        data.forEachIndexed { i, value ->
            val barH = if (maxVal > 0) (value.toFloat() / maxVal) * chartH else 0f
            val left = i * spacing + (spacing - barWidth) / 2
            val top = paddingTop + chartH - barH
            val right = left + barWidth
            val bottom = paddingTop + chartH

            val paint = if (i == data.size - 1) todayBarPaint else barPaint.also { it.alpha = 180 }
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Day label
            canvas.drawText(days.getOrNull(i) ?: "", left + barWidth / 2, h - 8f, labelPaint)

            // Value on top of bar if > 0
            if (value > 0 && barH > 30f) {
                val label = if (value >= 60) "${value / 60}h" else "${value}m"
                canvas.drawText(label, left + barWidth / 2, top - 4f, valuePaint)
            }
        }
    }
}
