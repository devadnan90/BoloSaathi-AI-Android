package com.orynex.bolosaathi.agent

import com.orynex.bolosaathi.core.Lang

/**
 * Fast on-device checks that run while the user is inside a UPI app.
 * No network needed, so the warning comes before the PIN screen.
 */
object ScamRules {

    class Hit(val code: String, private val hi: String, private val bho: String, private val mai: String) {
        fun message(lang: Lang) = when (lang) {
            Lang.HINDI -> hi
            Lang.BHOJPURI -> bho
            Lang.MAITHILI -> mai
        }
    }

    private val collect = Hit(
        "collect",
        "रुकिए! यह पैसे माँगने की रिक्वेस्ट है। यहाँ पिन डालने से पैसे आपके खाते से कटेंगे, आएँगे नहीं। अगर आप इन्हें नहीं जानते तो मना कर दीजिए।",
        "रुकीं! ई पइसा माँगे वाला रिक्वेस्ट बा। इहाँ पिन डालब त पइसा रउरा खाता से कटी, आई ना। अगर इनका के ना जानीं त मना कर दीं।",
        "रुकू! ई पाइ माँगबाक रिक्वेस्ट अछि। एतय पिन देब त पाइ अहाँक खाता सँ कटत, आओत नहि। जँ हिनका नहि चिन्हैत छी त मना क दिअ।"
    )

    private val receivePin = Hit(
        "receive_pin",
        "ध्यान दीजिए! पैसे लेने के लिए कभी पिन नहीं डालना पड़ता। पिन सिर्फ पैसे भेजने के लिए होता है। यह धोखा हो सकता है।",
        "ध्यान दीं! पइसा लेवे खातिर कबो पिन ना डाले के पड़ेला। पिन खाली पइसा भेजे खातिर होला। ई धोखा हो सकेला।",
        "ध्यान दिअ! पाइ लेबाक लेल कहियो पिन नहि देबऽ पड़ैत छैक। पिन मात्र पाइ पठेबाक लेल होइत छैक। ई धोखा भ सकैत अछि।"
    )

    fun check(screen: String): Hit? {
        val t = screen.lowercase()
        val money = t.contains("₹") || t.contains("rs") || t.contains("inr")
        val requestWords = listOf("request", "requested", "collect", "अनुरोध", "रिक्वेस्ट", "requesting")
        val approveWords = listOf("approve", "accept", "pay now", "proceed to pay", "स्वीकार", "भुगतान करें", "pay ₹")
        if (money && requestWords.any { t.contains(it) } && approveWords.any { t.contains(it) }) return collect

        val lureWords = listOf("cashback", "prize", "lottery", "reward", "refund", "you won", "you have won", "receive money", "inaam", "इनाम", "लॉटरी", "कैशबैक", "जीत")
        val pinWords = listOf("upi pin", "enter pin", "पिन")
        if (lureWords.any { t.contains(it) } && pinWords.any { t.contains(it) }) return receivePin
        return null
    }
}
