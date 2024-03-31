package com.example.eventcountdown

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.example.eventcountdown.countdownwidget.CountdownWidget
import com.example.eventcountdown.helper.CalendarData
import com.example.eventcountdown.helper.readAvailableCalendars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfig : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val ctx = this@WidgetConfig
        val manager = GlanceAppWidgetManager(ctx)
        val glanceId = manager.getGlanceIdBy(appWidgetId)
        val calendars = readAvailableCalendars(ctx)

        setContent {
            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                CalendarIntro()
                CalendarList(ctx, manager, glanceId, calendars)
            }
        }
    }

    @Composable
    fun CalendarIntro() {
        Text(
            text = LocalContext.current.getString(R.string.choose_calendar),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = LocalContext.current.getString(R.string.choose_calendar_long),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    @Composable
    fun CalendarList(
        ctx: Context,
        manager: GlanceAppWidgetManager,
        glanceId: GlanceId,
        calendars: List<CalendarData>
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
        ) {
            calendars.forEach { calendar ->
                item {
                    CalendarItem(ctx, calendar, glanceId, manager)
                }
            }
        }
    }

    @Composable
    private fun CalendarItem(
        ctx: Context,
        calendarData: CalendarData,
        glanceId: GlanceId,
        glanceManager: GlanceAppWidgetManager
    ) {
        val calendarId = calendarData.calendarId.toInt()

        Box(modifier = Modifier.padding(vertical = 6.dp)) {
            ListItem(
                headlineContent = {
                    Text(
                        text = calendarData.displayName,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                trailingContent = {
                    IconButton(
                        content = {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = "Localized description",
                            )
                        },
                        onClick = { updateWidget(ctx, calendarId, glanceId, glanceManager) }
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
    }

    private fun updateWidget(
        ctx: Context,
        calendarId: Int,
        glanceId: GlanceId,
        glanceManager: GlanceAppWidgetManager
    ) {
        lifecycleScope.launch(Dispatchers.Default) {
            val resultValue = Intent()
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

            val widget = CountdownWidget()
            val glanceIds = glanceManager.getGlanceIds(widget.javaClass)

            updateAppWidgetState(ctx, glanceId) { prefs ->
                prefs[intPreferencesKey("calendarId")] = calendarId
            }

            glanceIds.forEach { glanceId ->
                widget.update(ctx, glanceId)
            }

            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

