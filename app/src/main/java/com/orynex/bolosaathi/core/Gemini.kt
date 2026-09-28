package com.orynex.bolosaathi.core

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiException(msg: String) : Exception(msg)

/** One turn of conversation history. role = "user" or "model". */
data class Turn(val role: String, val text: String)

class Gemini(private val keyProvider: () -> String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Tried in order. If one is busy or gone, the next one answers. */
    private val models = listOf(
        "gemini-flash-latest",
        "gemini-3-flash-preview",
        "gemini-2.5-flash-lite",
        "gemini-3.1-flash-lite",
    )

    /** Remember which model worked last, so the next call starts there. */
    @Volatile
    private var preferred = 0

    fun hasKey() = keyProvider().isNotBlank()

    suspend fun json(
        system: String,
        user: String,
        history: List<Turn> = emptyList(),
        image: Bitmap? = null,
    ): JSONObject = parseJson(call(system, user, history, image, jsonMode = true))

    suspend fun text(
        system: String,
        user: String,
        history: List<Turn> = emptyList(),
        image: Bitmap? = null,
    ): String = call(system, user, history, image, jsonMode = false).trim()

    private suspend fun call(
        system: String,
        user: String,
        history: List<Turn>,
        image: Bitmap?,
        jsonMode: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val key = keyProvider()
        if (key.isBlank()) throw GeminiException("NO_KEY")
        val body = buildBody(system, user, history, image, jsonMode)

        var lastError = "unknown"
        repeat(2) { round ->
            for (i in models.indices) {
                val idx = (preferred + i) % models.size
                val model = models[idx]
                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                    .header("x-goog-api-key", key)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                try {
                    http.newCall(req).execute().use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            preferred = idx
                            return@withContext extractText(raw)
                        }
                        val msg = runCatching { JSONObject(raw).getJSONObject("error").optString("message") }
                            .getOrDefault(raw.take(200))
                        lastError = "HTTP ${resp.code} ($model): $msg"
                        // wrong key or blocked project: other models won't help
                        if (resp.code == 400 || resp.code == 401 || resp.code == 403) throw GeminiException(lastError)
                    }
                } catch (e: GeminiException) {
                    throw e
                } catch (e: Exception) {
                    lastError = "${e.javaClass.simpleName} ($model): ${e.message}"
                }
            }
            if (round == 0) delay(1500)
        }
        throw GeminiException(lastError)
    }

    private fun buildBody(system: String, user: String, history: List<Turn>, image: Bitmap?, jsonMode: Boolean): String {
        val contents = JSONArray()
        history.forEach { t ->
            contents.put(
                JSONObject()
                    .put("role", t.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", t.text)))
            )
        }
        val parts = JSONArray()
        if (image != null) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", image.toBase64Jpeg())
                )
            )
        }
        parts.put(JSONObject().put("text", user))
        contents.put(JSONObject().put("role", "user").put("parts", parts))

        val gen = JSONObject().put("temperature", 0.3)
        if (jsonMode) gen.put("responseMimeType", "application/json")

        return JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", gen)
            .toString()
    }

    private fun extractText(raw: String): String {
        val o = JSONObject(raw)
        val cands = o.optJSONArray("candidates") ?: throw GeminiException("Empty response")
        val p = cands.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            ?: throw GeminiException("Blocked or empty")
        return buildString {
            for (i in 0 until p.length()) {
                val part = p.getJSONObject(i)
                if (!part.optBoolean("thought", false)) append(part.optString("text"))
            }
        }
    }

    private fun parseJson(text: String): JSONObject {
        val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { JSONObject(t) }.getOrElse {
            val s = t.indexOf('{')
            val e = t.lastIndexOf('}')
            if (s >= 0 && e > s) JSONObject(t.substring(s, e + 1)) else throw GeminiException("Bad JSON")
        }
    }
}

private fun Bitmap.toBase64Jpeg(): String {
    val maxSide = 1280
    val scale = minOf(1f, maxSide.toFloat() / maxOf(width, height))
    val bmp = if (scale < 1f) Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true) else this
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
