package com.orynex.bolosaathi.core

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.TelephonyManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.MainActivity
import com.orynex.bolosaathi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Speaks once from the background, then frees the TTS engine. */
object BgSpeaker {
    suspend fun speak(ctx: Context, text: String) {
        withTimeoutOrNull(45_000) {
            suspendCancellableCoroutine<Unit> { cont ->
                var tts: TextToSpeech? = null
                tts = TextToSpeech(ctx.applicationContext) { status ->
                    if (status != TextToSpeech.SUCCESS) {
                        if (cont.isActive) cont.resume(Unit)
                        return@TextToSpeech
                    }
                    tts?.language = Locale("hi", "IN")
                    tts?.setSpeechRate(0.92f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            tts?.shutdown()
                            if (cont.isActive) cont.resume(Unit)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            tts?.shutdown()
                            if (cont.isActive) cont.resume(Unit)
                        }
                    })
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bg")
                }
                cont.invokeOnCancellation { tts?.shutdown() }
            }
        }
    }
}

/** Medicine reminders, morning brief, and re-arming alarms after reboot. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) { Extras.rescheduleAll(ctx); return }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.getStringExtra("kind")) {
                    "reminder" -> {
                        val id = intent.getIntExtra("id", 0)
                        val r = Store(ctx).reminders().firstOrNull { it.id == id }
                        if (r != null) {
                            val t = "याद दिला रहा हूँ: ${r.text} का समय हो गया है।"
                            Extras.notify(ctx, id, "दवा का समय", t)
                            BgSpeaker.speak(ctx, t)
                            Extras.schedule(ctx, r)
                        }
                    }
                    "brief" -> {
                        val t = Extras.buildBrief(ctx)
                        Extras.notify(ctx, 1, "आज का हाल", t)
                        BgSpeaker.speak(ctx, t)
                        Extras.scheduleBrief(ctx)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** After a call with an unknown number, warns about common phone scams. */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val st = Store(ctx)
        if (!st.callWarn) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        if (!num.isNullOrBlank()) st.lastCallNumber = num
        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> st.callWasActive = true
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (!st.callWasActive) return
                st.callWasActive = false
                val n = st.lastCallNumber
                st.lastCallNumber = ""
                if (n.isBlank() || known(ctx, n)) return
                val t = "अभी एक अनजान नंबर से बात हुई। अगर उन्होंने ओटीपी, पिन, केवाईसी या पैसे माँगे, तो वह धोखा है। कुछ भी न बताएँ। शक हो तो 1930 पर कॉल करें।"
                Extras.notify(ctx, 2, "अनजान कॉल: सावधान", t)
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try { BgSpeaker.speak(ctx, t) } finally { pending.finish() }
                }
            }
        }
    }

    private fun known(ctx: Context, n: String): Boolean {
        val d = n.filter { it.isDigit() }.takeLast(10)
        if (BoloApp.instance.prefs.people().any { it.phone.filter { c -> c.isDigit() }.takeLast(10) == d }) return true
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return false
        return runCatching {
            ctx.contentResolver.query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(n)),
                arrayOf(ContactsContract.PhoneLookup._ID), null, null, null,
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }
}

/** Reads new WhatsApp and SMS messages aloud. Never reads OTP messages. */
class SaathiNotifyService : NotificationListenerService() {
    private val apps = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging",
        "com.samsung.android.messaging", "com.android.mms",
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var last = ""

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!Store(this).readNotifs || sbn.packageName !in apps || sbn.isOngoing) return
        val ex = sbn.notification.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank() || Regex("(?i)\\d+ new messages|\\d+ नए संदेश").containsMatchIn(text)) return
        val key = title + text
        if (key == last) return
        last = key
        val app = if (sbn.packageName.contains("whatsapp")) "व्हाट्सऐप" else "मैसेज"
        val speech = if (Regex("(?i)(otp|\\bpin\\b|password|cvv|ओटीपी|पिन)").containsMatchIn(text))
            "$app पर एक ओटीपी वाला मैसेज आया है। इसे किसी को न बताएँ।"
        else "$app पर $title का संदेश: $text"
        scope.launch { BgSpeaker.speak(this@SaathiNotifyService, speech) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** Home screen widget: one big mic that opens the app ready to listen. */
class MicWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pi = PendingIntent.getActivity(
            ctx, 7,
            Intent(ctx, MainActivity::class.java).putExtra("listen", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        ids.forEach { id ->
            val rv = RemoteViews(ctx.packageName, R.layout.widget_mic)
            rv.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, rv)
        }
    }
}
