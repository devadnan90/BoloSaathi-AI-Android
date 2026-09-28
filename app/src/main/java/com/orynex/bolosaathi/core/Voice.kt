package com.orynex.bolosaathi.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Speech in and out. Uses the phone's own recogniser and TTS in hi-IN.
 * Bhojpuri and Maithili are understood by the Hindi recogniser well enough,
 * and Gemini handles the meaning.
 */
class Voice(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private val ttsReady = CompletableDeferred<Boolean>()
    private var recognizer: SpeechRecognizer? = null

    /** Called with the live level (0..1) while listening, for the mic animation. */
    var onLevel: ((Float) -> Unit)? = null
    var onPartial: ((String) -> Unit)? = null

    init {
        main.post {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hi = Locale("hi", "IN")
                    val r = tts?.setLanguage(hi)
                    tts?.setSpeechRate(0.92f)
                    ttsReady.complete(r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED)
                } else ttsReady.complete(false)
            }
        }
    }

    fun available() = SpeechRecognizer.isRecognitionAvailable(context)

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        if (!ttsReady.await()) return
        val clean = text.replace(Regex("[*_#`]"), "")
        suspendCancellableCoroutine { cont ->
            val id = UUID.randomUUID().toString()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
            })
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
            cont.invokeOnCancellation { tts?.stop() }
        }
    }

    fun stopSpeaking() { tts?.stop() }

    /** Listen once. Returns the best transcript, or null if nothing was heard. */
    suspend fun listen(): String? = withContext(Dispatchers.Main) {
        stopSpeaking()
        suspendCancellableCoroutine { cont ->
            recognizer?.destroy()
            val rec = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = rec
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    onLevel?.invoke(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onLevel?.invoke(0f) }
                override fun onError(error: Int) {
                    onLevel?.invoke(0f)
                    if (cont.isActive) cont.resume(null)
                }
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (cont.isActive) cont.resume(list?.firstOrNull()?.takeIf { it.isNotBlank() })
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let { onPartial?.invoke(it) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }
            rec.startListening(intent)
            cont.invokeOnCancellation { main.post { rec.cancel() } }
        }
    }

    fun stopListening() { main.post { recognizer?.stopListening() } }

    fun release() {
        main.post {
            recognizer?.destroy(); recognizer = null
            tts?.shutdown(); tts = null
        }
    }
}

/** Words that mean yes / no in Hindi, Bhojpuri, Maithili, Hinglish. */
object YesNo {
    private val yes = listOf("haan", "ha", "han", "haa", "hanji", "ji", "jee", "yes", "ok", "okay", "theek", "thik", "sahi", "kar do", "kardo", "bhej do", "bhejo", "karo", "ho", "hau",
        "हाँ", "हां", "हा", "जी", "ठीक", "सही", "कर दो", "करो", "भेज दो", "भेजो", "हओ", "हँ", "ओके", "हाँजी", "जरूर", "बिल्कुल", "कर दीजिए", "भेज दीजिए", "करी", "कर दी")
    private val no = listOf("nahi", "nahin", "na", "no", "mat", "ruko", "cancel", "band", "नहीं", "नही", "ना", "मत", "रुको", "रुकिए", "बंद", "कैंसल", "नै", "नइखे")

    /** true = yes, false = no, null = unclear */
    fun parse(s: String?): Boolean? {
        if (s == null) return null
        val t = s.lowercase(Locale.ROOT).trim()
        if (no.any { Regex("(^|\\s)${Regex.escape(it)}(\\s|$|[.!,])").containsMatchIn(t) }) return false
        if (yes.any { Regex("(^|\\s)${Regex.escape(it)}(\\s|$|[.!,])").containsMatchIn(t) }) return true
        return null
    }
}
