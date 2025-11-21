package com.antigravity.browser.data.ai

import com.google.gson.annotations.SerializedName

// Request models
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<Content>,
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null,
    @SerializedName("safetySettings")
    val safetySettings: List<SafetySetting>? = null,
    @SerializedName("tools")
    val tools: List<Tool>? = null,
    @SerializedName("systemInstruction")
    val systemInstruction: Content? = null
)

data class Content(
    @SerializedName("parts")
    val parts: List<Part>,
    @SerializedName("role")
    val role: String? = null
)

data class Part(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("inlineData")
    val inlineData: InlineData? = null,
    @SerializedName("functionCall")
    val functionCall: FunctionCall? = null,
    @SerializedName("functionResponse")
    val functionResponse: FunctionResponse? = null
)

data class InlineData(
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("data")
    val data: String // Base64 encoded
)

data class GenerationConfig(
    @SerializedName("temperature")
    val temperature: Float? = 1.0f,
    @SerializedName("topP")
    val topP: Float? = 0.95f,
    @SerializedName("topK")
    val topK: Int? = 40,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int? = 8192,
    @SerializedName("responseMimeType")
    val responseMimeType: String? = "text/plain",
    @SerializedName("responseSchema")
    val responseSchema: Any? = null
)

data class SafetySetting(
    @SerializedName("category")
    val category: String,
    @SerializedName("threshold")
    val threshold: String
)

data class Tool(
    @SerializedName("functionDeclarations")
    val functionDeclarations: List<FunctionDeclaration>? = null,
    @SerializedName("googleSearch")
    val googleSearch: GoogleSearch? = null
)

data class GoogleSearch(
    @SerializedName("dynamicRetrievalConfig")
    val dynamicRetrievalConfig: DynamicRetrievalConfig? = null
)

data class DynamicRetrievalConfig(
    @SerializedName("mode")
    val mode: String = "MODE_DYNAMIC", // MODE_DYNAMIC, MODE_UNSPECIFIED
    @SerializedName("dynamicThreshold")
    val dynamicThreshold: Float? = 0.3f
)

data class FunctionDeclaration(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("parameters")
    val parameters: FunctionParameters
)

data class FunctionParameters(
    @SerializedName("type")
    val type: String = "object",
    @SerializedName("properties")
    val properties: Map<String, PropertySchema>,
    @SerializedName("required")
    val required: List<String>? = null
)

data class PropertySchema(
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("enum")
    val enum: List<String>? = null
)

data class FunctionCall(
    @SerializedName("name")
    val name: String,
    @SerializedName("args")
    val args: Map<String, Any>
)

data class FunctionResponse(
    @SerializedName("name")
    val name: String,
    @SerializedName("response")
    val response: Map<String, Any>
)

// Response models
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>,
    @SerializedName("promptFeedback")
    val promptFeedback: PromptFeedback? = null,
    @SerializedName("usageMetadata")
    val usageMetadata: UsageMetadata? = null
)

data class Candidate(
    @SerializedName("content")
    val content: Content,
    @SerializedName("finishReason")
    val finishReason: String? = null,
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null,
    @SerializedName("citationMetadata")
    val citationMetadata: CitationMetadata? = null,
    @SerializedName("groundingMetadata")
    val groundingMetadata: GroundingMetadata? = null
)

data class PromptFeedback(
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>
)

data class SafetyRating(
    @SerializedName("category")
    val category: String,
    @SerializedName("probability")
    val probability: String
)

data class UsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int,
    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int,
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int
)

data class CitationMetadata(
    @SerializedName("citations")
    val citations: List<Citation>
)

data class Citation(
    @SerializedName("startIndex")
    val startIndex: Int,
    @SerializedName("endIndex")
    val endIndex: Int,
    @SerializedName("uri")
    val uri: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("license")
    val license: String? = null,
    @SerializedName("publicationDate")
    val publicationDate: String? = null
)

data class GroundingMetadata(
    @SerializedName("groundingChunks")
    val groundingChunks: List<GroundingChunk>? = null,
    @SerializedName("groundingSupports")
    val groundingSupports: List<GroundingSupport>? = null,
    @SerializedName("webSearchQueries")
    val webSearchQueries: List<String>? = null,
    @SerializedName("searchEntryPoint")
    val searchEntryPoint: SearchEntryPoint? = null
)

data class GroundingChunk(
    @SerializedName("web")
    val web: WebChunk? = null
)

data class WebChunk(
    @SerializedName("uri")
    val uri: String,
    @SerializedName("title")
    val title: String? = null
)

data class GroundingSupport(
    @SerializedName("groundingChunkIndices")
    val groundingChunkIndices: List<Int>,
    @SerializedName("confidenceScores")
    val confidenceScores: List<Float>,
    @SerializedName("segment")
    val segment: Segment
)

data class Segment(
    @SerializedName("startIndex")
    val startIndex: Int,
    @SerializedName("endIndex")
    val endIndex: Int,
    @SerializedName("text")
    val text: String
)

data class SearchEntryPoint(
    @SerializedName("renderedContent")
    val renderedContent: String
)
