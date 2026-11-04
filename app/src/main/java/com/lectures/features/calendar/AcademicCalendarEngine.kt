package com.lectures.features.calendar

import java.util.Calendar

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
    val startYear: Int,
    val startMonth: Int, // 0-based: 0=Jan, 7=Aug, etc.
    val startDay: Int,
    val endYear: Int,
    val endMonth: Int,
    val endDay: Int,
    val phase: AcademicPhase,
    val isHoliday: Boolean = false
) {
    // Zero-allocation YYYYMMDD integers for instant range comparisons
    val startYmd: Int = startYear * 10000 + (startMonth + 1) * 100 + startDay
    val endYmd: Int = endYear * 10000 + (endMonth + 1) * 100 + endDay

    fun contains(year: Int, month: Int, day: Int): Boolean {
        val ymd = year * 10000 + (month + 1) * 100 + day
        return ymd in startYmd..endYmd
    }

    fun isLiveNow(now: Calendar = Calendar.getInstance()): Boolean {
        val nowYmd = now.get(Calendar.YEAR) * 10000 + (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH)
        return nowYmd in startYmd..endYmd
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

    // Singleton immutable milestones schedule for the 2026/2027 Academic Year
    private val ACADEMIC_MILESTONES = listOf(
        AcademicMilestone(
            id = "sem1_orient",
            title = "Orientation & Registration",
            description = "Freshers orientation, student registration & course enrollment verification.",
            dateDisplay = "Aug 24 – Aug 28, 2026",
            startYear = 2026, startMonth = Calendar.AUGUST, startDay = 24,
            endYear = 2026, endMonth = Calendar.AUGUST, endDay = 28,
            phase = AcademicPhase.ORIENTATION
        ),
        AcademicMilestone(
            id = "sem1_commence",
            title = "Semester 1 Lectures Commence",
            description = "Official commencement of teaching and lab sessions across all departments.",
            dateDisplay = "Aug 31, 2026",
            startYear = 2026, startMonth = Calendar.AUGUST, startDay = 31,
            endYear = 2026, endMonth = Calendar.AUGUST, endDay = 31,
            phase = AcademicPhase.TEACHING
        ),
        AcademicMilestone(
            id = "sem1_add_drop",
            title = "Course Add / Drop Deadline",
            description = "Final day for students to adjust elective course modules with departmental approval.",
            dateDisplay = "Sep 18, 2026",
            startYear = 2026, startMonth = Calendar.SEPTEMBER, startDay = 18,
            endYear = 2026, endMonth = Calendar.SEPTEMBER, endDay = 18,
            phase = AcademicPhase.TEACHING
        ),
        AcademicMilestone(
            id = "independence_day",
            title = "Independence Day (Public Holiday)",
            description = "National Public Holiday. University lectures and administrative offices closed.",
            dateDisplay = "Oct 9, 2026",
            startYear = 2026, startMonth = Calendar.OCTOBER, startDay = 9,
            endYear = 2026, endMonth = Calendar.OCTOBER, endDay = 9,
            phase = AcademicPhase.RECESS,
            isHoliday = true
        ),
        AcademicMilestone(
            id = "sem1_midsem",
            title = "Mid-Semester Continuous Assessment",
            description = "Mid-term tests, practical assessments & project progress presentations (Week 8).",
            dateDisplay = "Oct 19 – Oct 23, 2026",
            startYear = 2026, startMonth = Calendar.OCTOBER, startDay = 19,
            endYear = 2026, endMonth = Calendar.OCTOBER, endDay = 23,
            phase = AcademicPhase.MID_SEM_TESTS
        ),
        AcademicMilestone(
            id = "sem1_revision",
            title = "Semester 1 Revision Week",
            description = "Lecture-free reading week dedicated to independent study and examination prep.",
            dateDisplay = "Dec 7 – Dec 11, 2026",
            startYear = 2026, startMonth = Calendar.DECEMBER, startDay = 7,
            endYear = 2026, endMonth = Calendar.DECEMBER, endDay = 11,
            phase = AcademicPhase.REVISION
        ),
        AcademicMilestone(
            id = "sem1_exams",
            title = "Semester 1 Final Examinations",
            description = "End of Semester 1 comprehensive university examinations period.",
            dateDisplay = "Dec 14 – Dec 23, 2026",
            startYear = 2026, startMonth = Calendar.DECEMBER, startDay = 14,
            endYear = 2026, endMonth = Calendar.DECEMBER, endDay = 23,
            phase = AcademicPhase.EXAMINATIONS
        ),
        AcademicMilestone(
            id = "sem1_recess",
            title = "Semester 1 Recess & Holidays",
            description = "Christmas and New Year academic break. Re-opening in mid-January.",
            dateDisplay = "Dec 24, 2026 – Jan 17, 2027",
            startYear = 2026, startMonth = Calendar.DECEMBER, startDay = 24,
            endYear = 2027, endMonth = Calendar.JANUARY, endDay = 17,
            phase = AcademicPhase.RECESS,
            isHoliday = true
        ),
        AcademicMilestone(
            id = "sem2_commence",
            title = "Semester 2 Lectures Commence",
            description = "Resumption of classes, coursework, and laboratory sessions for Semester 2.",
            dateDisplay = "Jan 18, 2027",
            startYear = 2027, startMonth = Calendar.JANUARY, startDay = 18,
            endYear = 2027, endMonth = Calendar.JANUARY, endDay = 18,
            phase = AcademicPhase.TEACHING
        ),
        AcademicMilestone(
            id = "sem2_midsem",
            title = "Semester 2 Mid-Semester Tests",
            description = "Continuous assessment tests and coursework submission checkpoint.",
            dateDisplay = "Mar 8 – Mar 12, 2027",
            startYear = 2027, startMonth = Calendar.MARCH, startDay = 8,
            endYear = 2027, endMonth = Calendar.MARCH, endDay = 12,
            phase = AcademicPhase.MID_SEM_TESTS
        ),
        AcademicMilestone(
            id = "easter_break",
            title = "Easter Recess (Public Holiday)",
            description = "Good Friday & Easter Monday university break.",
            dateDisplay = "Mar 26 – Mar 29, 2027",
            startYear = 2027, startMonth = Calendar.MARCH, startDay = 26,
            endYear = 2027, endMonth = Calendar.MARCH, endDay = 29,
            phase = AcademicPhase.RECESS,
            isHoliday = true
        ),
        AcademicMilestone(
            id = "sem2_revision",
            title = "Semester 2 Revision Week",
            description = "Reading week and final project presentation reviews.",
            dateDisplay = "May 3 – May 7, 2027",
            startYear = 2027, startMonth = Calendar.MAY, startDay = 3,
            endYear = 2027, endMonth = Calendar.MAY, endDay = 7,
            phase = AcademicPhase.REVISION
        ),
        AcademicMilestone(
            id = "sem2_exams",
            title = "Semester 2 Final Examinations",
            description = "End of academic year examinations and graduation clearance.",
            dateDisplay = "May 10 – May 21, 2027",
            startYear = 2027, startMonth = Calendar.MAY, startDay = 10,
            endYear = 2027, endMonth = Calendar.MAY, endDay = 21,
            phase = AcademicPhase.EXAMINATIONS
        )
    )

    fun getMilestonesForAcademicYear(): List<AcademicMilestone> = ACADEMIC_MILESTONES

    // Anchor timestamps in milliseconds for zero-allocation semester week calculations
    private val SEM1_START_MILLIS: Long
    private val SEM2_START_MILLIS: Long

    init {
        SEM1_START_MILLIS = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        SEM2_START_MILLIS = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 18, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Computes the Academic Context for any given calendar date using fast integer arithmetic.
     */
    fun getAcademicContextForDate(year: Int, month: Int, day: Int): DayAcademicContext {
        val targetYmd = year * 10000 + (month + 1) * 100 + day

        // Fast zero-allocation milestone lookup
        val matchingMilestone = ACADEMIC_MILESTONES.firstOrNull { targetYmd in it.startYmd..it.endYmd }

        // Determine semester number and anchor
        val isSem1 = targetYmd in 20260824..20270117
        val isSem2 = targetYmd in 20270118..20270630

        val (semesterNum, semesterLabel) = when {
            isSem1 -> 1 to "Semester 1"
            isSem2 -> 2 to "Semester 2"
            targetYmd < 20260824 -> 2 to "Academic Recess"
            else -> 2 to "Academic Recess"
        }

        val targetCal = Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val semStartMillis = if (semesterNum == 1) SEM1_START_MILLIS else SEM2_START_MILLIS
        val diffDays = ((targetCal.timeInMillis - semStartMillis) / (1000L * 60 * 60 * 24)).toInt()
        val calculatedWeek = if (diffDays >= 0) (diffDays / 7) + 1 else 0

        val (phase, weekLabel, isInstructional) = when {
            matchingMilestone != null -> {
                val isInst = matchingMilestone.phase == AcademicPhase.TEACHING ||
                        matchingMilestone.phase == AcademicPhase.MID_SEM_TESTS
                Triple(
                    matchingMilestone.phase,
                    if (calculatedWeek in 1..17) "Week $calculatedWeek" else matchingMilestone.phase.badgeLabel,
                    isInst && !matchingMilestone.isHoliday
                )
            }
            !isSem1 && !isSem2 -> {
                Triple(AcademicPhase.RECESS, "Recess / Break", false)
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
