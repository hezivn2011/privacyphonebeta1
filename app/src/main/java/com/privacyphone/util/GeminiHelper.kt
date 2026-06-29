package com.privacyphone.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {

    private const val API_KEY = "AIzaSyARXOkOPhttnds8_6UY_xzW6T1mxbpWcKM"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun evaluateUsage(
        totalUsageMinutes: Long,
        topApps: List<Pair<String, Long>>,
        sensitiveCount: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val appsText = topApps.take(5).joinToString(", ") { (name, mins) -> "$name (${mins}m)" }
            val prompt = """
                Bạn là trợ lý sức khỏe kỹ thuật số. Phân tích dữ liệu sử dụng điện thoại sau đây và đưa ra đánh giá ngắn gọn (2-3 câu) bằng tiếng Việt.
                
                - Tổng thời gian sử dụng hôm nay: ${totalUsageMinutes} phút
                - Ứng dụng dùng nhiều: $appsText
                - Nội dung nhạy cảm phát hiện: $sensitiveCount
                
                Đưa ra lời khuyên thực tế, thân thiện và mang tính xây dựng.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 200)
                })
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL?key=$API_KEY")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext "Không thể lấy đánh giá."
                val json = JSONObject(body)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                text.trim()
            } else {
                "Không thể kết nối với AI. Vui lòng thử lại."
            }
        } catch (e: Exception) {
            "Lỗi kết nối: ${e.message?.take(50)}"
        }
    }

    suspend fun analyzeMediaSafety(imageBase64: String): Pair<Boolean, Float> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Phân tích hình ảnh này và xác định xem nó có chứa nội dung nhạy cảm, không phù hợp, 
                bạo lực, hoặc người lớn không. Trả lời chỉ với JSON: {"sensitive": true/false, "score": 0.0-1.0}
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", imageBase64)
                                })
                            })
                        })
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL?key=$API_KEY")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext Pair(false, 0f)
                val json = JSONObject(body)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Parse JSON response
                val cleanJson = text.replace("```json", "").replace("```", "").trim()
                val result = JSONObject(cleanJson)
                val isSensitive = result.getBoolean("sensitive")
                val score = result.getDouble("score").toFloat()
                Pair(isSensitive, score)
            } else {
                Pair(false, 0f)
            }
        } catch (e: Exception) {
            Pair(false, 0f)
        }
    }
}
