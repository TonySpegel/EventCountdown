package com.example.eventcountdown.countdownwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.eventcountdown.helper.CalendarEvent
import com.example.eventcountdown.helper.cLTT
import com.example.eventcountdown.helper.calendarDisplayName
import com.example.eventcountdown.helper.getRemainingDays
import com.example.eventcountdown.helper.readCalendarEvents

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(
                GlanceTheme.colors
            ) {
                WidgetContentView(context)
            }
        }
    }
}

@Composable
private fun WidgetContentView(ctx: Context) {
    val calendarId = 18
    val events = readCalendarEvents(ctx, calendarId)
    val displayName = calendarDisplayName(ctx, calendarId)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$displayName Countdown",
            modifier = GlanceModifier.padding(horizontal = 18.dp),
            style = TextStyle(
                fontSize = 20.sp,
                color = GlanceTheme.colors.onPrimaryContainer
            )
        )
        LazyColumn {
            events.forEach { event ->
                item {
                    WidgetEventItem(event)
                }
            }
        }
    }
}

@Composable
private fun WidgetEventItem(event: CalendarEvent) {
    val (_, eventTitle, startTime, endTime) = event

    val start = cLTT(startTime)
    val remainingDays = getRemainingDays(startTime)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
    ) {
        Column {
            Text(
                text = eventTitle,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
            Text(
                text = start,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
            )
        }

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "$remainingDays Tage",
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
            )
        }
    }
}