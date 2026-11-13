package com.lectures.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Removes an existing alarm token without creating a new one during cleanup. */
internal fun cancelAlarmToken(context: Context, manager: AlarmManager, code: Int, intent: Intent) {
    val token = PendingIntent.getBroadcast(
        context, code, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: return
    manager.cancel(token)
    token.cancel()
}
