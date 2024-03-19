package com.example.eventcountdown.ui.views

import android.Manifest
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CalendarNotGranted(requestPermissionLauncher: ActivityResultLauncher<String>) {
    Button(
        onClick = {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    ) {
        Text(text = "Berechtigung erteilen")
    }
}

@Composable
fun CalendarDenied() {
    Text(text = "Berechtigung wurde abgelehnt. Die App benötigt diese Berechtigung, um die Kalender anzuzeigen.")
}