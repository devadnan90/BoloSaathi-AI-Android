package com.orynex.bolosaathi.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.agent.AgentRunner
import com.orynex.bolosaathi.agent.SaathiAccessibilityService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

enum class MsgKind { NORMAL, WARNING, SUCCESS, STEP }

data class ChatMsg(
    val fromUser: Boolean,
    val text: String,
    val kind: MsgKind = MsgKind.NORMAL,
    val id: Long = System.nanoTime(),
)

interface SaathiUi {
    fun add(msg: ChatMsg)
    fun status(text: String?)
    /** question == null hides the buttons */
    fun showConfirm(question: String?, onAnswer: ((Boolean) -> Unit)?)
    fun listening(on: Boolean) {}
}

/**
 * The brain. Takes what the user said, works out what they want,
 * and either does it, answers it, or hands it to the phone agent.
 * Every payment or message is confirmed first.
 */
class Saathi(
    private val context: Context,
    val voice: Voice,
    var ui: SaathiUi,
    private val screenProvider: (() -> String)? = null,
) {
    private val app get() = BoloApp.instance
    private val prefs get() = app.prefs
    private val gemini get() = app.gemini
    private val history = ArrayDeque<Turn>()

    private val schemes: String by lazy {
        runCatching { context.assets.open("schemes.json").bufferedReader().use { it.readText() } }.getOrDefault("[]")
    }

    // talking

    suspend fun say(text: String, kind: MsgKind = MsgKind.NORMAL) {
        ui.add(ChatMsg(false, text, kind))
        voice.speak(text)
    }

    suspend fun listenOnce(): String? {
        ui.listening(true)
        ui.status(L.listening(prefs.lang))
        return try {
            voice.listen()
        } finally {
            ui.listening(false)
            ui.status(null)
        }
    }

    suspend fun ask(question: String): String? {
        say(question)
        val a = listenOnce()
        if (a != null) ui.add(ChatMsg(true, a))
        return a
    }

    /** Yes/no by voice or by button, whichever comes first. */
    suspend fun confirm(question: String): Boolean = coroutineScope {
        val btn = CompletableDeferred<Boolean>()
        ui.showConfirm(question) { btn.complete(it) }
        ui.add(ChatMsg(false, question))
        voice.speak(question)
        val byVoice = async {
            repeat(2) { attempt ->
                val heard = listenOnce()
                if (heard != null) ui.add(ChatMsg(true, heard))
                YesNo.parse(heard)?.let { return@async it }
                if (attempt == 0) voice.speak(L.sayYesNo(prefs.lang))
            }
            null
        }
        val result = withTimeoutOrNull(45_000) {
            select<Boolean> {
                btn.onAwait { it }
                byVoice.onAwait { it ?: btn.await() }
            }
        } ?: false
        byVoice.cancel()
        voice.stopListening()
        ui.showConfirm(null, null)
        result
    }

    suspend fun listenAndHandle() {
        val heard = listenOnce()
        if (heard == null) {
            say(L.didntHear(prefs.lang))
            return
        }
        ui.add(ChatMsg(true, heard))
        handle(heard)
    }

    // main entry

    suspend fun handle(utterance: String) {
        if (Extras.isSos(utterance)) { Extras.sos(context, this); return }
        if (!gemini.hasKey()) {
            say(L.noKey(prefs.lang), MsgKind.WARNING); return
        }
        ui.status(L.thinking(prefs.lang))
        val intent = try {
            gemini.json(routerPrompt(), utterance, history.toList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ui.status(null)
            say(L.netError(prefs.lang), MsgKind.WARNING)
            return
        }
        ui.status(null)
        remember("user", utterance)
        val reply = intent.optString("reply")
        remember("model", reply.ifBlank { intent.toString() })

        when (intent.optString("intent")) {
            "send_message" -> sendMessage(intent)
            "pay" -> pay(intent)
            "phone_task" -> phoneTask(intent.optString("goal").ifBlank { utterance }, intent.optString("app"), reply)
            "open_app" -> openApp(intent.optString("app"), reply)
            "read_screen" -> readScreen()
            "scam_check" -> {
                val txt = intent.optString("text").ifBlank { utterance }
                checkScam(txt)
            }
            "reminder" -> Extras.addReminder(context, this, intent)
            "expense" -> Extras.addExpense(context, this, intent)
            "expense_summary" -> Extras.expenseSummary(context, this)
            "sos" -> Extras.sos(context, this)
            "brief" -> Extras.brief(context, this)
            else -> if (reply.isNotBlank()) say(reply) else say(L.didntGet(prefs.lang))
        }
    }

    private fun remember(role: String, text: String) {
        history.addLast(Turn(role, text))
        while (history.size > 12) history.removeFirst()
    }

    // actions

    private suspend fun sendMessage(o: JSONObject) {
        val who = o.optString("contact")
        val msg = o.optString("message")
        val lang = prefs.lang
        if (who.isBlank() || msg.isBlank()) { say(o.optString("reply").ifBlank { L.didntGet(lang) }); return }
        val person = PhoneBook.find(context, prefs, who)
        if (person == null || person.phone.isBlank()) { say(L.noContact(lang, who), MsgKind.WARNING); return }
        val intent = PhoneBook.whatsappIntent(context, person.phone, msg)
        if (intent == null) { say(L.noWhatsapp(lang), MsgKind.WARNING); return }
        if (!confirm(L.confirmMessage(lang, person.name, msg))) { say(L.cancelled(lang)); return }
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { say(L.noWhatsapp(lang)); return }
        val svc = SaathiAccessibilityService.instance
        if (svc != null) {
            val sent = svc.clickWhere(8000) { n ->
                n.viewIdResourceName?.endsWith(":id/send") == true ||
                    n.contentDescription?.toString()?.lowercase()?.let { it == "send" || it == "भेजें" } == true
            }
            if (sent) say(L.messageSent(lang, person.name), MsgKind.SUCCESS) else say(L.pressSend(lang))
        } else {
            say(L.pressSend(lang))
        }
    }

    private suspend fun pay(o: JSONObject) {
        val lang = prefs.lang
        val who = o.optString("contact")
        val amount = o.optInt("amount", 0)
        val person = PhoneBook.find(context, prefs, who)
        if (person == null) { say(L.noContact(lang, who), MsgKind.WARNING); return }
        if (person.upi.isBlank()) { say(L.noUpi(lang, person.name), MsgKind.WARNING); return }
        var amt = amount
        if (amt <= 0) {
            val a = ask(L.howMuch(lang))
            amt = a?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (amt <= 0) { say(L.didntGet(lang)); return }
        }
        if (!confirm(L.confirmPay(lang, person.name, amt))) { say(L.cancelled(lang)); return }
        try {
            context.startActivity(PhoneBook.upiIntent(person.upi, person.name, amt))
            Extras.guardianAlert(context, person.name, amt)
            say(L.enterPinYourself(lang), MsgKind.SUCCESS)
        } catch (e: ActivityNotFoundException) {
            say(L.noUpiApp(lang), MsgKind.WARNING)
        }
    }

    private suspend fun openApp(name: String, reply: String) {
        val i = PhoneBook.launchIntent(context, name)
        if (i == null) { say(L.appNotFound(prefs.lang, name)); return }
        if (reply.isNotBlank()) say(reply)
        context.startActivity(i)
    }

    private suspend fun phoneTask(goal: String, appName: String, reply: String) {
        val lang = prefs.lang
        val svc = SaathiAccessibilityService.instance
        if (svc == null) { say(L.needAccessibility(lang), MsgKind.WARNING); return }
        if (reply.isNotBlank()) say(reply)
        if (appName.isNotBlank()) {
            PhoneBook.launchIntent(context, appName)?.let { context.startActivity(it); delay(1800) }
        }
        val runner = AgentRunner(svc, gemini, lang, object : AgentRunner.Sink {
            override suspend fun say(text: String) = this@Saathi.say(text, MsgKind.STEP)
            override fun status(text: String?) = ui.status(text)
            override suspend fun confirm(question: String) = this@Saathi.confirm(question)
            override suspend fun ask(question: String) = this@Saathi.ask(question)
        })
        svc.agentBusy = true
        try {
            runner.run(goal)
        } finally {
            svc.agentBusy = false
            ui.status(null)
        }
    }

    suspend fun readScreen() {
        val lang = prefs.lang
        val text = screenProvider?.invoke()
        if (text.isNullOrBlank()) { say(L.useBubble(lang)); return }
        ui.status(L.reading(lang))
        val ans = runCatching {
            gemini.text(explainPrompt(), "Screen par ye likha hai:\n$text\n\nIse samjhaiye.")
        }.getOrElse { ui.status(null); say(L.netError(lang), MsgKind.WARNING); return }
        ui.status(null)
        say(ans)
    }

    suspend fun explainImage(bmp: Bitmap) {
        val lang = prefs.lang
        if (!gemini.hasKey()) { say(L.noKey(lang), MsgKind.WARNING); return }
        ui.status(L.reading(lang))
        val ans = runCatching {
            gemini.text(explainPrompt(), "Is photo mein jo kaagaz, bill, dawai ya screen hai, use samjhaiye.", image = bmp)
        }.getOrElse { ui.status(null); say(L.netError(lang), MsgKind.WARNING); return }
        ui.status(null)
        say(ans)
    }

    /** Returns true if it looks like a scam. */
    suspend fun checkScam(text: String): Boolean {
        val lang = prefs.lang
        if (!gemini.hasKey()) { say(L.noKey(lang), MsgKind.WARNING); return false }
        ui.status(L.checking(lang))
        val o = runCatching { gemini.json(scamPrompt(), text) }
            .getOrElse { ui.status(null); say(L.netError(lang), MsgKind.WARNING); return false }
        ui.status(null)
        val risky = o.optString("verdict") == "scam" || o.optString("verdict") == "suspicious"
        say(o.optString("reply"), if (risky) MsgKind.WARNING else MsgKind.SUCCESS)
        return risky
    }

    // prompts

    private fun langRule(): String {
        val l = prefs.lang
        return "Reply in ${l.label} (${l.native}) written in Devanagari script. Talk like a kind helper speaking to an elder who cannot read English: short, simple, warm sentences, no English jargon (say 'पैसे भेजना' not 'transfer'). No markdown, no bullet symbols, no emojis. Numbers as digits."
    }

    private fun routerPrompt(): String {
        val people = prefs.people().joinToString { it.name }.ifBlank { "(none saved)" }
        return """
You are Bolo Saathi, a voice assistant on an Android phone for people in Bihar, UP and nearby who speak Hindi, Bhojpuri or Maithili and cannot read English. The speech was transcribed by a Hindi recogniser, so words may be misspelt; guess the meaning.
${langRule()}

The user's saved people: $people

Decide what the user wants and return ONLY a JSON object:
{
 "intent": one of "send_message" | "pay" | "phone_task" | "open_app" | "read_screen" | "scam_check" | "scheme" | "chat" | "reminder" | "expense" | "expense_summary" | "sos" | "brief",
 "contact": person name as the user said it (for send_message / pay),
 "message": the WhatsApp message text in the user's own words, in Devanagari or as spoken (send_message),
 "amount": integer rupees (pay), 0 if not said,
 "hour": 0-23 and "minute": 0-59 (reminder, 24 hour clock; subah 8 = 8, raat 9 = 21),
 "app": app name in English if an app is needed, e.g. "PhonePe", "WhatsApp", "YouTube", "Paytm", "IRCTC",
 "goal": for phone_task, a clear English instruction for a phone-operating agent, e.g. "Open PhonePe and start a mobile recharge for number 98xxxxxx with the 299 plan, stop before payment",
 "text": for scam_check, the message or offer the user is asking about,
 "reply": what to say back to the user now
}

Rules:
- "send_message": send a WhatsApp message to a person. reply = short ack.
- "pay": send money to a person by UPI. reply = short ack.
- "phone_task": anything that needs tapping around inside an app: mobile recharge, bill payment, booking, searching, playing a video, checking something in an app. reply = short ack like "ठीक है, मैं करता हूँ".
- "open_app": only open an app.
- "read_screen": user asks what is written / what this means / padh ke sunao about the current screen.
- "scam_check": user asks if a call, SMS, offer, link, lottery or request is real or fake. reply = your verdict and advice.
- "scheme": questions about government schemes (yojana), pension, ration, kisan, awas, Ayushman, bima. Use the scheme list below. Ask one simple question at a time (age, farmer or not, BPL/ration card, woman, etc.) if needed to decide. Give name, benefit, documents and where to apply. Up to 6 short sentences. Say amounts can change and to confirm at the CSC centre.
- "chat": anything else, answer briefly and helpfully.
- "reminder": the user wants to be reminded daily (medicine, dawa, BP, sugar etc). Fill "text" with what to remind in their words, plus "hour" and "minute".
- "expense": the user tells money they spent. Fill "amount" and "text" (on what, e.g. sabzi, doodh).
- "expense_summary": the user asks how much they spent (kitna kharcha hua).
- "sos": the user is in danger, fell, is very sick or asks for urgent help.
- "brief": the user asks for today's update, aaj ka haal, mausam, or what is planned today.
- Never ask for or repeat PIN, OTP, CVV or passwords. If someone asks the user for OTP, warn them it is a scam.

Scheme list (JSON): $schemes
""".trimIndent()
    }

    private fun explainPrompt() = """
You help someone who cannot read English understand what is on their phone screen or in a photo (bill, letter, medicine strip, form, SMS).
${langRule()}
Say in 2 to 5 short sentences: what this is, the important numbers or dates, and what they need to do, if anything. If it looks like a scam (lottery, KYC block threat, OTP request, unknown payment request, too-good offer) start with a clear warning. Do not read out long codes or IDs.
""".trimIndent()

    private fun scamPrompt() = """
You check messages, calls and offers for fraud for people in rural India. Common scams: fake KYC/account block SMS, electricity cut threats, lottery/prize, job offers asking fees, UPI collect requests, "sent money by mistake, return it", OTP/PIN requests, fake customer care, loan apps, links from unknown numbers.
${langRule()}
Return ONLY JSON: {"verdict": "scam" | "suspicious" | "safe", "reply": "2 to 4 short sentences: verdict first, why, and exactly what to do (e.g. don't click, don't share OTP, call 1930 for cyber fraud)."}
""".trimIndent()
}
