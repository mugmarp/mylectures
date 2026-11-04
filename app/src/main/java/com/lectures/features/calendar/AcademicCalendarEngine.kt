package com.lectures.features.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AcademicPhase(val displayName: String, val badgeLabel: String) {
    ORIENTATION("Orientation & Registration", "Orientation"),
    TEACHING("Teaching & Instruction", "Teaching"),
    MID_SEM_TESTS("Mid-Semester Assessments", "Mid-Sem Tests"),
    REVISION("Revision & Reading Week", "Revision"),
    EXAMINATIONS("Final University Examinations", "Exams"),
    RECESS("Semester Recess & Break", "Recess")
}

data class AcademicMilestone(
    val id: String,
    val title: String,
    val description: String,
    val dateDisplay: String,
    val startCal: Calendar,
    val endCal: Calendar,
    val phase: AcademicPhase,
    val isHoliday: Boolean = false
) {
    fun contains(year: Int, month: Int, day: Int): Boolean {
        val check = Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
        }
        val start = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = endCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return check.timeInMillis in start.timeInMillis..end.timeInMillis
    }
}

data class DayAcademicContext(
    val semesterNumber: Int,
    val semesterLabel: String,
    val weekNumber: Int?,
    val weekLabel: String,
    val phase: AcademicPhase,
    val primaryMilestone: AcademicMilestone? = null,
    val isInstructionalDay: Boolean
)

object AcademicCalendarEngine {

    /**
     * Official Academic Milestones schedule for the Academic Year (2026 / 2027).
     * Provides structured visibility for semesters, tests, exams, and university dates.
     */
    fun getMilestonesForAcademicYear(): List<AcademicMilestone> {
        return listOf(
            createMilestone(
                id = "sem1_orient",
                title = "Orientation & Registration",
                description = "Freshers orientation, student registration & course enrollment verification.",
                dateDisplay = "Aug 24 – Aug 28, 2026",
                start = Triple(2026, Calendar.AUGUST, 24),
                end = Triple(2026, Calendar.AUGUST, 28),
                phase = AcademicPhase.ORIENTATION
            ),
            createMilestone(
                id = "sem1_commence",
                title = "Semester 1 Lectures Commence",
                description = "Official commencement of teaching and lab sessions across all departments.",
                dateDisplay = "Aug 31, 2026",
                start = Triple(2026, Calendar.AUGUST, 31),
                end = Triple(2026, Calendar.AUGUST, 31),
                phase = AcademicPhase.TEACHING
            ),
            createMilestone(
                id = "sem1_add_drop",
                title = "Course Add / Drop Deadline",
                description = "Final day for students to adjust elective course modules with departmental approval.",
                dateDisplay = "Sep 18, 2026",
                start = Triple(2026, Calendar.SEPTEMBER, 18),
                end = Triple(2026, Calendar.SEPTEMBER, 18),
                phase = AcademicPhase.TEACHING
            ),
            createMilestone(
                id = "independence_day",
                title = "Independence Day (Public Holiday)",
                description = "National Public Holiday. University lectures and administrative offices closed.",
                dateDisplay = "Oct 9, 2026",
                start = Triple(2026, Calendar.OCTOBER, 9),
                end = Triple(2026, Calendar.OCTOBER, 9),
                phase = AcademicPhase.RECESS,
                isHoliday = true
            ),
            createMilestone(
                id = "sem1_midsem",
                title = "Mid-Semester Continuous Assessment",
                description = "Mid-term tests, practical assessments & project progress presentations (Week 8).",
                dateDisplay = "Oct 19 – Oct 23, 2026",
                start = Triple(2026, Calendar.OCTOBER, 19),
                end = Triple(2026, Calendar.OCTOBER, 23),
                phase = AcademicPhase.MID_SEM_TESTS
            ),
            createMilestone(
                id = "sem1_revision",
                title = "Semester 1 Revision Week",
                description = "Lecture-free reading week dedicated to independent study and examination prep.",
                dateDisplay = "Dec 7 – Dec 11, 2026",
                start = Triple(2026, Calendar.DECEMBER, 7),
                end = Triple(2026, Calendar.DECEMBER, 11),
                phase = AcademicPhase.REVISION
            ),
            createMilestone(
                id = "sem1_exams",
                title = "Semester 1 Final Examinations",
                description = "End of Semester 1 comprehensive university examinations period.",
                dateDisplay = "Dec 14 – Dec 23, 2026",
                start = Triple(2026, Calendar.DECEMBER, 14),
                end = Triple(2026, Calendar.DECEMBER, 23),
                phase = AcademicPhase.EXAMINATIONS
            ),
            createMilestone(
                id = "sem1_recess",
                title = "Semester 1 Recess & Holidays",
                description = "Christmas and New Year academic break. Re-opening in mid-January.",
                dateDisplay = "Dec 24, 2026 – Jan 17, 2027",
                start = Triple(2026, Calendar.DECEMBER, 24),
                end = Triple(2027, Calendar.JANUARY, 17),
                phase = AcademicPhase.RECESS,
                isHoliday = true
            ),
            createMilestone(
                id = "sem2_commence",
                title = "Semester 2 Lectures Commence",
                description = "Resumption of classes, coursework, and laboratory sessions for Semester 2.",
                dateDisplay = "Jan 18, 2027",
                start = Triple(2027, Calendar.JANUARY, 18),
                end = Triple(2027, Calendar.JANUARY, 18),
                phase = AcademicPhase.TEACHING
            ),
            createMilestone(
                id = "sem2_midsem",
                title = "Semester 2 Mid-Semester Tests",
                description = "Continuous assessment tests and coursework submission checkpoint.",
                dateDisplay = "Mar 8 – Mar 12, 2027",
                start = Triple(2027, Calendar.MARCH, 8),
                end = Triple(2027, Calendar.MARCH, 12),
                phase = AcademicPhase.MID_SEM_TESTS
            ),
            createMilestone(
                id = "easter_break",
                title = "Easter Recess (Public Holiday)",
                description = "Good Friday & Easter Monday university break.",
                dateDisplay = "Mar 26 – Mar 29, 2027",
                start = Triple(2027, Calendar.MARCH, 26),
                end = Triple(2027, Calendar.MARCH, 29),
                phase = AcademicPhase.RECESS,
                isHoliday = true
            ),
            createMilestone(
                id = "sem2_revision",
                title = "Semester 2 Revision Week",
                description = "Reading week and final project presentation reviews.",
                dateDisplay = "May 3 – May 7, 2027",
                start = Triple(2027, Calendar.MAY, 3),
                end = Triple(2027, Calendar.MAY, 7),
                phase = AcademicPhase.REVISION
            ),
            createMilestone(
                id = "sem2_exams",
                title = "Semester 2 Final Examinations",
                description = "End of academic year examinations and graduation clearance.",
                dateDisplay = "May 10 – May 21, 2027",
                start = Triple(2027, Calendar.MAY, 10),
                end = Triple(2027, Calendar.MAY, 21),
                phase = AcademicPhase.EXAMINATIONS
            )
        )
    }

