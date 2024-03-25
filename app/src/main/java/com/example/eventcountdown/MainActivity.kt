package com.example.eventcountdown

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.eventcountdown.countdownwidget.CountdownWidget
import com.example.eventcountdown.helper.CalendarData
import com.example.eventcountdown.helper.CalendarEvent
import com.example.eventcountdown.helper.convertLongToTime
import com.example.eventcountdown.helper.getRemainingDays
import com.example.eventcountdown.helper.readCalendarEvents
import com.example.eventcountdown.ui.theme.EventCountdownTheme
import com.example.eventcountdown.ui.views.CalendarDenied
import com.example.eventcountdown.ui.views.CalendarNotGranted
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)
            val widget = CountdownWidget()
            val glanceIds = manager.getGlanceIds(widget.javaClass)
            glanceIds.forEach { glanceId -> widget.update(this@MainActivity, glanceId) }
        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    recreate() // Restart the activity to show the calendar list
                } else {
                    recreate() // Restart the activity to show notice
                }
            }

        setContent {
            val calendarPermissionState by remember { mutableStateOf(getCalendarPermissionState()) }
            Log.d("permission", calendarPermissionState.toString())

            EventCountdownTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (calendarPermissionState) {
                        // No permission was granted/denied yet
                        CalendarPermissionState.NOT_GRANTED -> CalendarNotGranted(
                            requestPermissionLauncher
                        )
                        CalendarPermissionState.GRANTED -> CalendarGranted() // Permission granted let's go
                        CalendarPermissionState.DENIED -> CalendarDenied()
                    }
                }
            }
        }
    }

    @Composable
    fun CalendarGranted() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CalendarList(readAvailableCalendars())
        }
    }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    enum class CalendarPermissionState {
        NOT_GRANTED,
        GRANTED,
        DENIED
    }

    private fun getCalendarPermissionState(): CalendarPermissionState {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return CalendarPermissionState.GRANTED
        }

        // Check if the user has previously denied the permission
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.READ_CALENDAR
            )
        ) {
            return CalendarPermissionState.DENIED
        }

        return CalendarPermissionState.NOT_GRANTED
    }

    @SuppressLint("Range")
    private fun readAvailableCalendars(): List<CalendarData> {
        val calendarsList = mutableListOf<CalendarData>()
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI

        val projection: Array<String> = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        val selection: String? = null
        val selectionArgs: Array<String>? = null

        val sortOrder = "${CalendarContract.Calendars._ID} ASC"
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val calendarId: Long =
                        it.getLong(it.getColumnIndex(CalendarContract.Calendars._ID))
                    val accountName: String =
                        it.getString(it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME))
                    val displayName: String =
                        it.getString(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))

                    val calendarData = CalendarData(calendarId, accountName, displayName)
                    calendarsList.add(calendarData)
                } while (it.moveToNext())
            }
        }

        return calendarsList
    }

    companion object {
        private const val READ_CALENDAR_PERMISSION_CODE = 1
    }

    @Composable
    fun CalendarItem(calendar: CalendarData) {
        var showDialog by remember { mutableStateOf(false) }

        fun closeDialog() {
            showDialog = false
        }

        ListItem(
            headlineContent = { Text(text = calendar.displayName) },
            trailingContent = {
                IconButton(
                    onClick = {
                        Log.d("lock", "${calendar.displayName} ${calendar.calendarId}")
                        showDialog = true

                        pinWidget(this@MainActivity)
                    },
                    content = {
                        Icon(
                            Icons.AutoMirrored.Outlined.EventNote,
                            contentDescription = "Localized description"
                        )
                    }
                )
            }
        )

        if (showDialog) {
            val events = readCalendarEvents(
                LocalContext.current,
                calendar.calendarId.toInt()
            )

            EventDialog(
                showDialog,
                "${calendar.displayName} - ${calendar.calendarId}",
                events,
                onDismiss = { closeDialog() })
        }
    }

    // WIP
    private fun pinWidget(ctx: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(ctx)
        val myProvider = ComponentName(ctx, CountdownWidget::class.java)
        val successIntent = Intent(ctx, MainActivity::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val successCallback = PendingIntent.getBroadcast(
                ctx,
                0,
                successIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
        }
    }

    @Composable
    fun CalendarList(
        calendars: List<CalendarData>
    ) {
        Column {
            LazyColumn {
                calendars.forEach { calendar ->
                    item {
                        CalendarItem(calendar)
                    }
                }
            }
        }
    }

    @Composable
    fun EventItem(event: CalendarEvent) {
        val start = convertLongToTime(event.startTime)
        val title = event.eventTitle
        val remainingDays = getRemainingDays(event.startTime)

        ListItem(
            headlineContent = { Text(text = title) },
            supportingContent = { Text(text = start) },
            trailingContent = { Text(text = "$remainingDays Tage") }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun EventDialog(
        showDialog: Boolean,
        calendarTitle: String,
        events: List<CalendarEvent>,
        onDismiss: () -> Unit
    ) {
        if (showDialog) {
            val vertScrollState = rememberScrollState()

            Dialog(onDismissRequest = onDismiss) {
                Card(
                    // or Surface
                    modifier = Modifier
                        .requiredWidth(LocalConfiguration.current.screenWidthDp.dp * 0.96f)
                        .padding(4.dp),

                    ) {
                    Column {
                        TopAppBar(
                            title = { Text(text = calendarTitle) },
                        )
                        FlowRow(
                            modifier = Modifier
                                .verticalScroll(vertScrollState)
                                .weight(1f)
                        ) {
                            events.forEach { event -> EventItem(event) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {}) {
                                Text(text = stringResource(R.string.add_to_homescreen))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text(text = stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text(text = "ok")
                            }
                        }
                    }
                }
            }
        }
    }
}
