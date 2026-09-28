package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TutorialStepEntity
import com.example.ui.components.VerificationStatusBadge
import com.example.ui.theme.*

@Composable
fun ReviewTutorialScreen(
    workflowTitle: String,
    audience: String,
    duration: String,
    presentationType: String,
    voiceName: String,
    narrationLang: String,
    subtitleLang: String,
    steps: List<TutorialStepEntity>,
    onGenerateClick: () -> Unit,
    onBackToStepsClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeSteps = steps.filter { it.isChecked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Guidance Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TUTORIAL PRODUCTION SPECIFICATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Review your structured tutorial plan. All video scenes, narration synthesizers, and subtitle alignments are generated automatically behind the scenes.",
                    fontSize = 12.sp,
                    color = Slate300
                )
            }
        }

        // Summary Specs Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = workflowTitle.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate50
                )

                HorizontalDivider(color = Slate800)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecRow(label = "Audience:", value = audience)
                    SpecRow(label = "Duration:", value = duration)
                    SpecRow(label = "Presentation:", value = presentationType.replace('_', ' '))
                    SpecRow(label = "Voice:", value = voiceName)
                    SpecRow(label = "Narration Language:", value = narrationLang)
                    SpecRow(label = "Subtitles Language:", value = subtitleLang)
                    SpecRow(label = "Total Active Steps:", value = "${activeSteps.size} verified steps")
                }
            }
        }

        // Steps Breakdown List
        Text(
            text = "STEP-BY-STEP SCENE BREAKDOWN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.6.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeSteps.forEachIndexed { index, step ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STEP ${index + 1}: ${step.title.uppercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            VerificationStatusBadge(status = step.verificationStatus)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Visual: ${step.screenName}",
                                fontSize = 11.sp,
                                color = Cyan400
                            )
                            Text(
                                text = "Action: ${step.actionType}",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        Surface(
                            color = Slate950,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${if (step.instruction.isNotEmpty()) step.instruction else "Start by inspecting verified screen controls for ${step.title}."}\"",
                                fontSize = 11.sp,
                                color = Slate300,
                                modifier = Modifier.padding(8.dp),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackToStepsClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .weight(0.4f)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Edit Steps")
            }

            Button(
                onClick = onGenerateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                modifier = Modifier
                    .weight(0.6f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("generate_tutorial_button")
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Slate400)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate100)
    }
}
