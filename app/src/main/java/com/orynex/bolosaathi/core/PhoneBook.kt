package com.orynex.bolosaathi.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat

object PhoneBook {

    private val relationAliases = mapOf(
        "mummy" to listOf("mummy", "maa", "mom", "mother", "amma", "mai", "माँ", "मम्मी", "अम्मा", "माई"),
        "papa" to listOf("papa", "pitaji", "father", "dad", "babuji", "पापा", "पिताजी", "बाबूजी", "बाबू"),
        "beta" to listOf("beta", "son", "बेटा", "बबुआ"),
        "beti" to listOf("beti", "daughter", "बेटी", "बिटिया"),
    )

    /** Look in the user's saved people first, then the phone contacts. */
    fun find(context: Context, prefs: Prefs, spoken: String): Person? {
        val q = spoken.trim().lowercase()
        if (q.isBlank()) return null
        val people = prefs.people()
        people.firstOrNull { it.name.lowercase() == q }?.let { return it }
        people.firstOrNull { it.name.lowercase().contains(q) || q.contains(it.name.lowercase()) }?.let { return it }
        // relation words: "mummy" could be saved as "Maa"
        relationAliases.values.firstOrNull { group -> group.any { q.contains(it) } }?.let { group ->
            people.firstOrNull { p -> group.any { p.name.lowercase().contains(it) } }?.let { return it }
        }
        return fromContacts(context, q)
    }

    private fun fromContacts(context: Context, q: String): Person? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        return runCatching {
            context.contentResolver.query(
                uri, proj,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$q%"), null
            )?.use { c ->
                if (c.moveToFirst()) Person(c.getString(0), c.getString(1), "") else null
            }
        }.getOrNull()
    }

    fun normalizeIndian(phone: String): String? {
        val d = phone.filter { it.isDigit() }
        return when {
            d.length == 10 -> "91$d"
            d.length == 12 && d.startsWith("91") -> d
            d.length == 11 && d.startsWith("0") -> "91${d.drop(1)}"
            d.length > 10 -> "91${d.takeLast(10)}"
            else -> null
        }
    }

    fun whatsappIntent(context: Context, phone: String, text: String): Intent? {
        val num = normalizeIndian(phone) ?: return null
        val pkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { isInstalled(context, it) } ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$num&text=${Uri.encode(text)}"))
            .setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun upiIntent(upi: String, name: String, amount: Int): Intent {
        val uri = Uri.Builder().scheme("upi").authority("pay")
            .appendQueryParameter("pa", upi)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("am", "$amount.00")
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", "Bolo Saathi")
            .build()
        return Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "UPI app chuniye")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun isInstalled(context: Context, pkg: String) = runCatching {
        context.packageManager.getPackageInfo(pkg, 0); true
    }.getOrDefault(false)

    private val knownApps = mapOf(
        "whatsapp" to "com.whatsapp",
        "phonepe" to "com.phonepe.app",
        "phone pe" to "com.phonepe.app",
        "google pay" to "com.google.android.apps.nbu.paisa.user",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "paytm" to "net.one97.paytm",
        "bhim" to "in.org.npci.upiapp",
        "youtube" to "com.google.android.youtube",
        "irctc" to "cris.org.in.prs.ima",
        "jio" to "com.jio.myjio",
        "myjio" to "com.jio.myjio",
        "airtel" to "com.myairtelapp",
        "umang" to "in.gov.umang.negd.g2c",
        "digilocker" to "com.digilocker.android",
        "aadhaar" to "in.gov.uidai.mAadhaarPlus",
        "maps" to "com.google.android.apps.maps",
        "camera" to "",
    )

    /** Find an installed app by what the user called it. Returns its launch intent. */
    fun launchIntent(context: Context, spoken: String): Intent? {
        val q = spoken.lowercase().trim()
        if (q.isBlank()) return null
        val pm = context.packageManager
        knownApps.entries.firstOrNull { q.contains(it.key) }?.value?.takeIf { it.isNotBlank() }?.let { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let { return it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val hit = apps.firstOrNull { it.loadLabel(pm).toString().lowercase() == q }
            ?: apps.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(q) }
            ?: apps.firstOrNull { q.contains(it.loadLabel(pm).toString().lowercase()) }
        return hit?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    data class Sms(val from: String, val body: String, val date: Long)

    fun recentSms(context: Context, limit: Int = 25): List<Sms> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null, null, "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                val out = mutableListOf<Sms>()
                while (c.moveToNext() && out.size < limit) out += Sms(c.getString(0) ?: "", c.getString(1) ?: "", c.getLong(2))
                out
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
