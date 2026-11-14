# Alarm and notification delivery

Class, task, snooze, test, and daily-digest broadcasts use explicit URI
identities. These keep separate alarms independent even when integer request
codes collide. Successful replacement removes the previous URI-less alarm
token; cancellation removes both the current and legacy regular tokens.
Cancelling a class also cancels its snooze. Explicit test alarms remain separate.

Class trigger calculation advances to the class day before applying the lead
time, including reminders that cross midnight. A trigger less than a minute
away remains eligible. Automatic scheduling reads saved note alarms to preserve
custom lead times during startup, boot recovery, timetable loading, and changes
to notification settings.

Normal scheduling and delivery respect quiet mode. Explicit tests remain
available. The daily digest receiver is manifest-registered. Asynchronous boot,
digest, and task-action receiver work uses an 8.5-second timeout and finishes its
pending result in `finally`, following the bounded-work requirements of
[BroadcastReceiver.goAsync](https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()).

Schedulers use exact alarms when available and fall back to inexact delivery
when exact access is unavailable or raises a security exception. Inexact
delivery can be delayed by the system; see the Android
[alarm scheduling guide](https://developer.android.com/develop/background-work/services/alarms).

Task date parsing uses a fresh formatter per attempt and requires complete
matches. Explicit times and past years are preserved, invalid calendar dates
are rejected, and ISO dates survive normalization. Notification permission
decisions and banner dismissal persist; returning from system settings refreshes
the task permission state without automatically reopening settings on denial.

## Verification

`AlarmDeliveryTest` and `AlarmDateCalculationTest` cover identity collisions,
legacy cleanup, cancellation, custom lead times, quiet mode, manifest delivery,
permission persistence, inexact fallback, midnight boundaries, strict date/time
parsing, and concurrent formatter use. Existing alarm and notification tests
remain part of the full CI suite.

Device checks remain necessary for real reboot/package replacement, doze,
exact-alarm permission changes, notification permission dialogs, snooze/dismiss,
task completion actions, and delivery timing. These hardware and UI paths have
not been exercised locally.
