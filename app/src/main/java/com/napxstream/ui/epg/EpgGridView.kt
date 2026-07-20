package com.napxstream.ui.epg

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import com.napxstream.data.model.EpgListing
import com.napxstream.util.Constants
import kotlin.math.max
import kotlin.math.min

data class EpgChannelRow(val streamId: Int, val name: String, val programs: List<EpgListing>, val tvArchive: Boolean = false)

/**
 * Klasik TV rehberi görünümü: solda sabit kanal sütunu, üstte sabit saat başlığı,
 * ortada hem yatay (zaman) hem dikey (kanal) kaydırılabilen program bloklarını çizen
 * özel bir View. RecyclerView yerine tek bir Canvas kullanılır çünkü hücreler
 * program süresine göre değişken genişlikte olduğundan grid/list adapter'larıyla
 * senkron kaydırma yönetmek yerine burada tüm çizim ve dokunma testini kendimiz yapıyoruz.
 */
class EpgGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val rowHeight = 68 * density
    private val headerHeight = 40 * density
    private val channelColWidth = 130 * density
    private val pixelsPerMinute = 5.5f * density

    private var rows: List<EpgChannelRow> = emptyList()
    private var windowStartMs = 0L
    private var windowEndMs = 0L

    private var scrollXPos = 0f
    private var scrollYPos = 0f
    private var maxScrollX = 0f
    private var maxScrollY = 0f

    private var onProgramClick: ((EpgChannelRow, EpgListing) -> Unit)? = null

    private val scroller = OverScroller(context)
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollXPos = (scrollXPos + distanceX).coerceIn(0f, maxScrollX)
            scrollYPos = (scrollYPos + distanceY).coerceIn(0f, maxScrollY)
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            scroller.fling(
                scrollXPos.toInt(), scrollYPos.toInt(), -velocityX.toInt(), -velocityY.toInt(),
                0, maxScrollX.toInt(), 0, maxScrollY.toInt()
            )
            postInvalidateOnAnimation()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }
    })

    private val bgPaint = Paint().apply { color = Color.parseColor("#0E1116") }
    private val headerPaint = Paint().apply { color = Color.parseColor("#232833") }
    private val channelBgPaint = Paint().apply { color = Color.parseColor("#171B22") }
    private val gridLinePaint = Paint().apply { color = Color.parseColor("#2A2F3A"); strokeWidth = 1f }
    private val programPaint = Paint().apply { color = Color.parseColor("#232833") }
    private val programNowPaint = Paint().apply { color = Color.parseColor("#3A3350") }
    private val nowLinePaint = Paint().apply { color = Color.parseColor("#22D3EE"); strokeWidth = 3f }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 13 * density; isAntiAlias = true }
    private val timeTextPaint = Paint().apply { color = Color.parseColor("#A0A6B1"); textSize = 11 * density; isAntiAlias = true }
    private val channelTextPaint = Paint().apply { color = Color.WHITE; textSize = 13 * density; isAntiAlias = true }

    /** windowStartMs/EndMs: grid'in gösterdiği zaman aralığı (ör. şimdi-1sa .. şimdi+5sa) */
    fun setData(newRows: List<EpgChannelRow>, newWindowStartMs: Long, newWindowEndMs: Long) {
        rows = newRows
        windowStartMs = newWindowStartMs
        windowEndMs = newWindowEndMs
        recalcBounds()
        scrollXPos = 0f
        scrollYPos = 0f
        invalidate()
    }

    fun setOnProgramClickListener(listener: (EpgChannelRow, EpgListing) -> Unit) {
        onProgramClick = listener
    }

    /** Grid'i "şimdi" zamanına, belirtilen kanal satırına kaydırır. */
    fun scrollToNow(channelIndex: Int = 0) {
        val nowMs = System.currentTimeMillis()
        val minutesFromStart = (nowMs - windowStartMs) / 60000f
        val targetX = (minutesFromStart * pixelsPerMinute) - (width - channelColWidth) / 3f
        scrollXPos = targetX.coerceIn(0f, maxScrollX)
        scrollYPos = (channelIndex * rowHeight).coerceIn(0f, maxScrollY)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalcBounds()
    }

    private fun recalcBounds() {
        val totalMinutes = (windowEndMs - windowStartMs) / 60000f
        val contentWidth = totalMinutes * pixelsPerMinute
        maxScrollX = max(0f, contentWidth - (width - channelColWidth))
        val contentHeight = rows.size * rowHeight
        maxScrollY = max(0f, contentHeight - (height - headerHeight))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        parent?.requestDisallowInterceptTouchEvent(
            event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL
        )
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollXPos = scroller.currX.toFloat().coerceIn(0f, maxScrollX)
            scrollYPos = scroller.currY.toFloat().coerceIn(0f, maxScrollY)
            invalidate()
        }
    }

    private fun handleTap(x: Float, y: Float) {
        if (x < channelColWidth || y < headerHeight) return
        val rowIndex = ((y - headerHeight + scrollYPos) / rowHeight).toInt()
        if (rowIndex !in rows.indices) return
        val row = rows[rowIndex]
        val tappedTimeMs = windowStartMs + ((x - channelColWidth + scrollXPos) / pixelsPerMinute * 60000).toLong()
        val program = row.programs.firstOrNull { listing ->
            val start = (listing.startTimestamp?.toLongOrNull() ?: 0L) * 1000
            val end = (listing.stopTimestamp?.toLongOrNull() ?: 0L) * 1000
            tappedTimeMs in start until end
        }
        if (program != null) onProgramClick?.invoke(row, program)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        if (rows.isEmpty() || windowEndMs <= windowStartMs) return

        val nowMs = System.currentTimeMillis()

        // ---- Zaman başlığı (yatayda kaydırılır, dikeyde sabit) ----
        canvas.save()
        canvas.clipRect(channelColWidth, 0f, width.toFloat(), headerHeight)
        canvas.drawRect(channelColWidth, 0f, width.toFloat(), headerHeight, headerPaint)
        var t = windowStartMs - (windowStartMs % (30 * 60000L))
        while (t < windowEndMs) {
            val xPos = channelColWidth + ((t - windowStartMs) / 60000f * pixelsPerMinute) - scrollXPos
            if (xPos in (channelColWidth - 100)..(width + 100f)) {
                canvas.drawText(Constants.formatEpgTime((t / 1000).toString()), xPos + 6, headerHeight - 12 * density, timeTextPaint)
                canvas.drawLine(xPos, 0f, xPos, height.toFloat(), gridLinePaint)
            }
            t += 30 * 60000L
        }
        canvas.restore()

        // ---- Kanal satırları + program blokları (gövde, dikeyde kaydırılır) ----
        canvas.save()
        canvas.clipRect(0f, headerHeight, width.toFloat(), height.toFloat())

        rows.forEachIndexed { index, row ->
            val rowTop = headerHeight + index * rowHeight - scrollYPos
            if (rowTop + rowHeight < headerHeight || rowTop > height) return@forEachIndexed

            canvas.drawLine(0f, rowTop + rowHeight, width.toFloat(), rowTop + rowHeight, gridLinePaint)

            // Programlar (kanal sütununun sağı; hem yatay hem dikey kaydırmaya tabi)
            canvas.save()
            canvas.clipRect(channelColWidth, headerHeight, width.toFloat(), height.toFloat())
            row.programs.forEach programLoop@{ program ->
                val startMs = (program.startTimestamp?.toLongOrNull() ?: return@programLoop) * 1000
                val endMs = (program.stopTimestamp?.toLongOrNull() ?: return@programLoop) * 1000
                if (endMs < windowStartMs || startMs > windowEndMs) return@programLoop

                val left = channelColWidth + ((max(startMs, windowStartMs) - windowStartMs) / 60000f * pixelsPerMinute) - scrollXPos
                val right = channelColWidth + ((min(endMs, windowEndMs) - windowStartMs) / 60000f * pixelsPerMinute) - scrollXPos
                if (right < channelColWidth || left > width) return@programLoop

                val isNow = nowMs in startMs until endMs
                val rect = RectF(max(left, channelColWidth), rowTop + 4, max(right - 2, max(left, channelColWidth) + 2), rowTop + rowHeight - 4)
                canvas.drawRoundRect(rect, 6f, 6f, if (isNow) programNowPaint else programPaint)

                canvas.save()
                canvas.clipRect(rect)
                canvas.drawText(Constants.decodeEpgText(program.title), rect.left + 8, rowTop + rowHeight / 2 + 5 * density, textPaint)
                canvas.restore()
            }
            canvas.restore()

            // Kanal adı sütunu (yatayda sabit kalır — en son çizilir ki program blokları altında kalmasın)
            canvas.drawRect(0f, rowTop, channelColWidth, rowTop + rowHeight, channelBgPaint)
            canvas.drawText(row.name.take(18), 12 * density, rowTop + rowHeight / 2 + 5 * density, channelTextPaint)
            canvas.drawLine(channelColWidth, rowTop, channelColWidth, rowTop + rowHeight, gridLinePaint)
        }
        canvas.restore()

        // ---- "Şimdi" çizgisi ----
        if (nowMs in windowStartMs..windowEndMs) {
            val nowX = channelColWidth + ((nowMs - windowStartMs) / 60000f * pixelsPerMinute) - scrollXPos
            if (nowX in channelColWidth..width.toFloat()) {
                canvas.drawLine(nowX, headerHeight, nowX, height.toFloat(), nowLinePaint)
            }
        }

        // ---- Sol üst köşe (kanal sütunu başlığı, her şeyin üstünde sabit) ----
        canvas.drawRect(0f, 0f, channelColWidth, headerHeight, headerPaint)
    }
}
