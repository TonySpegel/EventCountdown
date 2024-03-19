package com.example.eventcountdown.helper

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class CalendarData(val calendarId: Long, val accountName: String, val displayName: String)

data class CalendarEvent(
    val eventId: Long,
    val eventTitle: String,
    val startTime: Long,
    val endTime: Long
)

fun getRemainingDays(startTime: Long): Int {
    val currentTime = System.currentTimeMillis()
    val remainingTimeInMillis = startTime - currentTime
    val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingTimeInMillis)
    return remainingDays.toInt()
}

fun getStartOfLast14DaysInMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -14)
    return calendar.timeInMillis
}

fun getStartDaysInMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR,0)
    return calendar.timeInMillis
}

@SuppressLint("SimpleDateFormat")
fun convertLongToTime(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("dd.MM.yyyy")
    return format.format(date)
}

@SuppressLint("SimpleDateFormat")
fun cLTT(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("EEEE, dd. MMMM")
    return format.format(date)
}

fun calendarDisplayName(context: Context,calendarId: Int): String? {
    val contentResolver: ContentResolver = context.contentResolver
    val uri: Uri = CalendarContract.Calendars.CONTENT_URI

    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
    )

    val selection = "${CalendarContract.Calendars._ID} = ?"
    val selectionArgs = arrayOf(calendarId.toString())

    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            return it.getString(displayNameIndex)
        }
    }

    return null
}

fun readCalendarEvents(context: Context, calendarId: Int): List<CalendarEvent> {
    val contentResolver: ContentResolver = context.contentResolver
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
        getStartDaysInMillis().toString()
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

fun openCalendarEvent(context: Context, eventId: Long) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setData(CalendarContract.Events.CONTENT_URI.buildUpon()
            .appendPath(eventId.toString())
            .build())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}