package com.example.eventcountdown.countdownwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.eventcountdown.R
import com.example.eventcountdown.helper.CalendarEvent
import com.example.eventcountdown.helper.cLTT
import com.example.eventcountdown.helper.calendarDisplayName
import com.example.eventcountdown.helper.getRelativeTimeString
import com.example.eventcountdown.helper.openCalendarEvent
import com.example.eventcountdown.helper.readCalendarEvents
import java.time.Duration
import java.time.Instant

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val calendarId = prefs[intPreferencesKey("calendarId")] ?: 0

            GlanceTheme(
                GlanceTheme.colors
            ) {
                WidgetContentView(context, calendarId)
            }
        }
    }
}

@Composable
private fun WidgetContentView(ctx: Context, calendarId: Int) {
    val events = readCalendarEvents(ctx, calendarId)
    val displayName = calendarDisplayName(ctx, calendarId)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                text = "$displayName · Countdown",
                modifier = GlanceModifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                style = TextStyle(
                    fontSize = 20.sp,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }

        when {
            events.isEmpty() -> WidgetNoEvents()
            else -> WidgetEvents(events, ctx)
        }
    }
}

@Composable
private fun WidgetNoEvents() {
    Column {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = LocalContext.current.getString(R.string.no_events),
                style = TextStyle(
                    fontSize = 18.sp,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }
    }
}

@Composable
private fun WidgetEvents(events: List<CalendarEvent>, ctx: Context) {
    LazyColumn(
        horizontalAlignment = Alignment.Start,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        events.forEach {
            item { WidgetEventItem(it, ctx) }
        }
    }
}

@Composable
private fun WidgetEventItem(event: CalendarEvent, ctx: Context) {
    val (eventId, eventTitle, startTime, _) = event
    val start = cLTT(startTime)
    val duration = Duration.between(
        Instant.now(),
        Instant.ofEpochMilli(startTime)
    )

    val relativeDuration = getRelativeTimeString(duration).toString()

    Box(modifier = GlanceModifier.padding(vertical = 6.dp)) {
        Column(
            modifier = GlanceModifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
                .fillMaxWidth()
                .background(GlanceTheme.colors.tertiaryContainer)
                .cornerRadius(16.dp)
                .clickable { openCalendarEvent(ctx, eventId) }
        ) {
            Row {
                Text(
                    text = eventTitle,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onTertiaryContainer
                    ),
                )
            }
            Row {
                Text(
                    text = "$start · $relativeDuration",
                    style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer)
                )
            }
        }
    }
}