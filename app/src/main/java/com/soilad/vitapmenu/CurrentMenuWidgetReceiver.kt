package com.soilad.vitapmenu

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soilad.vitapmenu.ui.theme.ApplicationTheme
import java.time.LocalDateTime
import java.time.YearMonth

class CurrentMenuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CurrentMenuWidget()
}

class CurrentMenuWidget : GlanceAppWidget() {
    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val menu = getMenu(context, YearMonth.now())
        val localTime = LocalDateTime.now()
        val day = (localTime.dayOfMonth - 1) % 14
        val timeEntry = menu?.get(day)?.filter {
            it.end > localTime.toLocalTime().toString()
            // true
        }?.getOrNull(0)
        provideContent {
            GlanceTheme() {
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(5.dp)
                ) {
                    if (timeEntry == null) {
                        Button(
                            "Nuke VIT-AP",
                            onClick = {},
                        )
                    } else {
                        Text(
                            timeEntry?.title!!,
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GlanceTheme.colors.secondary
                            )
                        )
                        for (foodEntry in timeEntry?.foodEntries!!) {
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