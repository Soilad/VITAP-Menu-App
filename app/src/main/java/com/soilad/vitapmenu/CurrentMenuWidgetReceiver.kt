package com.soilad.vitapmenu

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth

class CurrentMenuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CurrentMenuWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleNextWidgetUpdate(context, ExistingWorkPolicy.KEEP)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK)
    }
}

class CurrentMenuWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val menu = getMenu(context, YearMonth.now())
        val localTime = LocalDateTime.now()
        val day = (localTime.dayOfMonth - 1) % 14
        val timeEntry = menu?.getOrNull(day)?.filter {
            it.end > localTime.toLocalTime().toString()
        }?.getOrNull(0)
        provideContent {
            GlanceTheme {
                LazyColumn(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(5.dp)
                ) {
                    if (timeEntry == null) {
                        item {
                            Button(
                                "Nuke VIT-AP",
                                onClick = {},
                            )
                        }
                    } else {
                        // Each child gets its own item: an item's content is stacked in a Box
                        item {
                            Text(
                                timeEntry.title,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    color = GlanceTheme.colors.secondary
                                )
                            )
                        }
                        items(timeEntry.foodEntries) { foodEntry ->
                            Text(
                                "${foodIcons[foodEntry.type]} ${foodEntry.name}",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    color = GlanceTheme.colors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
