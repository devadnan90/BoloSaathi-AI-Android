package com.orynex.bolosaathi

import android.app.Application
import com.orynex.bolosaathi.core.Gemini
import com.orynex.bolosaathi.core.Prefs

class BoloApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var gemini: Gemini
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        gemini = Gemini { prefs.apiKey() }
        com.orynex.bolosaathi.core.Extras.rescheduleAll(this)
    }

    companion object {
        lateinit var instance: BoloApp
            private set
    }
}
