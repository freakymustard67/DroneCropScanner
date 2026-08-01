package com.example.cropdrone.data.remote

import com.example.cropdrone.data.remote.dto.GeminiRequest
import com.example.cropdrone.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service for Google Gemini 2.5 Flash.
 * Endpoint: POST /v1beta/models/gemini-2.5-flash:generateContent
 */
interface GeminiApiService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Body request: GeminiRequest,
        @Query("key") apiKey: String,
    ): GeminiResponse
}
