package com.mustime.features.timetable.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mustime.features.timetable.domain.TimetableEntry

@Composable
fun OverlapTimeline(entries: List<TimetableEntry>, onEntryClick: (TimetableEntry) -> Unit) {
    Box {
        Text("Timeline goes here")
    }
}
