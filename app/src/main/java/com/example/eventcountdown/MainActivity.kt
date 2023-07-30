package com.example.eventcountdown

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.eventcountdown.ui.theme.EventCountdownTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventCountdownTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalendarList(readAvailableCalendars())
                }
            }


            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
            ) {

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CALENDAR),
                    READ_CALENDAR_PERMISSION_CODE
                )
            }
        }
    }

    private fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("dd.MM.yyyy")
        return format.format(date)
    }

    private fun readCalendarEvents(calendarId: Long): List<CalendarEvent> {
        val contentResolver: ContentResolver = contentResolver
        val uri: Uri = CalendarContract.Events.CONTENT_URI

        val projection: Array<String> = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
        )

        val selection =
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs: Array<String> = arrayOf(
            calendarId.toString(),
            getStartOfLast14DaysInMillis().toString()
        )


        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val cursor: Cursor? =
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

        val eventsList = mutableListOf<CalendarEvent>()

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumnIndex: Int = it.getColumnIndex(CalendarContract.Events._ID)
                val titleColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startTimeColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endTimeColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.DTEND)


                while (!it.isAfterLast) {
                    val eventId: Long = if (idColumnIndex != -1) it.getLong(idColumnIndex) else -1
                    val eventTitle: String? =
                        if (titleColumnIndex != -1 && !it.isNull(titleColumnIndex)) it.getString(
                            titleColumnIndex
                        ) else null
                    val startTime: Long =
                        if (startTimeColumnIndex != -1) it.getLong(startTimeColumnIndex) else -1
                    val endTime: Long =
                        if (endTimeColumnIndex != -1) it.getLong(endTimeColumnIndex) else -1

                    val calendarEvent = CalendarEvent(eventId, eventTitle ?: "", startTime, endTime)
                    eventsList.add(calendarEvent)

                    it.moveToNext()
                }
            }

        }
        return eventsList
    }

    data class CalendarData(val calendarId: Long, val accountName: String, val displayName: String)

    data class CalendarEvent(
        val eventId: Long,
        val eventTitle: String,
        val startTime: Long,
        val endTime: Long
    )

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalendarItem(calendar: CalendarData) {
        var showDialog by remember { mutableStateOf(false) }

        fun closeDialog() {
            showDialog = false
        }

        ListItem(
            headlineText = { Text(text = calendar.displayName) },
            trailingContent = {
                IconButton(
                    onClick = {
                        Log.d("lock", "$calendar.displayName $calendar.calendarId")
                        showDialog = true
                    },
                    content = {
                        Icon(
                            Icons.Outlined.EventNote,
                            contentDescription = "Localized description"
                        )
                    }
                )
            }
        )

        if (showDialog) {
            val events = readCalendarEvents(calendar.calendarId)
            EventDialog(showDialog, calendar.displayName, events, onDismiss = { closeDialog() })
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalendarList(
        calendars: List<CalendarData>
    ) {
        Column {
            calendars.forEach { calendar ->
                CalendarItem(calendar)
            }
        }
    }

    private fun getRemainingDays(startTime: Long): Int {
        val currentTime = System.currentTimeMillis()
        val remainingTimeInMillis = startTime - currentTime
        val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingTimeInMillis)
        return remainingDays.toInt()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventItem(event: CalendarEvent) {
        val start = convertLongToTime(event.startTime)
        val title = event.eventTitle
        val remainingDays = getRemainingDays(event.startTime)

        ListItem(
            headlineText = { Text(text = title) },
            supportingText = { Text(text = start) },
            trailingContent = { Text(text = "$remainingDays Tage") }
        )
    }

    private fun getStartOfLast14DaysInMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -14)
        return calendar.timeInMillis
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
                            TextButton(onClick = onDismiss) {
                                Text(text = "abbrechen")
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
