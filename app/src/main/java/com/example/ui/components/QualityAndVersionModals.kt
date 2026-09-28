package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectVersionEntity
import com.example.data.model.QualityCheckReport
import com.example.ui.theme.*

@Composable
fun QualityCheckReportDialog(
    report: QualityCheckReport,
    onDismiss: () -> Unit,
    onFixAutomatically: () -> Unit,
    onContinueAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (report.isClean) Icons.Default.Verified else Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = if (report.isClean) Emerald400 else Amber500
                )
                Text(
                    text = if (report.isClean) "Automated Quality Check: Passed" else "${report.issues.size} Quality Check Notice",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Automated verification against AST routes, runtime DOM controls, subtitle synchronization, and zero-hallucination compliance.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                Surface(
                    color = Slate950,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Passed Checks:", fontSize = 11.sp, color = Slate400)
                        Text(
                            text = "${report.passedChecks} / ${report.totalChecks} verification checks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                    }
                }

                if (report.issues.isNotEmpty()) {
                    report.issues.forEach { issue ->
                        Surface(
                            color = Amber500.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Step ${issue.stepOrder}: ${issue.issueType}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber500
                                )
                                Text(
                                    text = issue.description,
                                    fontSize = 11.sp,
                                    color = Slate200,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "Fix: ${issue.suggestedFix}",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Emerald500.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "All screens, routes, and controls fully verified against codebase runtime. 0 hallucinated UI elements.",
                            fontSize = 11.sp,
                            color = Emerald400,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (report.issues.isNotEmpty()) {
                Button(
                    onClick = onFixAutomatically,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Fix Automatically")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Looks Good")
                }
            }
        },
        dismissButton = {
            if (report.issues.isNotEmpty()) {
                TextButton(onClick = onContinueAnyway) {
                    Text("Continue Anyway", color = Slate400)
                }
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun ProjectVersionDialog(
    versions: List<ProjectVersionEntity>,
    onDismiss: () -> Unit,
    onApplyUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Indigo400
                )
                Text(
                    text = "Codebase Versioning & Impact Analysis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "When new code is pushed, CodeCast tracks AST diffs to identify which tutorials may become outdated.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                versions.forEach { ver ->
                    Surface(
                        color = if (ver.isLatest) Slate800 else Slate950,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (ver.isLatest) Amber500.copy(alpha = 0.4f) else Slate800
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Version ${ver.versionTag}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ver.isLatest) Amber500 else Slate200
                                )
                                if (ver.isLatest) {
                                    Surface(
                                        color = Amber500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "1 AFFECTED TUTORIAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Amber500,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = ver.changelog,
                                fontSize = 11.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyUpdate()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) {
                Text("Update Affected Tutorials")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Existing", color = Slate400)
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(12.dp)
    )
}
