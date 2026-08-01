package com.example.cropdrone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ---- Gemini 2.5 Flash REST API request/response DTOs ----
// Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=YOUR_KEY

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user",
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String,    // base64-encoded image bytes
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.4,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 1024,
    val responseMimeType: String = "application/json",
    val responseSchema: JsonElement? = null,  // optional structured schema
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

@Serializable
data class GeminiUsageMetadata(
    @SerialName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerialName("candidatesTokenCount") val candidatesTokenCount: Int = 0,
    @SerialName("totalTokenCount") val totalTokenCount: Int = 0,
)

// ---- Parsed Pass 2 diagnosis result ----
@Serializable
data class CropDiagnosisResult(
    @SerialName("disease_name") val diseaseName: String,
    @SerialName("severity_1_to_5") val severity: Int,
    val confidence: Double,
    @SerialName("treatment_recommendation") val treatmentRecommendation: String,
)
