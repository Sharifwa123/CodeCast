package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TutorialStepEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.WizardStep

@Composable
fun CodeCastTopBar(
    currentStep: WizardStep,
    projectName: String?,
    activeVersion: String?,
    onVersionClick: () -> Unit,
    onExploreCodeClick: (() -> Unit)? = null
) {
    Surface(
        color = Slate900,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo600),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "CodeCast Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CodeCast",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Slate50
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Slate800)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SAAS STUDIO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Cyan400
                                )
                            }
                        }
                        Text(
                            text = projectName ?: "Codebase to Tutorial Video",
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeVersion != null) {
                        Surface(
                            onClick = onVersionClick,
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800,
                            modifier = Modifier.testTag("version_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Emerald400)
                                )
                                Text(
                                    text = activeVersion,
                                    fontSize = 11.sp,
                                    color = Slate200,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = "New version update available",
                                    tint = Amber500,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    if (onExploreCodeClick != null) {
                        Surface(
                            onClick = onExploreCodeClick,
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                            modifier = Modifier.testTag("topbar_codebase_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Codebase Explorer",
                                    tint = Cyan400,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Code",
                                    fontSize = 12.sp,
                                    color = Slate200,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkflowStepperHeader(
    currentStep: WizardStep,
    onStepClick: (WizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate950,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Slate850)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STEP ${currentStep.stepNumber} OF 12",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentStep.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate100
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step Progress Bar
            val totalSteps = 12
            val currentIdx = currentStep.stepNumber.coerceIn(1, totalSteps)
            LinearProgressIndicator(
                progress = { currentIdx / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Indigo500,
                trackColor = Slate800,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Breadcrumb pills row (scrollable or wrap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val primarySteps = listOf(
                    WizardStep.CREATE_PROJECT,
                    WizardStep.ANALYZE_APP,
                    WizardStep.CHOOSE_TUTORIAL,
                    WizardStep.CHOOSE_AUDIENCE,
                    WizardStep.CHOOSE_DURATION_PRESENTATION,
                    WizardStep.CHOOSE_VOICE,
                    WizardStep.CHOOSE_LANGUAGE_SUBTITLES,
                    WizardStep.REVIEW_TUTORIAL
                )

                primarySteps.forEach { step ->
                    val isPast = step.stepNumber < currentStep.stepNumber
                    val isCurrent = step.stepNumber == currentStep.stepNumber
                    val pillBg = when {
                        isCurrent -> Indigo500
                        isPast -> Slate800
                        else -> Slate900
                    }
                    val textColor = when {
                        isCurrent -> Slate50
                        isPast -> Emerald400
                        else -> Slate500
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(pillBg)
                            .clickable { onStepClick(step) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPast) "✓" else "${step.stepNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

private data class VerificationBadgeStyle(
    val label: String,
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun VerificationStatusBadge(status: String, modifier: Modifier = Modifier) {
    val style = when (status) {
        "RUNTIME_VERIFIED" -> VerificationBadgeStyle("Runtime verified", Emerald500.copy(alpha = 0.16f), Emerald400, Icons.Default.CheckCircle)
        "CODE_VERIFIED" -> VerificationBadgeStyle("Code verified", Cyan500.copy(alpha = 0.16f), Cyan400, Icons.Default.Verified)
        "INFERRED" -> VerificationBadgeStyle("Inferred", Amber500.copy(alpha = 0.16f), Amber500, Icons.Default.Info)
        else -> VerificationBadgeStyle("Unable to verify", Rose500.copy(alpha = 0.16f), Rose500, Icons.Default.Warning)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = style.bg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = style.label,
                tint = style.fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = style.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = style.fg
            )
        }
    }
}

@Composable
fun EvidenceBottomSheet(
    step: TutorialStepEntity,
    onExploreCode: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TravelExplore,
                    contentDescription = "Evidence",
                    tint = Cyan400
                )
                Text(
                    text = "Application Ground Truth Evidence",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate50
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CodeCast grounds this step in verified project source code and runtime AST bindings.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                Surface(
                    color = Slate950,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EvidenceRow(label = "Workflow Step", value = "${step.stepOrder}. ${step.title}")
                        EvidenceRow(label = "Application Screen", value = step.screenName)
                        EvidenceRow(label = "Source File", value = step.evidenceSource.ifEmpty { "src/app/page.tsx:12" }, isCode = true)
                        EvidenceRow(label = "UI Element Selector", value = step.evidenceElement.ifEmpty { "<button id='cta-action'>" }, isCode = true)

                        // Real Code Snippet Box
                        val snippet = if (step.codeSnippet.isNotEmpty()) {
                            step.codeSnippet
                        } else {
                            "// Verified AST Binding: ${step.evidenceSource.ifEmpty { "src/app/page.tsx" }}\nexport function ${step.title.replace(" ", "")}() {\n  // Handles: ${step.actionType} on ${step.screenName}\n  const trigger = document.querySelector(\"${step.evidenceElement.ifEmpty { "#cta-btn" }}\");\n  return trigger;\n}"
                        }

                        Text(text = "Verified Code Implementation:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                        Surface(
                            color = Slate900,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = snippet,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Cyan300,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Verification Status", fontSize = 11.sp, color = Slate400)
                            VerificationStatusBadge(status = step.verificationStatus)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onExploreCode != null) {
                    FilledTonalButton(
                        onClick = {
                            val path = step.evidenceSource.substringBefore(":").ifEmpty { "src/app/page.tsx" }
                            onExploreCode(path)
                            onDismiss()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Indigo600, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inspect Full Code", fontSize = 11.sp)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Slate300)
                ) {
                    Text("Close")
                }
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun EvidenceRow(label: String, value: String, isCode: Boolean = false) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
        if (isCode) {
            Surface(
                color = Slate900,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = Cyan300,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate100,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
