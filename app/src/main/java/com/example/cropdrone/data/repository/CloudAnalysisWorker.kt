package com.example.cropdrone.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.example.cropdrone.core.Constants
import com.example.cropdrone.domain.repository.AnomalyRepository
import com.example.cropdrone.domain.usecase.RunPass2DiagnosisUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically (every 15 min, when internet is available)
 * drains the pending-cloud-analysis queue and runs Pass 2 on each anomaly.
 *
 * Critical: the NetworkType.UNMETERED constraint means this only fires when the
 * phone has a real internet connection (NOT when connected to the drone's Wi-Fi AP).
 */
@HiltWorker
class CloudAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val anomalyRepo: AnomalyRepository,
    private val pass2: RunPass2DiagnosisUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending = anomalyRepo.getPendingCloudAnalysis()
        if (pending.isEmpty()) return Result.success()

        for (anomaly in pending) {
            val result = pass2.invoke(anomaly)
            if (result.isSuccess) {
                val diagnosis = result.getOrThrow()
                anomalyRepo.updateAnomaly(
                    anomaly.copy(
                        diseaseName = diagnosis.diseaseName,
                        severity = diagnosis.severity,
                        confidence = diagnosis.confidence,
                        treatment = diagnosis.treatmentRecommendation,
                        analyzedAt = System.currentTimeMillis(),
                    )
                )
            } else {
                // Log and continue; the worker will retry on next cycle
            }
        }
        return Result.success()
    }

    companion object {
        fun buildRequest() = PeriodicWorkRequestBuilder<CloudAnalysisWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
        ).build()
    }
}
