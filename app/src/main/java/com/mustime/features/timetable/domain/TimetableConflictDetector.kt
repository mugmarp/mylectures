package com.mustime.features.timetable.domain

import com.mustime.core.util.TimeUtil

data class ConflictDetail(
    val entryA: TimetableEntry,
    val entryB: TimetableEntry,
    val day: String,
    val overlapStart: String,
    val overlapEnd: String,
    val overlapMinutes: Int
)

object TimetableConflictDetector {

    /**
     * Checks if two day strings refer to the same day of the week (e.g. "Monday" and "Mon").
     */
    fun daysMatch(day1: String, day2: String): Boolean {
        val d1 = day1.trim().lowercase()
        val d2 = day2.trim().lowercase()
        if (d1.isEmpty() || d2.isEmpty()) return false
        if (d1 == d2) return true
        return d1.startsWith(d2.take(3)) || d2.startsWith(d1.take(3))
    }

    /**
     * Finds all pairwise lecture collisions in a given list of timetable entries.
     */
    fun findConflicts(entries: List<TimetableEntry>): List<ConflictDetail> {
        val conflicts = mutableListOf<ConflictDetail>()
        val n = entries.size
        for (i in 0 until n) {
            val a = entries[i]
            val aStart = TimeUtil.toMinutes(a.startTime)
            val aEnd = TimeUtil.toMinutes(a.endTime)
            if (aEnd <= aStart) continue

            for (j in i + 1 until n) {
                val b = entries[j]
                if (!daysMatch(a.day, b.day)) continue

                val bStart = TimeUtil.toMinutes(b.startTime)
                val bEnd = TimeUtil.toMinutes(b.endTime)
                if (bEnd <= bStart) continue

                if (TimeUtil.rangesOverlap(aStart, aEnd, bStart, bEnd)) {
                    val overlapStartMin = maxOf(aStart, bStart)
                    val overlapEndMin = minOf(aEnd, bEnd)
                    val overlapDuration = overlapEndMin - overlapStartMin
                    val overlapStartStr = String.format(java.util.Locale.US, "%02d:%02d", overlapStartMin / 60, overlapStartMin % 60)
                    val overlapEndStr = String.format(java.util.Locale.US, "%02d:%02d", overlapEndMin / 60, overlapEndMin % 60)

                    conflicts.add(
                        ConflictDetail(
                            entryA = a,
                            entryB = b,
                            day = a.day,
                            overlapStart = overlapStartStr,
                            overlapEnd = overlapEndStr,
                            overlapMinutes = overlapDuration
                        )
                    )
                }
            }
        }
        return conflicts
    }

    /**
     * Checks if a specific timetable entry collides with any other entry in the list.
     */
    fun hasConflict(target: TimetableEntry, entries: List<TimetableEntry>): Boolean {
        val targetStart = TimeUtil.toMinutes(target.startTime)
        val targetEnd = TimeUtil.toMinutes(target.endTime)
        if (targetEnd <= targetStart) return false

        return entries.any { other ->
            if (other.naturalKey == target.naturalKey) return@any false
            if (!daysMatch(target.day, other.day)) return@any false
            val otherStart = TimeUtil.toMinutes(other.startTime)
            val otherEnd = TimeUtil.toMinutes(other.endTime)
            if (otherEnd <= otherStart) return@any false
            TimeUtil.rangesOverlap(targetStart, targetEnd, otherStart, otherEnd)
        }
    }

    /**
     * Checks if a timetable entry collides with a custom user event.
     */
    fun hasConflictWithEvent(entry: TimetableEntry, event: CustomEvent): Boolean {
        if (!daysMatch(entry.day, event.dayOfWeek)) return false
        val entryStart = TimeUtil.toMinutes(entry.startTime)
        val entryEnd = TimeUtil.toMinutes(entry.endTime)
        val eventStart = TimeUtil.toMinutes(event.startTime)
        val eventEnd = TimeUtil.toMinutes(event.endTime)

        if (entryEnd <= entryStart || eventEnd <= eventStart) return false
        return TimeUtil.rangesOverlap(entryStart, entryEnd, eventStart, eventEnd)
    }
}
