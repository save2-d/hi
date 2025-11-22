package com.antigravity.browser.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {
    @POST("v1beta/models/gemini-pro:generateContent") // Dynamic model in URL usually, but Retrofit needs static or dynamic path
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @retrofit2.http.Url url: String, // To support switching models dynamically
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

data class Part(
    val text: String? = null
)

data class GenerationConfig(
    val temperature: Float? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)
