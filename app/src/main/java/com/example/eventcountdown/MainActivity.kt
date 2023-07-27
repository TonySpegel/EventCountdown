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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.eventcountdown.ui.theme.EventCountdownTheme

data class Message(val author: String, val body: String)

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
                    val calendars = readAvailableCalendars()

                    CalendarList(calendars)
                }
            }


            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // readCalendarEvents()
                val calendars = readAvailableCalendars()


            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CALENDAR),
                    READ_CALENDAR_PERMISSION_CODE
                )
            }
        }
    }

    private fun readCalendarEvents() {
        val contentResolver: ContentResolver = contentResolver
        val uri: Uri = CalendarContract.Events.CONTENT_URI

        val projection: Array<String> = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
        )

        val selection: String? = null
        val selectionArgs: Array<String>? = null

        val sortOrder: String = "${CalendarContract.Events.DTSTART} ASC"

        val cursor: Cursor? =
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumnIndex: Int = it.getColumnIndex(CalendarContract.Events._ID)
                val titleColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startTimeColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endTimeColumnIndex: Int = it.getColumnIndex(CalendarContract.Events.DTEND)


                while (!it.isAfterLast) {
                    val eventId: Long = if (idColumnIndex != -1) it.getLong(idColumnIndex) else -1
                    val eventTitle: String =
                        if (titleColumnIndex != -1) it.getString(titleColumnIndex) else ""
                    val startTime: Long =
                        if (startTimeColumnIndex != -1) it.getLong(startTimeColumnIndex) else -1
                    val endTime: Long =
                        if (endTimeColumnIndex != -1) it.getLong(endTimeColumnIndex) else -1

                    Log.d("CalendarData", "Event Title: $eventTitle")

                    it.moveToNext()
                }
            }
        }
    }

    data class CalendarData(val calendarId: Long, val accountName: String, val displayName: String)

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

        val sortOrder: String = "${CalendarContract.Calendars._ID} ASC"
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

                    // Ausgabe der Kalenderdaten in der Logcat-Konsole
                    Log.d("AvailableCalendar", "Display Name: $displayName")

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

        fun openDialog() {
            showDialog = true
        }

        ListItem(
            headlineText = { Text(text = calendar.displayName) },
            trailingContent = {
                IconButton(
                    onClick = {
                        Log.d("lock", calendar.displayName)
                        openDialog()
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
            EventDialog(onDismiss = {
                showDialog = false
            })
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

    @Composable
    fun EventDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "moin") },
            confirmButton = {
                TextButton(onClick = { /*TODO*/ }) {
                    Text(text = "ok")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                }) {
                    Text(text = "abbrechen")
                }
            }
        )
    }
}
