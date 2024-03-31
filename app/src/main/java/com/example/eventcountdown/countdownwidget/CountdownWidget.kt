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
import androidx.glance.text.TextAlign
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val displayName =
        calendarDisplayName(ctx, calendarId)
            ?: "${LocalContext.current.getString(R.string.is_loading)}..."
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentTime = LocalDateTime.now().format(formatter)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = displayName,
                modifier = GlanceModifier.padding(end = 18.dp, bottom = 8.dp),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,

                    ),
            )
            Text(
                text = "${LocalContext.current.getString(R.string.updated)} $currentTime",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onTertiaryContainer,
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                ),
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
    val numberOfEvents = events.size

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        events.forEachIndexed { index, event ->
            val isLastItem = index == numberOfEvents - 1

            run {
                item { WidgetEventItem(event, isLastItem, ctx) }
            }
        }
    }
}

@Composable
private fun WidgetEventItem(event: CalendarEvent, lastEvent: Boolean, ctx: Context) {
    val (eventId, eventTitle, startTime, _) = event
    val start = cLTT(startTime)
    val duration = Duration.between(
        Instant.now(),
        Instant.ofEpochMilli(startTime)
    )

    val relativeDuration = getRelativeTimeString(duration).toString()
    val eventModifierPadding =
        if (!lastEvent) {
            GlanceModifier.padding(vertical = 6.dp)
        } else GlanceModifier.padding(top = 6.dp, bottom = 16.dp)

    Box(modifier = eventModifierPadding) {
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