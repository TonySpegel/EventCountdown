package com.example.eventcountdown

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.example.eventcountdown.countdownwidget.CountdownWidget
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
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        setContent {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CalendarButton(ctx, 18, glanceId, manager)
                }
                item {
                    CalendarButton(ctx, 9, glanceId, manager)
                }
            }
        }
    }

    @Composable
    private fun CalendarButton(
        ctx: Context,
        calendarId: Int,
        glanceId: GlanceId,
        glanceManager: GlanceAppWidgetManager
    ) {
        TextButton(onClick = {
            updateWidget(ctx, calendarId, glanceId, glanceManager)
        }) {
            Text(text = "$calendarId ${stringResource(R.string.add_to_homescreen)}")
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

