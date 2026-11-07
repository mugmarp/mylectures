package com.lectures.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/**
 * Ambient CompositionLocal providing real-time clock timestamp in milliseconds.
 * Pulses every 15 seconds while app is in foreground and IMMEDIATELY updates
 * synchronously whenever the user returns to the app from the home screen / background.
 */
val LocalTimePulse = compositionLocalOf { 0L }

/**
 * Composable utility that returns an observable clock pulse (timestamp in ms).
 * Automatically updates on:
 * 1. Immediate ON_START / ON_RESUME (returning from background / home screen)
 * 2. Regular 15-second timer while the screen is active in foreground
 */
@Composable
fun rememberLiveClockPulse(intervalMillis: Long = 15_000L): Long {
    var clockPulse by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Synchronous refresh on foreground return (ON_RESUME & ON_START)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                clockPulse = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic heartbeat pulse while in foreground
    LaunchedEffect(intervalMillis) {
        while (true) {
            delay(intervalMillis)
            clockPulse = System.currentTimeMillis()
        }
    }

    return clockPulse
}
