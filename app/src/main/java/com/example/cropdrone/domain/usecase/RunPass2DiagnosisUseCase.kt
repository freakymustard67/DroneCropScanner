package com.example.cropdrone.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.cropdrone.core.Constants
import com.example.cropdrone.data.remote.GeminiApiService
import com.example.cropdrone.data.remote.dto.CropDiagnosisResult
import com.example.cropdrone.data.remote.dto.GeminiContent
import com.example.cropdrone.data.remote.dto.GeminiGenerationConfig
import com.example.cropdrone.data.remote.dto.GeminiInlineData
import com.example.cropdrone.data.remote.dto.GeminiPart
import com.example.cropdrone.data.remote.dto.GeminiRequest
import com.example.cropdrone.domain.model.Anomaly
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Pass 2 — Cloud Gemini 2.5 Flash diagnosis for frames Pass 1 flagged.
 *
 * Caller responsibility: this only runs when the phone has internet (not connected
 * to drone Wi-Fi). The CloudAnalysisWorker queues pending anomalies and runs this
 * use case for each.
 *
 * Prompt:
 *   "Analyze this crop image. Identify any disease, pest, or water stress.
 *    Return JSON with keys: 'disease_name', 'severity_1_to_5', 'confidence',
 *    'treatment_recommendation'."
 */
class RunPass2DiagnosisUseCase @Inject constructor(
    private val api: GeminiApiService,
    @Named("geminiApiKey") private val apiKey: String,
    private val json: Json,
) {

    suspend operator fun invoke(anomaly: Anomaly): Result<CropDiagnosisResult> {
        val imageBytes = readImageBytes(anomaly.imagePath)
            ?: return Result.failure(IllegalArgumentException("Cannot read image: ${anomaly.imagePath}"))
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val prompt = """
            You are an expert agronomist AI assistant. Analyze this crop image
            and identify any disease, pest infestation, or water/nutrient stress
            visible in the frame.

            Return your analysis as strict JSON with these keys:
              - "disease_name": string — common name of the disease/pest/condition
              - "severity_1_to_5": integer — 1 (minor) to 5 (severe)
              - "confidence": number — your confidence in the diagnosis, 0.0 to 1.0
              - "treatment_recommendation": string — actionable treatment in 2-3 sentences

            If the crop appears healthy, set disease_name to "Healthy" and severity to 1.
            Only output the JSON object — no markdown, no extra commentary.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image)),
                        GeminiPart(text = prompt),
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.4, maxOutputTokens = 1024,
                responseMimeType = "application/json"),
        )

        return try {
            val response = api.generateContent(request, apiKey)
            val responseText = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: return Result.failure(IllegalStateException("Empty response from Gemini"))
            val result = json.decodeFromString<CropDiagnosisResult>(responseText)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readImageBytes(path: String): ByteArray? = try {
        // path may be a content URI or filesystem path
        val bitmap = BitmapFactory.decodeFile(path)
            ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    } catch (_: Exception) { null }
}
