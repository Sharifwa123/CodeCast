package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.*

@Composable
fun AudienceSelectionScreen(
    selectedAudience: String,
    selectedExperienceLevel: String,
    onSelectAudience: (String) -> Unit,
    onSelectExperienceLevel: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val audienceOptions = listOf(
        "New users" to "Clear, accessible terminology with UI element guidance.",
        "Existing users" to "Focus on new functionality and quick updates.",
        "Customers" to "Friendly, benefit-focused and solution-driven walkthrough.",
        "Employees" to "Internal operations and internal policy adherence.",
        "Administrators" to "Permissions, audit logs, billing, and system configuration.",
        "Developers" to "Technical terminology, API parameters, and schema structures.",
        "Technical users" to "Direct, high-density explanations without fluff.",
        "General audience" to "Balanced clarity suitable for broad public viewing."
    )

    val experienceLevels = listOf(
        "Beginner" to "Comprehensive step-by-step guidance with highlighted cursor cues.",
        "Intermediate" to "Standard pacing assuming basic familiarity with software.",
        "Advanced" to "Fast-paced, concise instructions highlighting key actions."
    )

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
                    text = "TARGET AUDIENCE & EXPERIENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan400,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Who are you teaching? CodeCast tailors vocabulary, explanation depth, and pacing automatically based on your selections.",
                    fontSize = 12.sp,
                    color = Slate300
                )
            }
        }

        // Audience Section
        Text(
            text = "WHO IS THIS TUTORIAL FOR?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.6.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            audienceOptions.forEach { (audience, desc) ->
                val isSelected = selectedAudience == audience
                Surface(
                    onClick = { onSelectAudience(audience) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Slate800 else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Indigo500 else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audience_option_${audience.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = audience,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate50 else Slate200
                            )
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectAudience(audience) },
                            colors = RadioButtonDefaults.colors(selectedColor = Indigo500)
                        )
                    }
                }
            }
        }

        // Experience Level Section
        Text(
            text = "EXPERIENCE LEVEL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.6.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            experienceLevels.forEach { (level, desc) ->
                val isSelected = selectedExperienceLevel == level
                Surface(
                    onClick = { onSelectExperienceLevel(level) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Slate800 else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Cyan400 else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("level_option_${level.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = level,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate50 else Slate200
                            )
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectExperienceLevel(level) },
                            colors = RadioButtonDefaults.colors(selectedColor = Cyan400)
                        )
                    }
                }
            }
        }

        // Continue Button
        Button(
            onClick = onContinueClick,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("continue_to_presentation_btn")
        ) {
            Text(
                text = "Continue to Duration & Presentation",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}
