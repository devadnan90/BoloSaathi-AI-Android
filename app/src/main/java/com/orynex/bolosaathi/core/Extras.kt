package com.orynex.bolosaathi.core

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.MainActivity
import com.orynex.bolosaathi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Extras {
    const val CHANNEL = "saathi_alerts"
    private const val BRIEF_ID = 1

    private fun has(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    // notifications
    fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, "Bolo Saathi", NotificationManager.IMPORTANCE_HIGH))
        }
        if (Build.VERSION.SDK_INT >= 33 && !has(ctx, Manifest.permission.POST_NOTIFICATIONS)) return
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.bubble_mic)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }

    // time helpers
    fun timeText(h: Int, m: Int): String {
        val part = when (h) { in 4..11 -> "सुबह"; in 12..15 -> "दोपहर"; in 16..19 -> "शाम"; else -> "रात" }
        val hh = if (h % 12 == 0) 12 else h % 12
        return if (m == 0) "$part $hh बजे" else "$part $hh:${"%02d".format(m)} बजे"
    }

    fun dayStart(daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysAgo)
    }.timeInMillis

    fun monthStart(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // alarms
    private fun alarmIntent(ctx: Context, id: Int, kind: String, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            ctx, id,
            Intent(ctx, AlarmReceiver::class.java).putExtra("kind", kind).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or flags,
        )

    private fun nextAt(h: Int, m: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun schedule(ctx: Context, r: Reminder) {
        val pi = alarmIntent(ctx, r.id, "reminder", PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        setAlarm(ctx, nextAt(r.hour, r.minute), pi)
    }

    fun cancel(ctx: Context, id: Int) {
        val pi = alarmIntent(ctx, id, "reminder", PendingIntent.FLAG_NO_CREATE) ?: return
        ctx.getSystemService(AlarmManager::class.java).cancel(pi)
    }

    fun scheduleBrief(ctx: Context) {
        val st = Store(ctx)
        if (!st.briefOn) {
            alarmIntent(ctx, BRIEF_ID, "brief", PendingIntent.FLAG_NO_CREATE)?.let {
                ctx.getSystemService(AlarmManager::class.java).cancel(it)
            }
            return
        }
        val pi = alarmIntent(ctx, BRIEF_ID, "brief", PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        setAlarm(ctx, nextAt(st.briefHour, 0), pi)
    }

    fun rescheduleAll(ctx: Context) {
        Store(ctx).reminders().forEach { schedule(ctx, it) }
        scheduleBrief(ctx)
    }

    // voice actions
    suspend fun addReminder(ctx: Context, s: Saathi, o: JSONObject) {
        val text = o.optString("text").ifBlank { "दवा" }
        val h = o.optInt("hour", -1)
        val m = o.optInt("minute", 0).coerceIn(0, 59)
        if (h !in 0..23) { s.say(o.optString("reply").ifBlank { "किस समय याद दिलाऊँ? जैसे सुबह 8 बजे।" }); return }
        val r = Store(ctx).addReminder(text, h, m)
        schedule(ctx, r)
        s.say("ठीक है, रोज़ ${timeText(h, m)} $text की याद दिलाऊँगा।", MsgKind.SUCCESS)
    }

    suspend fun addExpense(ctx: Context, s: Saathi, o: JSONObject) {
        val amt = o.optInt("amount", 0)
        if (amt <= 0) { s.say("कितने रुपये खर्च हुए? फिर से बताइए।"); return }
        val note = o.optString("text").ifBlank { "खर्च" }
        val st = Store(ctx)
        st.addExpense(amt, note)
        val today = st.expenses().filter { it.time >= dayStart(0) }.sumOf { it.amount }
        s.say("लिख लिया: $amt रुपये, $note। आज का कुल $today रुपये।", MsgKind.SUCCESS)
    }

    suspend fun expenseSummary(ctx: Context, s: Saathi) {
        val list = Store(ctx).expenses()
        val today = list.filter { it.time >= dayStart(0) }.sumOf { it.amount }
        val monthList = list.filter { it.time >= monthStart() }
        val month = monthList.sumOf { it.amount }
        if (month == 0) { s.say("इस महीने अभी तक कोई खर्च नहीं लिखा।"); return }
        val top = monthList.groupBy { it.note }.mapValues { e -> e.value.sumOf { it.amount } }.maxByOrNull { it.value }
        s.say("आज $today रुपये और इस महीने $month रुपये खर्च हुए।" + (top?.let { " सबसे ज़्यादा ${it.key} पर ${it.value} रुपये।" } ?: ""))
    }

    fun isSos(t: String): Boolean {
        val x = t.lowercase()
        return listOf("बचाओ", "मदद करो", "मदद चाहिए", "इमरजेंसी", "sos", "help me", "bachao", "madad karo", "emergency").any { x.contains(it) }
    }

    suspend fun sos(ctx: Context, s: Saathi) {
        val num = Store(ctx).guardian
        if (num.length < 10) { s.say("सेटिंग में परिवार का नंबर डालिए, तभी मैं मदद भेज पाऊँगा।", MsgKind.WARNING); return }
        s.say("परिवार को संदेश और कॉल जा रही है। रोकना हो तो 'रुको' बोलिए।", MsgKind.WARNING)
        if (YesNo.parse(s.listenOnce()) == false) { s.say("ठीक है, नहीं भेजा।"); return }
        val loc = lastLocation(ctx)
        val name = BoloApp.instance.prefs.userName.ifBlank { "आपके परिवार वाले" }
        val msg = "SOS! $name को तुरंत मदद चाहिए।" +
            (loc?.let { " लोकेशन: https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: "") + " (Bolo Saathi)"
        val sent = sendSms(ctx, num, msg)
        s.say(if (sent) "परिवार को संदेश भेज दिया। अब कॉल लगा रहा हूँ।" else "संदेश नहीं जा सका, कॉल लगा रहा हूँ।", MsgKind.WARNING)
        call(ctx, num)
    }

    fun sendSms(ctx: Context, num: String, msg: String): Boolean {
        if (!has(ctx, Manifest.permission.SEND_SMS)) return false
        return try {
            @Suppress("DEPRECATION")
            val sm = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            sm.sendMultipartTextMessage(num, null, sm.divideMessage(msg), null, null)
            true
        } catch (_: Exception) { false }
    }

    private fun call(ctx: Context, num: String) {
        val uri = Uri.parse("tel:$num")
        val i = if (has(ctx, Manifest.permission.CALL_PHONE)) Intent(Intent.ACTION_CALL, uri) else Intent(Intent.ACTION_DIAL, uri)
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }

    fun lastLocation(ctx: Context): Location? {
        if (!has(ctx, Manifest.permission.ACCESS_FINE_LOCATION) && !has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null } }
            .maxByOrNull { it.time }
    }

    /** Tell the family by SMS when a big UPI payment is being made. */
    fun guardianAlert(ctx: Context, to: String, amt: Int) {
        val st = Store(ctx)
        if (!st.bigPayAlert || amt < 2000 || st.guardian.length < 10) return
        val name = BoloApp.instance.prefs.userName.ifBlank { "आपके परिवार वाले" }
        sendSms(ctx, st.guardian, "Bolo Saathi: $name ने $to को $amt रुपये भेजने के लिए UPI खोला है। ठीक न लगे तो उन्हें फ़ोन करें।")
    }

    // morning brief
    suspend fun brief(ctx: Context, s: Saathi) = s.say(buildBrief(ctx))

    suspend fun buildBrief(ctx: Context): String {
        val name = BoloApp.instance.prefs.userName
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val parts = mutableListOf<String>()
        parts += (if (h < 12) "सुप्रभात" else "नमस्ते") + (if (name.isNotBlank()) " $name जी।" else "।")
        parts += "आज " + SimpleDateFormat("d MMMM, EEEE", Locale("hi", "IN")).format(Date()) + " है।"
        weather(ctx)?.let { parts += it }
        val rem = Store(ctx).reminders().sortedBy { it.hour * 60 + it.minute }
        if (rem.isNotEmpty()) parts += "आज की याद: " + rem.joinToString(", ") { "${timeText(it.hour, it.minute)} ${it.text}" } + "।"
        val y = Store(ctx).expenses().filter { it.time >= dayStart(1) && it.time < dayStart(0) }.sumOf { it.amount }
        if (y > 0) parts += "कल आपने $y रुपये खर्च किए।"
        parts += "किसी अनजान को ओटीपी या पिन न बताएँ।"
        return parts.joinToString(" ")
    }

    private suspend fun weather(ctx: Context): String? = withContext(Dispatchers.IO) {
        val loc = lastLocation(ctx) ?: return@withContext null
        runCatching {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=1"
            OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { r ->
                val d = JSONObject(r.body!!.string()).getJSONObject("daily")
                val max = d.getJSONArray("temperature_2m_max").getDouble(0).toInt()
                val min = d.getJSONArray("temperature_2m_min").getDouble(0).toInt()
                val rain = d.getJSONArray("precipitation_probability_max").optInt(0, 0)
                "आज तापमान $min से $max डिग्री रहेगा" + if (rain >= 50) ", बारिश हो सकती है, छाता रखिए।" else "।"
            }
        }.getOrNull()
    }
}
