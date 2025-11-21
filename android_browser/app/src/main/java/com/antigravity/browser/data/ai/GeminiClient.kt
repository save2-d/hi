package com.antigravity.browser.data.ai

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Service interface would go here
    // suspend fun generateResponse(apiKey: String, prompt: String): String { ... }
}