    private fun createMilestone(
        id: String,
        title: String,
        description: String,
        dateDisplay: String,
        start: Triple<Int, Int, Int>,
        end: Triple<Int, Int, Int>,
        phase: AcademicPhase,
        isHoliday: Boolean = false
    ): AcademicMilestone {
        val startCal = Calendar.getInstance().apply {
            set(start.first, start.second, start.third, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(end.first, end.second, end.third, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return AcademicMilestone(
            id = id,
            title = title,
            description = description,
            dateDisplay = dateDisplay,
            startCal = startCal,
            endCal = endCal,
            phase = phase,
            isHoliday = isHoliday
        )
    }

    /**
     * Computes the Academic Context for any given calendar date.
     */
    fun getAcademicContextForDate(year: Int, month: Int, day: Int): DayAcademicContext {
        val target = Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check against known milestones first
        val matchingMilestone = getMilestonesForAcademicYear().firstOrNull { it.contains(year, month, day) }

        // Determine semester start anchor
        // Sem 1 starts late August 2026; Sem 2 starts mid-Jan 2027
        val isSem1 = (month in Calendar.AUGUST..Calendar.DECEMBER) ||
                (month == Calendar.JANUARY && day < 18 && year == 2027)

        val semesterNum = if (isSem1) 1 else 2
        val semesterLabel = "Semester $semesterNum"

        val semStart = Calendar.getInstance().apply {
            if (semesterNum == 1) {
                set(2026, Calendar.AUGUST, 31, 0, 0, 0)
            } else {
                set(2027, Calendar.JANUARY, 18, 0, 0, 0)
            }
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((target.timeInMillis - semStart.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val calculatedWeek = if (diffDays >= 0) (diffDays / 7) + 1 else 0

        val (phase, weekLabel, isInstructional) = when {
            matchingMilestone != null -> {
                val isInst = matchingMilestone.phase == AcademicPhase.TEACHING ||
                        matchingMilestone.phase == AcademicPhase.MID_SEM_TESTS
                Triple(matchingMilestone.phase, if (calculatedWeek in 1..17) "Week $calculatedWeek" else matchingMilestone.phase.badgeLabel, isInst && !matchingMilestone.isHoliday)
            }
            calculatedWeek in 1..14 -> {
                Triple(AcademicPhase.TEACHING, "Week $calculatedWeek", true)
            }
            calculatedWeek == 15 -> {
                Triple(AcademicPhase.REVISION, "Revision Week (Week 15)", false)
            }
            calculatedWeek in 16..17 -> {
                Triple(AcademicPhase.EXAMINATIONS, "Exam Period (Week $calculatedWeek)", false)
            }
            else -> {
                Triple(AcademicPhase.RECESS, "Recess / Break", false)
            }
        }

        return DayAcademicContext(
            semesterNumber = semesterNum,
            semesterLabel = semesterLabel,
            weekNumber = if (calculatedWeek in 1..17) calculatedWeek else null,
            weekLabel = weekLabel,
            phase = phase,
            primaryMilestone = matchingMilestone,
            isInstructionalDay = isInstructional
        )
    }
}
