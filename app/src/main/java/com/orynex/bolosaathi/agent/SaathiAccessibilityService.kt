package com.orynex.bolosaathi.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.core.ChatMsg
import com.orynex.bolosaathi.core.Saathi
import com.orynex.bolosaathi.core.SaathiUi
import com.orynex.bolosaathi.core.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SaathiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: SaathiAccessibilityService? = null
            private set
        val connected get() = instance != null

        /** Apps where we watch for "request money" style scams. */
        val UPI_APPS = setOf(
            "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm",
            "in.org.npci.upiapp", "in.amazon.mShop.android.shopping", "com.dreamplug.androidapp",
            "com.mobikwik_new", "com.freecharge.android", "com.sbi.upi", "com.axis.mobile",
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastNodes: List<AccessibilityNodeInfo> = emptyList()
    private lateinit var voice: Voice
    private var bubble: Bubble? = null
    private var saathi: Saathi? = null
    private var bubbleJob: Job? = null

    /** True while the agent is driving the phone, so the scam watcher stays quiet. */
    @Volatile
    var agentBusy = false

    private var lastWarnKey = ""
    private var lastWarnAt = 0L
    private var lastScanAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        voice = Voice(this)
        bubble = Bubble(this, onTap = ::onBubbleTap, onStop = ::stopBubbleTask)
        if (BoloApp.instance.prefs.bubble) bubble?.show()
    }

    fun setBubbleVisible(show: Boolean) {
        if (show) bubble?.show() else bubble?.hide()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && BoloApp.instance.prefs.bubble && bubbleJob?.isActive != true &&
            !pkg.contains("inputmethod") && !pkg.contains("keyboard") && pkg != "com.android.systemui"
        ) {
            if (pkg == packageName) bubble?.hide() else bubble?.show()
        }
        if (pkg == packageName) return
        if (!BoloApp.instance.prefs.scamGuard || agentBusy) return
        if (pkg !in UPI_APPS) return
        val now = System.currentTimeMillis()
        if (now - lastScanAt < 700) return
        lastScanAt = now
        val text = screenText(maxChars = 3000)
        val hit = ScamRules.check(text) ?: return
        val key = pkg + hit.code
        if (key == lastWarnKey && now - lastWarnAt < 60_000) return
        lastWarnKey = key
        lastWarnAt = now
        scope.launch {
            val lang = BoloApp.instance.prefs.lang
            val msg = hit.message(lang)
            bubble?.warn(msg)
            voice.speak(msg)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        bubble?.hide()
        scope.cancel()
        voice.release()
        super.onDestroy()
    }

    // bubble flow

    private fun onBubbleTap() {
        if (bubbleJob?.isActive == true) {
            voice.stopListening()
            return
        }
        val b = bubble ?: return
        val ui = object : SaathiUi {
            override fun add(msg: ChatMsg) { b.setText(msg.text, msg.fromUser) }
            override fun status(text: String?) { b.setStatus(text) }
            override fun showConfirm(question: String?, onAnswer: ((Boolean) -> Unit)?) { b.showConfirm(question, onAnswer) }
            override fun listening(on: Boolean) { b.setListening(on) }
        }
        val s = saathi ?: Saathi(this, voice, ui, screenProvider = { screenText(4000) }).also { saathi = it }
        s.ui = ui
        bubbleJob = scope.launch {
            b.expand()
            s.listenAndHandle()
            delay(4000)
            if (bubbleJob?.isActive == true) b.collapse()
        }
    }

    private fun stopBubbleTask() {
        bubbleJob?.cancel()
        voice.stopSpeaking()
        agentBusy = false
        bubble?.collapse()
    }

    // screen reading

    private fun appRoot(): AccessibilityNodeInfo? {
        rootInActiveWindow?.let { if (it.packageName?.toString() != packageName) return it }
        return windows.asSequence()
            .filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { it.root }
            .firstOrNull { it.packageName?.toString() != packageName }
    }

    fun currentPackage(): String = appRoot()?.packageName?.toString() ?: ""

    /** Plain text of the current app screen, for reading aloud or scam checks. */
    fun screenText(maxChars: Int = 4000): String {
        val root = appRoot() ?: return ""
        val sb = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40 || sb.length > maxChars) return
            if (n.isVisibleToUser) {
                val t = n.text?.toString()?.trim().orEmpty()
                val d = n.contentDescription?.toString()?.trim().orEmpty()
                if (t.isNotEmpty()) sb.append(t).append('\n')
                else if (d.isNotEmpty() && d.length < 80) sb.append(d).append('\n')
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return sb.toString().take(maxChars)
    }

    /**
     * Numbered list of the things on screen the agent can act on.
     * Keeps the node refs so the agent can say "click 7".
     */
    fun snapshot(maxNodes: Int = 140): String {
        val root = appRoot() ?: return ""
        val list = mutableListOf<AccessibilityNodeInfo>()
        val sb = StringBuilder()
        fun label(n: AccessibilityNodeInfo): String {
            val t = n.text?.toString()?.trim().orEmpty()
            val d = n.contentDescription?.toString()?.trim().orEmpty()
            val h = if (android.os.Build.VERSION.SDK_INT >= 26) n.hintText?.toString()?.trim().orEmpty() else ""
            val id = n.viewIdResourceName?.substringAfter(":id/").orEmpty()
            return listOf(t, d, h).firstOrNull { it.isNotEmpty() }?.replace('\n', ' ')?.take(70)
                ?: if (id.isNotEmpty()) "#$id" else ""
        }
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 45 || list.size >= maxNodes) return
            if (n.isVisibleToUser) {
                val lbl = label(n)
                val act = n.isClickable || n.isEditable || n.isCheckable || n.isScrollable || n.isLongClickable
                if (lbl.isNotEmpty() || (act && n.isEditable)) {
                    val idx = list.size
                    list += n
                    val type = n.className?.toString()?.substringAfterLast('.') ?: "View"
                    val flags = buildList {
                        if (n.isClickable) add("tap")
                        if (n.isEditable) add("edit")
                        if (n.isScrollable) add("scroll")
                        if (n.isCheckable) add(if (n.isChecked) "checked" else "unchecked")
                    }.joinToString(",")
                    sb.append("[").append(idx).append("] ").append(type).append(" \"").append(lbl).append("\"")
                    if (flags.isNotEmpty()) sb.append(" (").append(flags).append(")")
                    sb.append('\n')
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        lastNodes = list
        return sb.toString()
    }

    fun nodeLabel(i: Int): String {
        val n = lastNodes.getOrNull(i) ?: return ""
        return listOfNotNull(n.text, n.contentDescription, if (android.os.Build.VERSION.SDK_INT >= 26) n.hintText else null,
            n.viewIdResourceName).joinToString(" ")
    }

    // actions

    suspend fun click(i: Int): Boolean {
        val n = lastNodes.getOrNull(i) ?: return false
        var cur: AccessibilityNodeInfo? = n
        var hops = 0
        while (cur != null && hops < 6) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = cur.parent
            hops++
        }
        val r = Rect().also { n.getBoundsInScreen(it) }
        return tap(r.exactCenterX(), r.exactCenterY())
    }

    suspend fun type(i: Int, text: String): Boolean {
        val n = lastNodes.getOrNull(i) ?: return false
        if (!n.isFocused) n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    suspend fun scroll(forward: Boolean): Boolean {
        val candidates = lastNodes.filter { it.isScrollable }
        val best = candidates.maxByOrNull { Rect().also { r -> it.getBoundsInScreen(r) }.let { r -> r.width() * r.height() } }
        if (best != null && best.performAction(
                if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            )
        ) return true
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val (y1, y2) = if (forward) dm.heightPixels * 0.72f to dm.heightPixels * 0.30f else dm.heightPixels * 0.30f to dm.heightPixels * 0.72f
        return swipe(x, y1, x, y2)
    }

    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)

    /** Click the first node whose id or label matches. Used for WhatsApp's send button. */
    suspend fun clickWhere(timeoutMs: Long = 7000, match: (AccessibilityNodeInfo) -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val root = appRoot()
            val hit = root?.let { findNode(it, match, 0) }
            if (hit != null) {
                var cur: AccessibilityNodeInfo? = hit
                while (cur != null) {
                    if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    cur = cur.parent
                }
            }
            delay(400)
        }
        return false
    }

    private fun findNode(n: AccessibilityNodeInfo, match: (AccessibilityNodeInfo) -> Boolean, depth: Int): AccessibilityNodeInfo? {
        if (depth > 45) return null
        if (n.isVisibleToUser && match(n)) return n
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            findNode(c, match, depth + 1)?.let { return it }
        }
        return null
    }

    private suspend fun tap(x: Float, y: Float): Boolean {
        val p = Path().apply { moveTo(x, y) }
        return gesture(GestureDescription.StrokeDescription(p, 0, 60))
    }

    private suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        return gesture(GestureDescription.StrokeDescription(p, 0, 350))
    }

    private suspend fun gesture(stroke: GestureDescription.StrokeDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val ok = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { if (cont.isActive) cont.resume(true) }
                override fun onCancelled(gestureDescription: GestureDescription?) { if (cont.isActive) cont.resume(false) }
            }, null)
            if (!ok && cont.isActive) cont.resume(false)
        }
}
