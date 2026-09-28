package com.orynex.bolosaathi.agent

import com.orynex.bolosaathi.core.Gemini
import com.orynex.bolosaathi.core.L
import com.orynex.bolosaathi.core.Lang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * The phone-operating loop: look at the screen, ask Gemini for one step,
 * do it, check again. Stops before anything risky and asks the user.
 */
class AgentRunner(
    private val svc: SaathiAccessibilityService,
    private val gemini: Gemini,
    private val lang: Lang,
    private val sink: Sink,
) {
    interface Sink {
        suspend fun say(text: String)
        fun status(text: String?)
        suspend fun confirm(question: String): Boolean
        suspend fun ask(question: String): String?
    }

    private val sensitive = Regex("(?i)(\\bpin\\b|otp|password|passcode|cvv|पिन|ओटीपी|पासवर्ड|mpin|upi pin)")

    suspend fun run(goal: String, maxSteps: Int = 18): Boolean {
        val done = mutableListOf<String>()
        var emptyScreens = 0
        for (step in 1..maxSteps) {
            delay(1300)
            val screen = svc.snapshot()
            if (screen.isBlank()) {
                if (++emptyScreens > 3) break
                continue
            }
            if (Regex("(?i)(enter (your )?upi pin|enter pin|upi पिन|ENTER 4-DIGIT|ENTER 6-DIGIT)").containsMatchIn(svc.screenText(2500))) {
                sink.say(L.pinStop(lang)); return true
            }
            sink.status("कदम $step…")
            val o = try {
                gemini.json(system(), user(goal, done, screen, svc.currentPackage()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sink.say(L.netError(lang)); return false
            }
            val action = o.optString("action")
            val idx = o.optInt("index", -1)
            val text = o.optString("text")
            val say = o.optString("say")
            val risky = o.optBoolean("risky", false)

            if (risky && action in setOf("tap", "type")) {
                val q = say.ifBlank { "आगे बढ़ूँ?" }
                if (!sink.confirm(q)) { sink.say(L.cancelled(lang)); return false }
            } else if (say.isNotBlank() && action !in setOf("done", "fail", "ask")) {
                sink.status(say)
            }

            val ok: Boolean = when (action) {
                "tap" -> svc.click(idx)
                "type" -> {
                    if (sensitive.containsMatchIn(svc.nodeLabel(idx)) || sensitive.containsMatchIn(text)) {
                        sink.say(L.pinStop(lang)); return true
                    }
                    svc.type(idx, text)
                }
                "scroll_down" -> svc.scroll(true)
                "scroll_up" -> svc.scroll(false)
                "back" -> svc.back()
                "wait" -> { delay(1500); true }
                "ask" -> {
                    val ans = sink.ask(say.ifBlank { "क्या करूँ?" })
                    done += "Asked user: \"$say\" -> user answered: \"${ans ?: "(no answer)"}\""
                    continue
                }
                "done" -> { if (say.isNotBlank()) sink.say(say); return true }
                "fail" -> { sink.say(say.ifBlank { L.agentGiveUp(lang) }); return false }
                else -> false
            }
            done += "Step $step: $action ${if (idx >= 0) "[$idx]" else ""} ${if (text.isNotBlank()) "\"$text\"" else ""} -> ${if (ok) "ok" else "FAILED"}"
            if (done.size > 10) done.removeAt(0)
        }
        sink.say(L.agentGiveUp(lang))
        return false
    }

    private fun system() = """
You operate an Android phone for an elderly Indian user who cannot read English. You see the current screen as a numbered list of elements and pick ONE next step toward the goal.

Return ONLY JSON:
{"thought": "short reasoning in English",
 "action": "tap" | "type" | "scroll_down" | "scroll_up" | "back" | "wait" | "ask" | "done" | "fail",
 "index": element number for tap/type, else -1,
 "text": text to type (type only),
 "risky": true if this step sends money, pays, sends a message, submits a form, books, or confirms a purchase,
 "say": a very short sentence in ${lang.label} (Devanagari) for the user: for risky steps a yes/no question describing exactly what will happen (amount, person, plan); for ask a simple question; for done/fail a summary; otherwise a short status}

Rules:
- Prefer tapping clearly labelled buttons. If a popup, ad or permission dialog blocks the way, close or allow it sensibly.
- Never type PIN, OTP, CVV or passwords. If the next step needs one, return done and tell the user to enter it themselves.
- If you need information you don't have (mobile number, plan, name), use ask.
- If the goal is already achieved or the app is now waiting for the user's PIN, return done.
- If the same step failed twice or you are stuck, try back or scroll; after that return fail.
""".trimIndent()

    private fun user(goal: String, done: List<String>, screen: String, pkg: String) = buildString {
        append("GOAL: ").append(goal).append("\n\n")
        append("CURRENT APP: ").append(pkg).append("\n\n")
        append("STEPS SO FAR:\n").append(if (done.isEmpty()) "(none)" else done.joinToString("\n")).append("\n\n")
        append("SCREEN:\n").append(screen.take(9000))
    }
}
