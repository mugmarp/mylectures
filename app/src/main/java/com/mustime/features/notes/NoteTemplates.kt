package com.mustime.features.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NoteTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tag: String,
    val defaultTitle: String,
    val content: String
)

object NoteTemplatesRepository {

    val templates = listOf(
        NoteTemplate(
            id = "cornell",
            title = "Cornell Lecture Note",
            description = "Structured academic notes with cues, lecture details, and summary.",
            icon = Icons.Outlined.School,
            tag = "Lecture",
            defaultTitle = "Lecture: [Topic / Unit]",
            content = """
# Lecture: [Topic / Unit Title]
**Course:** [Course Code] · **Date:** [Date]
**Lecturer:** [Lecturer Name] · **Venue:** [Lecture Room]

> **Core Objective:** What is the primary concept or theorem being studied today?

## 📌 Key Cues & Questions
- What problem does this solve?
- What are the fundamental assumptions or properties?
- How does this connect to previous topics?

## 📝 Detailed Lecture Notes
- **Definition:** Key definition explained in plain terms.
- **Mechanism / Concept 1:**
  - Sub-point A
  - Sub-point B
- **Mechanism / Concept 2:**
  - Details and edge cases

## 🔬 Examples & Practice
- Example 1: Walkthrough problem
- Solution notes:

## ✅ Follow-up Checklist
- [ ] Review lecture slides on Moodle
- [ ] Attempt practice problem set
- [ ] Ask question during office hours or tutorial

## 💡 Summary (Key Takeaway)
In 2-3 sentences, summarize the most important takeaway from this lecture.
""".trimIndent()
        ),
        NoteTemplate(
            id = "lab_report",
            title = "Lab & Practical Log",
            description = "Ideal for science, computing, engineering, and medical labs.",
            icon = Icons.Outlined.Science,
            tag = "Lab Prep",
            defaultTitle = "Lab Practical: [Experiment / Task]",
            content = """
# Lab Practical: [Experiment Name]
**Course:** [Course Code] · **Lab Venue:** Computer / Science Lab
**Date:** [Date] · **Group / Station:** [Station #]

## 🎯 Practical Objectives
1. Understand the theoretical principles of the experiment.
2. Execute the procedure accurately and record measurable metrics.

## 🛠️ Tools, Equipment & Setup
- Tool / Software:
- Equipment / Reagents:
- Pre-requisite configurations:

## 🔬 Step-by-Step Procedure
1. Initialize test environment and verify connections.
2. Run baseline measurements.
3. Apply variables and record output.

## 📊 Observations & Results
> **Key Finding:** Summarize unexpected behavior or critical readings here.

- Measurement 1:
- Measurement 2:

## ✅ Post-Lab Action Items
- [ ] Save dataset / code commits
- [ ] Complete discussion section
- [ ] Submit lab report before deadline
""".trimIndent()
        ),
        NoteTemplate(
            id = "assignment",
            title = "Assignment & Coursework Tracker",
            description = "Track requirements, research notes, references, and completion checklist.",
            icon = Icons.Outlined.Assignment,
            tag = "Assignment",
            defaultTitle = "Coursework: [Assignment Title]",
            content = """
# Coursework: [Assignment Title]
**Course:** [Course Code] · **Weight:** 20%
**Due Date:** [Due Date & Time] · **Submission Mode:** University Portal

> ⚠️ **Deadline Alert:** Must be submitted on university portal before 11:59 PM. Late penalties apply.

## 📌 Assignment Brief & Requirements
- Target length / deliverables:
- Specific formatting guidelines (e.g. IEEE / APA):
- Grading criteria highlights:

## 📑 Task Breakdown & Checklist
- [ ] Read rubric and define problem statement
- [ ] Conduct literature research and gather sources
- [ ] Implement core logic / write first draft
- [ ] Verify test results / proofread text
- [ ] Run plagiarism check and format references
- [ ] Export final PDF and submit on Moodle

## 🔗 Reference Links & Papers
- [University Portal](https://moodle.must.ac.ug)
- Source 1: Author, Title, Year
""".trimIndent()
        ),
        NoteTemplate(
            id = "exam_revision",
            title = "Exam & Revision Sheet",
            description = "High-yield formulas, key terms, likely exam questions, and confidence tracker.",
            icon = Icons.Outlined.Psychology,
            tag = "Revision",
            defaultTitle = "Revision Sheet: [Course Code]",
            content = """
# High-Yield Revision: [Course Code]
**Target Exam:** End of Semester Examination

## 🔑 Must-Memorize Definitions
- **Concept A:** Precise definition and context.
- **Concept B:** Key differences when compared to Concept A.
- **Theorem / Rule:** Critical condition when it holds true.

## 📐 Formulas & Equations
> `Formula: Output = (Input * Rate) / Standard_Constant`

## ❓ Frequently Tested Exam Questions
1. Compare and contrast Method X and Method Y.
2. Derive the expression for system throughput.
3. List 4 real-world failure scenarios and mitigation steps.

## 🎯 Topic Confidence Checklist
- [ ] Unit 1: Foundations & Definitions
- [ ] Unit 2: Core Methodology
- [ ] Unit 3: Advanced Applications
- [ ] Past Paper (2024 Semester 1) Solved
- [ ] Past Paper (2025 Semester 1) Solved
""".trimIndent()
        ),
        NoteTemplate(
            id = "scratchpad",
            title = "Quick Lecture Scratchpad",
            description = "Simple, lightweight bullet points and immediate to-dos.",
            icon = Icons.Outlined.Notes,
            tag = "Lecture",
            defaultTitle = "Quick Notes: [Subject]",
            content = """
# Lecture Notes: [Subject]
**Date:** [Date]

- Main discussion topic today:
- Important note from lecturer:
- Warning about test or quiz next week:

## Quick To-Dos
- [ ] Read pages 45-60
- [ ] Review today's code sample
""".trimIndent()
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTemplatesBottomSheet(
    onDismiss: () -> Unit,
    onSelectTemplate: (NoteTemplate) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Academic Note Templates",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jumpstart your notes with pre-formatted structures",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NoteTemplatesRepository.templates, key = { it.id }) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTemplate(template)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    template.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = template.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            template.tag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = template.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
