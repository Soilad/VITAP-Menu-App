package com.soilad.vitapmenu

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

const val WIDGET_UPDATE_WORK = "widget_update"

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        CurrentMenuWidget().updateAll(applicationContext)
        // APPEND_OR_REPLACE, not REPLACE: REPLACE would cancel this worker while it's still running
        scheduleNextWidgetUpdate(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}

// Wakes the widget up when the current meal ends, or just after midnight once the day's meals are over
suspend fun scheduleNextWidgetUpdate(context: Context, policy: ExistingWorkPolicy) {
    val now = LocalDateTime.now()
    val nextEnd = getMenu(context, YearMonth.from(now))
        ?.getOrNull((now.dayOfMonth - 1) % 14)
        ?.map { LocalTime.parse(it.end) }
        ?.filter { it > now.toLocalTime() }
        ?.minOrNull()
    val target = nextEnd?.let { now.with(it) } ?: now.toLocalDate().plusDays(1).atTime(0, 1)

    WorkManager.getInstance(context).enqueueUniqueWork(
        WIDGET_UPDATE_WORK,
        policy,
        OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInitialDelay(Duration.between(now, target).plusSeconds(5))
            .build()
    )
}
