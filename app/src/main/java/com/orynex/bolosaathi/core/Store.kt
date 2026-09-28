package com.orynex.bolosaathi.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Reminder(val id: Int, val text: String, val hour: Int, val minute: Int)
data class Expense(val amount: Int, val note: String, val time: Long)

/** Extra feature settings and data, kept only on the phone. */
class Store(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("bolo_extra", Context.MODE_PRIVATE)

    var guardian: String
        get() = sp.getString("guardian", "") ?: ""
        set(v) = sp.edit().putString("guardian", v.filter { it.isDigit() }).apply()

    var briefOn: Boolean
        get() = sp.getBoolean("brief_on", false)
        set(v) = sp.edit().putBoolean("brief_on", v).apply()

    var briefHour: Int
        get() = sp.getInt("brief_hour", 8)
        set(v) = sp.edit().putInt("brief_hour", v).apply()

    var readNotifs: Boolean
        get() = sp.getBoolean("read_notifs", false)
        set(v) = sp.edit().putBoolean("read_notifs", v).apply()

    var callWarn: Boolean
        get() = sp.getBoolean("call_warn", false)
        set(v) = sp.edit().putBoolean("call_warn", v).apply()

    var bigPayAlert: Boolean
        get() = sp.getBoolean("big_pay", true)
        set(v) = sp.edit().putBoolean("big_pay", v).apply()

    var lastCallNumber: String
        get() = sp.getString("last_call", "") ?: ""
        set(v) = sp.edit().putString("last_call", v).apply()

    var callWasActive: Boolean
        get() = sp.getBoolean("call_active", false)
        set(v) = sp.edit().putBoolean("call_active", v).apply()

    // reminders
    fun reminders(): List<Reminder> = runCatching {
        val a = JSONArray(sp.getString("reminders", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Reminder(o.getInt("id"), o.getString("text"), o.getInt("h"), o.getInt("m"))
        }
    }.getOrDefault(emptyList())

    private fun saveReminders(list: List<Reminder>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("h", it.hour).put("m", it.minute)) }
        sp.edit().putString("reminders", a.toString()).apply()
    }

    fun addReminder(text: String, hour: Int, minute: Int): Reminder {
        val list = reminders()
        val r = Reminder((list.maxOfOrNull { it.id } ?: 1000) + 1, text, hour, minute)
        saveReminders(list + r)
        return r
    }

    fun removeReminder(id: Int) = saveReminders(reminders().filter { it.id != id })

    // expenses
    fun expenses(): List<Expense> = runCatching {
        val a = JSONArray(sp.getString("expenses", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Expense(o.getInt("amt"), o.getString("note"), o.getLong("t"))
        }
    }.getOrDefault(emptyList())

    private fun saveExpenses(list: List<Expense>) {
        val a = JSONArray()
        list.takeLast(500).forEach { a.put(JSONObject().put("amt", it.amount).put("note", it.note).put("t", it.time)) }
        sp.edit().putString("expenses", a.toString()).apply()
    }

    fun addExpense(amount: Int, note: String) = saveExpenses(expenses() + Expense(amount, note.trim(), System.currentTimeMillis()))

    fun removeExpense(time: Long) = saveExpenses(expenses().filter { it.time != time })
}
