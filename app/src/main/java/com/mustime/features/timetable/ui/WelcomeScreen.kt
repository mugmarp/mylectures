package com.mustime.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.R
import com.mustime.ui.LocalAppTheme

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark

    var termsAcknowledged by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val primaryBlue = Color(0xFF2563EB)
    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0F172A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE0E7FF), Color(0xFFEEF2FF), Color(0xFFF8FAFC)))
    }
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF64748B)

    // Institutional Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = primaryBlue) },
            title = {
                Text(
                    text = "Institutional Disclaimer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Independent Student Companion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "Lectures is an independent student companion tool designed for academic schedule tracking, note-taking, and coursework planning.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = "2. Non-Affiliation Notice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "Lectures is NOT an official application of, nor is it endorsed by or associated with Mbarara University of Science and Technology (MUST) or its departments.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = "3. Verification of Timetables",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "Schedule slots are compiled for personal student assistance. Students should cross-reference official university online portals (timetable.must.ac.ug) for official announcements.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = "4. Schedule Accuracy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "Timetables are stored locally for fast offline access. Students are encouraged to consult official university online systems for examination and room allocation updates.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDisclaimerDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Text("I Understand")
                }
            }
        )
    }

    // Terms & Regulations Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = primaryBlue) },
            title = {
                Text(
                    text = "Terms of Service",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Permitted Personal Use",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "Lectures is provided for personal, non-commercial academic schedule tracking, note-taking, and task planning.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = "2. 'As-Is' Warranty Disclaimer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "The software is provided on an 'AS IS' basis without warranties. The developer assumes no liability for missed classes or schedule changes.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = "3. Local Privacy Guarantee",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryBlue
                    )
                    Text(
                        text = "All notes, personal tasks, and timetable preferences are stored 100% locally on your device in private SQLite storage without remote telemetry.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Text("Accept Terms")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Hero Header: Prominent Brand Emblem, Name & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    modifier = Modifier.size(92.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = cardBg,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFDBEAFE))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "Lectures App Logo",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // App Identity Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(primaryBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Smart Campus Companion",
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lectures",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Offline timetables, vacant study rooms & coursework manager.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // 2. Core Feature Showcase: 2x2 Bento Grid (Addresses: "view your lectures or so, find free room")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Lectures Timetable & Free Rooms Finder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WelcomeBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarToday,
                        iconBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFDBEAFE),
                        iconTint = primaryBlue,
                        tag = "Timetable",
                        title = "Your Lectures",
                        description = "Daily classes, halls & lecturers",
                        isDark = isDark,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    WelcomeBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MeetingRoom,
                        iconBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFDCFCE7),
                        iconTint = Color(0xFF059669),
                        tag = "Campus",
                        title = "Free Rooms",
                        description = "Locate empty halls for revision",
                        isDark = isDark,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                // Row 2: Coursework Tasks & Offline Notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WelcomeBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TaskAlt,
                        iconBg = if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        tag = "Tasks",
                        title = "Coursework",
                        description = "Track assignments, tests & deadlines",
                        isDark = isDark,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    WelcomeBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.School,
                        iconBg = if (isDark) Color(0xFF581C87).copy(alpha = 0.4f) else Color(0xFFF3E8FF),
                        iconTint = Color(0xFF7C3AED),
                        tag = "100% Local",
                        title = "Study Notes",
                        description = "Write summaries linked to courses",
                        isDark = isDark,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }

            // 3. Campus Notice & Agreement Card (Notice title clearly NOT "MUST Terms...")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Campus Notice & Terms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disclaimer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showDisclaimerDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text("•", color = textMuted, fontSize = 10.sp)
                            Text(
                                text = "Terms",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showTermsDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Lectures is an independent student companion. Timetable slots and schedules are organized for student study convenience.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Interactive Checkbox row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { termsAcknowledged = !termsAcknowledged }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = termsAcknowledged,
                            onCheckedChange = { termsAcknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = primaryBlue,
                                uncheckedColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I agree to the Terms of Service & Campus Notice.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (termsAcknowledged) textPrimary else textSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // 4. Primary "Get Started" Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGetStarted,
                    enabled = termsAcknowledged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("get_started_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue,
                        disabledContainerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Get Started",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (termsAcknowledged) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (termsAcknowledged) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!termsAcknowledged) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please accept the notice above to continue.",
                        fontSize = 10.sp,
                        color = textMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun WelcomeBentoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    tag: String,
    title: String,
    description: String,
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            iconBg.copy(alpha = if (isDark) 0.35f else 0.7f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = textSecondary,
                maxLines = 2
            )
        }
    }
}
