package com.mustime.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustime.R

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    var termsAcknowledged by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Institutional Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2563EB)) },
            title = { Text("Institutional Non-Affiliation Disclaimer", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Independent Student Utility",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "Lectures is an independently designed, programmed, and maintained student companion tool authored by MUGENDAWALA MARK PAUL (TiralLab).",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "2. No Official Affiliation or Endorsement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "Lectures is NOT an official application of, nor is it endorsed by, affiliated with, sponsored by, or in any way officially associated with Mbarara University of Science and Technology (MUST) or any of its faculties, departments, or administrative offices.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "3. Community Timetable Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "All course codes, faculty designations, and schedule slots are student-compiled and provided solely for personal organizational assistance. Students are strictly advised and expected to cross-reference official university physical notice boards and administrative releases for official updates, room changes, and examination dates.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "4. Developer Attribution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "Author: MUGENDAWALA MARK PAUL\nStudio: TiralLab (https://tirallab.page)\nGitHub: https://github.com/mugmarp\nEmail: markpaulmu@gmail.com",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDisclaimerDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFF2563EB)) },
            title = { Text("Regulations & Terms of Service", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Permitted Personal Use",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "Lectures is provided for personal, non-commercial academic schedule tracking, note-taking, and task planning. Commercial resale or unauthorized distribution is prohibited.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "2. 'As-Is' Warranty Disclaimer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "The software is provided on an 'AS IS' basis without warranties of any kind. The developer and TiralLab assume no liability for missed classes, test clashes, device alarm failures, or schedule discrepancies.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "3. Local Privacy Guarantee",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "All notes, personal tasks, and timetable preferences are stored 100% locally on your device in private SQLite storage without telemetry, analytics, or remote tracking.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Accept Terms")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE0E7FF), Color(0xFFEEF2FF))))
    ) {
        // Top Illustration Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Lectures Logo Emblem",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }

        // Bottom Content Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                // Header badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF2563EB), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Lectures",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "by TiralLab",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Start Learning\nToday",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 36.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your offline-first companion for class schedules, pre-class alarms, lecture notes, and vacant campus study halls.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // MANDATORY DISCLAIMER & ACKNOWLEDGEMENT CARD
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Institutional Notice & Data Terms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Lectures is an independent companion developed by Mugendawala Mark Paul (TiralLab) and is NOT affiliated with or endorsed by Mbarara University of Science and Technology (MUST). Timetable data is student-curated and may require verification with official physical notice boards.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Read Disclaimer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showDisclaimerDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                            Text("•", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                text = "Terms of Service",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showTermsDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { termsAcknowledged = !termsAcknowledged }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = termsAcknowledged,
                                onCheckedChange = { termsAcknowledged = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I understand the independent nature of this app and accept the Terms & Disclaimer.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (termsAcknowledged) Color(0xFF1E293B) else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Get Started Action Button
                Button(
                    onClick = onGetStarted,
                    enabled = termsAcknowledged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        disabledContainerColor = Color(0xFFE2E8F0)
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
                            color = if (termsAcknowledged) Color.White else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = if (termsAcknowledged) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!termsAcknowledged) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Please check the acknowledgement above to continue.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureIcon(Icons.Default.Notifications, "Alarms", Color(0xFF4F46E5), Color(0xFFEEF2FF))
                    FeatureIcon(Icons.Default.Notes, "Notes", Color(0xFF059669), Color(0xFFDCFCE7))
                    FeatureIcon(Icons.Default.WifiOff, "Offline", Color(0xFFD97706), Color(0xFFFEF3C7))
                }
            }
        }
    }
}

@Composable
fun FeatureIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
