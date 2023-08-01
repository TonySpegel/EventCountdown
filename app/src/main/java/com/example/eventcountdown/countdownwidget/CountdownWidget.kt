package com.example.eventcountdown.countdownwidget

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
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
import androidx.glance.text.Text
import com.example.eventcountdown.MainActivity

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(
                GlanceTheme.colors
            ) {
                ContentView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentView() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.secondary)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Where to?",
            modifier = GlanceModifier.padding(12.dp)
        )
        Row {
            Button(
                text = "Home",
                onClick = actionStartActivity<MainActivity>()
            )
            Button(
                text = "Work",
                onClick = actionStartActivity<MainActivity>()
            )
        }
        LazyColumn {
            items(10) { index: Int ->
                EventItem("Item $index", index, index * 10)
            }
        }
    }
}

@Composable
private fun EventItem(title: String, startTime: Int, days: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val modifier = GlanceModifier.defaultWeight()

        Text(text = title, modifier)
        Text(text = startTime.toString(), modifier)
        Text(text = "$days Tage", modifier)
    }
}