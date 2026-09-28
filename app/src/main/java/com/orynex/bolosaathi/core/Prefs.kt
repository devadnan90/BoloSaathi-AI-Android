package com.orynex.bolosaathi.core

import android.content.Context
import com.orynex.bolosaathi.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

data class Person(val name: String, val phone: String, val upi: String)

enum class Lang(val code: String, val label: String, val native: String) {
    HINDI("hi", "Hindi", "हिंदी"),
    BHOJPURI("bho", "Bhojpuri", "भोजपुरी"),
    MAITHILI("mai", "Maithili", "मैथिली");

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: HINDI
    }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("bolo", Context.MODE_PRIVATE)

    var lang: Lang
        get() = Lang.from(sp.getString("lang", "hi"))
        set(v) = sp.edit().putString("lang", v.code).apply()

    var userKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v.trim()).apply()

    var scamGuard: Boolean
        get() = sp.getBoolean("scam_guard", true)
        set(v) = sp.edit().putBoolean("scam_guard", v).apply()

    var bubble: Boolean
        get() = sp.getBoolean("bubble", true)
        set(v) = sp.edit().putBoolean("bubble", v).apply()

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(v) = sp.edit().putString("user_name", v.trim()).apply()

    var userPhone: String
        get() = sp.getString("user_phone", "") ?: ""
        set(v) = sp.edit().putString("user_phone", v.trim()).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    fun apiKey(): String = userKey.ifBlank { BuildConfig.GEMINI_API_KEY }

    fun people(): List<Person> {
        val raw = sp.getString("people", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Person(o.optString("name"), o.optString("phone"), o.optString("upi"))
            }
        }.getOrDefault(emptyList())
    }

    fun savePeople(list: List<Person>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("name", it.name).put("phone", it.phone).put("upi", it.upi))
        }
        sp.edit().putString("people", arr.toString()).apply()
    }
}
