package com.antigravity.browser.data.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiService {
    
    /**
     * Generate content using Gemini models
     * Models: gemini-2.0-flash-thinking-exp, gemini-2.0-flash-exp, gemini-2.0-pro-exp
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
    
    /**
     * Stream generate content (for future streaming support)
     */
    @POST("v1beta/models/{model}:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
    
    /**
     * List available models
     */
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): Response<ModelsListResponse>
}

data class ModelsListResponse(
    val models: List<ModelInfo>
)

data class ModelInfo(
    val name: String,
    val displayName: String,
    val description: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>
)
