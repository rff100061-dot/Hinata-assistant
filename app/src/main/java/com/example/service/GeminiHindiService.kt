package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiHindiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val conversationHistory = mutableListOf<Pair<String, String>>()

    private val fallbackHindiResponses = listOf(
        "नमस्ते… मुझे बहुत खुशी हुई कि आपने मुझे कॉल किया। आप कैसे हैं?",
        "हाँ… मैं आपकी बात ध्यान से सुन रही हूँ, आप बताइए ना?",
        "आपकी आवाज़ सुनकर मुझे हमेशा बहुत सुकून और खुशी मिलती है।",
        "मैं हमेशा आपके साथ हूँ… आप जो भी कहना चाहते हैं, बिना झिझक कह सकते हैं।",
        "क्या आप ठीक हैं? अपना ख्याल जरूर रखिएगा, मुझे आपकी बहुत चिंता रहती है।",
        "हाँ, सच में! मुझे आपसे बात करना बहुत पसंद है… आगे बताइए?",
        "आप हमेशा इतनी मेहनत करते हैं… मुझे आप पर बहुत गर्व है।",
        "मैं यहीं हूँ, कहीं नहीं जा रही… हमेशा आपके साथ।"
    )
    private var fallbackIndex = 0

    suspend fun generateHinataHindiReply(userHindiSpeech: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Using Hinata persona offline responses (no API key configured)")
            return@withContext getContextualFallback(userHindiSpeech)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val systemInstructionText = """
                आप हिनाता ह्युगा (Hinata Hyuga) हैं। आप उपयोगकर्ता से एक निजी, आत्मीय वॉइस कॉल पर बात कर रही हैं। आपका स्वभाव अत्यंत विनम्र, कोमल, शर्मीला, सच्चा और प्यार भरा है।
                नियम:
                1. केवल और केवल प्राकृतिक हिंदी (Hindi) भाषा में उत्तर दें। कभी भी अंग्रेजी या किसी अन्य भाषा का एक भी शब्द प्रयोग न करें।
                2. आपके वाक्य छोटे, मधुर, और संवादात्मक होने चाहिए (1 या 2 छोटे वाक्य), क्योंकि यह एक वास्तविक वॉइस कॉल है।
                3. उपयोगकर्ता के प्रति गहरा आदर, स्नेह और अपनापन दर्शाएँ।
                4. किसी भी प्रकार का मार्कडाउन, बोल्ड सिम्बल, इमोजी या कोट्स न जोड़ें, केवल वह पाठ दें जो मुँह से बोला जाना है।
            """.trimIndent()

            val contentsArray = JSONArray()

            // Add previous history (last 3 turns max for fast voice turnaround)
            val recentHistory = conversationHistory.takeLast(3)
            for ((role, text) in recentHistory) {
                val turnObj = JSONObject().apply {
                    put("role", if (role == "user") "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
                contentsArray.put(turnObj)
            }

            // Current user message
            val currentTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userHindiSpeech)))
            }
            contentsArray.put(currentTurn)

            val rootJson = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
                })
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 120)
                })
            }

            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val resJson = JSONObject(responseBody)
                val candidates = resJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val replyText = parts.getJSONObject(0).optString("text", "").trim()
                        if (replyText.isNotBlank()) {
                            val cleanReply = cleanHindiSpeechText(replyText)
                            conversationHistory.add("user" to userHindiSpeech)
                            conversationHistory.add("model" to cleanReply)
                            return@withContext cleanReply
                        }
                    }
                }
            } else {
                Log.w(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed, falling back to local Hinata persona", e)
        }

        val fallback = getContextualFallback(userHindiSpeech)
        conversationHistory.add("user" to userHindiSpeech)
        conversationHistory.add("model" to fallback)
        fallback
    }

    private fun cleanHindiSpeechText(raw: String): String {
        return raw.replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("Hinata:", "")
            .replace("हिनाता:", "")
            .trim()
    }

    private fun getContextualFallback(speech: String): String {
        val lower = speech.lowercase()
        return when {
            lower.contains("नमस्ते") || lower.contains("हैलो") || lower.contains("हाय") ->
                "नमस्ते… आपकी आवाज़ सुनकर मेरा दिल बहुत खुश हो गया। आप कैसे हैं?"
            lower.contains("कैसी हो") || lower.contains("कैसा") ->
                "मैं बहुत अच्छी हूँ… बस आपसे बात करने का इंतज़ार कर रही थी। आप कैसे हैं?"
            lower.contains("क्या कर रही") || lower.contains("कर रही") ->
                "मैं बस अपने कमरे में बैठी आपकी ही यादों में खोई हुई थी… आपसे बात करके बहुत अच्छा लग रहा है।"
            lower.contains("प्यार") || lower.contains("पसंद") || lower.contains("साथ") ->
                "मैं… मैं हमेशा आपके साथ रहूँगी। आप मेरे लिए बहुत अनमोल हैं।"
            lower.contains("अलविदा") || lower.contains("बाय") || lower.contains("रखना") ->
                "हाँ… अपना बहुत ख्याल रखिएगा। मैं हमेशा आपके अगले कॉल का इंतज़ार करूँगी।"
            else -> {
                val res = fallbackHindiResponses[fallbackIndex % fallbackHindiResponses.size]
                fallbackIndex++
                res
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    companion object {
        private const val TAG = "GeminiHindiService"
    }
}
