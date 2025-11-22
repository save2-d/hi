package com.antigravity.browser.data.ai

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val TAG = "GeminiClient"
    
    // Model identifiers
    const val MODEL_FLASH_THINKING = "gemini-2.0-flash-thinking-exp"
    const val MODEL_FLASH = "gemini-2.0-flash-exp"
    const val MODEL_PRO = "gemini-2.0-pro-exp"
    
    // Default model
    var currentModel = MODEL_FLASH_THINKING

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GeminiService = retrofit.create(GeminiService::class.java)

    /**
     * Generate text response from prompt
     */
    suspend fun generateText(
        apiKey: String,
        prompt: String,
        enableThinking: Boolean = true,
        enableGoogleSearch: Boolean = true,
        model: String = currentModel
    ): Result<String> {
        return try {
            val request = buildTextRequest(prompt, enableThinking, enableGoogleSearch)
            val response = service.generateContent(model, apiKey, request)
            
            if (response.isSuccessful && response.body() != null) {
                val text = response.body()!!.candidates.firstOrNull()?.content?.parts
                    ?.firstOrNull()?.text ?: ""
                Result.success(text)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API Error: $errorMsg")
                Result.failure(Exception("API Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            Result.failure(e)
        }
    }

    /**
     * Generate response with vision (image analysis)
     */
    suspend fun generateWithVision(
        apiKey: String,
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        model: String = currentModel
    ): Result<String> {
        return try {
            val request = buildVisionRequest(prompt, imageBase64, mimeType)
            val response = service.generateContent(model, apiKey, request)
            
            if (response.isSuccessful && response.body() != null) {
                val text = response.body()!!.candidates.firstOrNull()?.content?.parts
                    ?.firstOrNull()?.text ?: ""
                Result.success(text)
            } else {
                Result.failure(Exception("Vision API Error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate with function calling
     */
    suspend fun generateWithFunctions(
        apiKey: String,
        prompt: String,
        functions: List<FunctionDeclaration>,
        model: String = currentModel
    ): Result<GeminiResponse> {
        return try {
            val request = buildFunctionRequest(prompt, functions)
            val response = service.generateContent(model, apiKey, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Function calling error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTextRequest(
        prompt: String,
        enableThinking: Boolean,
        enableGoogleSearch: Boolean
    ): GeminiRequest {
        val tools = mutableListOf<Tool>()
        
        if (enableGoogleSearch) {
            tools.add(
                Tool(
                    googleSearch = GoogleSearch(
                        dynamicRetrievalConfig = DynamicRetrievalConfig(
                            mode = "MODE_DYNAMIC",
                            dynamicThreshold = 0.3f
                        )
                    )
                )
            )
        }
        
        return GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt)),
                    role = "user"
                )
            ),
            generationConfig = GenerationConfig(
                temperature = if (enableThinking) 1.0f else 0.7f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 8192
            ),
            tools = tools.ifEmpty { null }
        )
    }

    private fun buildVisionRequest(
        prompt: String,
        imageBase64: String,
        mimeType: String
    ): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = imageBase64))
                    ),
                    role = "user"
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.4f,
                maxOutputTokens = 8192
            )
        )
    }

    private fun buildFunctionRequest(
        prompt: String,
        functions: List<FunctionDeclaration>
    ): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt)),
                    role = "user"
                )
            ),
            tools = listOf(Tool(functionDeclarations = functions)),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                maxOutputTokens = 4096
            )
        )
    }
    
    /**
     * Switch to a different model
     */
    fun switchModel(modelName: String) {
        currentModel = when (modelName.lowercase()) {
            "flash", "2.5 flash", "gemini 2.5 flash" -> MODEL_FLASH_THINKING // User requested "thinking" enabled default
            "pro", "2.5 pro", "gemini 2.5 pro" -> MODEL_PRO
            "flash-thinking" -> MODEL_FLASH_THINKING
            else -> MODEL_FLASH_THINKING
        }
        Log.d(TAG, "Switched to model: $currentModel (mapped from $modelName)")
    }
}

