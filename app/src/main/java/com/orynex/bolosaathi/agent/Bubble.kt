package com.orynex.bolosaathi.agent

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.orynex.bolosaathi.R
import kotlin.math.abs

/**
 * Small floating mic that sits on top of every app. Tap it and speak.
 * Built with plain Views because it lives inside the accessibility service.
 */
class Bubble(
    private val ctx: Context,
    private val onTap: () -> Unit,
    private val onStop: () -> Unit,
) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val d = ctx.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    private val brand = Color.parseColor("#1E6FD0")
    private val warnColor = Color.parseColor("#B71C1C")

    private var root: LinearLayout? = null
    private lateinit var card: LinearLayout
    private lateinit var userText: TextView
    private lateinit var mainText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnRow: LinearLayout
    private lateinit var mic: FrameLayout
    private lateinit var micBg: GradientDrawable
    private var pulse: ValueAnimator? = null
    private var confirmCb: ((Boolean) -> Unit)? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.END
        x = dp(16)
        y = dp(120)
    }

    fun show() {
        if (root != null) return
        root = build()
        runCatching { wm.addView(root, params) }
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    private fun cardBg(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), Color.parseColor("#DCE5EF"))
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun build(): LinearLayout {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            elevation = dp(6).toFloat()
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }
        }
        statusText = TextView(ctx).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#4A5668"))
            text = "बोलो साथी"
        }
        userText = TextView(ctx).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#4A5668"))
            setPadding(0, dp(4), 0, 0)
            visibility = View.GONE
        }
        mainText = TextView(ctx).apply {
            textSize = 17f
            setTextColor(Color.parseColor("#0E1726"))
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(0f, 1.15f)
        }
        btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            visibility = View.GONE
        }
        fun pill(label: String, fill: Int, fg: Int, value: Boolean) = Button(ctx).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(fg)
            background = GradientDrawable().apply { setColor(fill); cornerRadius = dp(24).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(8) }
            setOnClickListener { confirmCb?.invoke(value); showConfirm(null, null) }
        }
        btnRow.addView(pill("हाँ", brand, Color.WHITE, true))
        btnRow.addView(pill("नहीं", Color.parseColor("#EEF3F9"), Color.parseColor("#0E1726"), false))

        val stop = TextView(ctx).apply {
            text = "बंद करें"
            textSize = 13f
            setTextColor(brand)
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { onStop() }
        }
        card.addView(statusText)
        card.addView(userText)
        card.addView(mainText)
        card.addView(btnRow)
        card.addView(stop)

        micBg = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(brand) }
        mic = FrameLayout(ctx).apply {
            background = micBg
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(60), dp(60))
            contentDescription = "Bolo Saathi se boliye"
            addView(ImageView(ctx).apply {
                setImageResource(R.drawable.bubble_mic)
                layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER)
            })
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        mic.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) {
                        params.x = (startX - dx).toInt().coerceAtLeast(0)
                        params.y = (startY - dy).toInt().coerceAtLeast(0)
                        root?.let { runCatching { wm.updateViewLayout(it, params) } }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) onTap(); true }
                else -> false
            }
        }
        col.addView(card)
        col.addView(mic)
        return col
    }

    fun expand() { if (::card.isInitialized) card.visibility = View.VISIBLE }

    fun collapse() {
        if (!::card.isInitialized) return
        card.visibility = View.GONE
        userText.visibility = View.GONE
        mainText.text = ""
        showConfirm(null, null)
        setListening(false)
        micBg.setColor(brand)
    }

    fun setStatus(s: String?) {
        if (!::statusText.isInitialized) return
        statusText.text = s ?: "बोलो साथी"
    }

    fun setText(s: String, fromUser: Boolean) {
        if (!::mainText.isInitialized) return
        expand()
        if (fromUser) {
            userText.visibility = View.VISIBLE
            userText.text = "आपने कहा: $s"
        } else {
            mainText.setTextColor(Color.parseColor("#0E1726"))
            mainText.text = s
        }
    }

    fun warn(s: String) {
        show()
        expand()
        statusText.text = "चेतावनी"
        userText.visibility = View.GONE
        mainText.setTextColor(warnColor)
        mainText.text = s
        micBg.setColor(warnColor)
    }

    fun showConfirm(q: String?, cb: ((Boolean) -> Unit)?) {
        if (!::btnRow.isInitialized) return
        confirmCb = cb
        btnRow.visibility = if (q != null) View.VISIBLE else View.GONE
        if (q != null) { expand(); mainText.text = q }
    }

    fun setListening(on: Boolean) {
        if (!::mic.isInitialized) return
        pulse?.cancel()
        if (on) {
            pulse = ValueAnimator.ofFloat(1f, 1.14f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { mic.scaleX = it.animatedValue as Float; mic.scaleY = it.animatedValue as Float }
                start()
            }
        } else {
            mic.scaleX = 1f; mic.scaleY = 1f
        }
    }
}
