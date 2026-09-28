package com.orynex.bolosaathi.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orynex.bolosaathi.BoloApp
import com.orynex.bolosaathi.agent.SaathiAccessibilityService
import com.orynex.bolosaathi.core.ChatMsg
import com.orynex.bolosaathi.core.Lang
import com.orynex.bolosaathi.core.Person
import com.orynex.bolosaathi.core.PhoneBook
import com.orynex.bolosaathi.core.Saathi
import com.orynex.bolosaathi.core.SaathiUi
import com.orynex.bolosaathi.core.Voice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Confirm(val question: String, val answer: (Boolean) -> Unit)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = BoloApp.instance.prefs
    val voice = Voice(app)

    private val _messages = MutableStateFlow<List<ChatMsg>>(emptyList())
    val messages: StateFlow<List<ChatMsg>> = _messages.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()
    private val _confirm = MutableStateFlow<Confirm?>(null)
    val confirm = _confirm.asStateFlow()
    private val _listening = MutableStateFlow(false)
    val listening = _listening.asStateFlow()
    private val _level = MutableStateFlow(0f)
    val level = _level.asStateFlow()
    private val _partial = MutableStateFlow("")
    val partial = _partial.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    val lang = MutableStateFlow(prefs.lang)
    val people = MutableStateFlow(prefs.people())
    val a11yOn = MutableStateFlow(SaathiAccessibilityService.connected)
    val legal = MutableStateFlow<Pair<String, String>?>(null)
    val diaryOpen = MutableStateFlow(false)

    // scam screen has its own result area
    private val _scamResult = MutableStateFlow<ChatMsg?>(null)
    val scamResult = _scamResult.asStateFlow()
    private val _scamBusy = MutableStateFlow(false)
    val scamBusy = _scamBusy.asStateFlow()

    private val homeUi = object : SaathiUi {
        override fun add(msg: ChatMsg) { _messages.update { it + msg } }
        override fun status(text: String?) { _status.value = text }
        override fun showConfirm(question: String?, onAnswer: ((Boolean) -> Unit)?) {
            _confirm.value = if (question != null && onAnswer != null) Confirm(question, onAnswer) else null
        }
        override fun listening(on: Boolean) { _listening.value = on; if (!on) _partial.value = "" }
    }

    private val scamUi = object : SaathiUi {
        override fun add(msg: ChatMsg) { if (!msg.fromUser) _scamResult.value = msg }
        override fun status(text: String?) {}
        override fun showConfirm(question: String?, onAnswer: ((Boolean) -> Unit)?) {}
    }

    private val saathi = Saathi(app, voice, homeUi)
    private val scamSaathi = Saathi(app, voice, scamUi)
    private var job: Job? = null

    init {
        voice.onLevel = { _level.value = it }
        voice.onPartial = { _partial.value = it }
    }

    fun refresh() {
        a11yOn.value = SaathiAccessibilityService.connected
        people.value = prefs.people()
    }

    private fun run(block: suspend () -> Unit) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _busy.value = true
            try { block() } finally { _busy.value = false; _status.value = null; _confirm.value = null }
        }
    }

    fun micTap() {
        if (_listening.value) { voice.stopListening(); return }
        if (job?.isActive == true) { stop(); return }
        run { saathi.listenAndHandle() }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        run {
            homeUi.add(ChatMsg(true, text.trim()))
            saathi.handle(text.trim())
        }
    }

    fun explainPhoto(bmp: Bitmap) = run {
        homeUi.add(ChatMsg(true, "(फ़ोटो दिखाई)"))
        saathi.explainImage(bmp)
    }

    fun sos() = run { com.orynex.bolosaathi.core.Extras.sos(getApplication(), saathi) }

    fun stop() {
        job?.cancel()
        voice.stopSpeaking()
        voice.stopListening()
        _busy.value = false
        _listening.value = false
        _confirm.value = null
        _status.value = null
    }

    fun clearChat() { stop(); _messages.value = emptyList() }

    fun checkScam(text: String) {
        if (text.isBlank() || _scamBusy.value) return
        viewModelScope.launch {
            _scamBusy.value = true
            _scamResult.value = null
            try { scamSaathi.checkScam(text) } finally { _scamBusy.value = false }
        }
    }

    fun recentSms() = PhoneBook.recentSms(getApplication())

    fun setLang(l: Lang) { prefs.lang = l; lang.value = l }

    fun savePerson(old: Person?, p: Person) {
        val list = prefs.people().toMutableList()
        if (old != null) list.remove(old)
        list.add(0, p)
        prefs.savePeople(list)
        people.value = list
    }

    fun deletePerson(p: Person) {
        val list = prefs.people().filter { it != p }
        prefs.savePeople(list)
        people.value = list
    }

    fun speakTest() = viewModelScope.launch {
        voice.speak(com.orynex.bolosaathi.core.L.greet(prefs.lang))
    }

    override fun onCleared() {
        voice.release()
        super.onCleared()
    }
}
